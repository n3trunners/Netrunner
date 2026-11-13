use crate::agent_bridge::RuntimeEvent;
use crate::{CoreError, Result};
use nix::libc::{winsize, TIOCSWINSZ};
use nix::pty::{openpty, OpenptyResult};
use nix::sys::signal::{kill, Signal};
use nix::unistd::{dup2, execvp, fork, setsid, ForkResult, Pid};
use std::ffi::CString;
use std::fs::File;
use std::io::{Read, Write};
use std::os::fd::{AsRawFd, FromRawFd, OwnedFd};
use tokio::sync::broadcast;

#[derive(Debug, Clone)]
pub struct PtySpawnRequest {
    pub session_id: String,
    pub cwd: String,
    pub rows: u16,
    pub cols: u16,
    pub rootfs_dir: Option<String>,
}

pub struct PtyHandle {
    pid: Pid,
    master_fd: OwnedFd,
    master_writer: File,
}

impl PtyHandle {
    pub fn spawn(req: PtySpawnRequest, events_tx: broadcast::Sender<RuntimeEvent>) -> Result<Self> {
        let initial_size = winsize {
            ws_row: req.rows.max(12),
            ws_col: req.cols.max(40),
            ws_xpixel: 0,
            ws_ypixel: 0,
        };

        let OpenptyResult { master, slave } =
            openpty(&initial_size, None).map_err(|e| CoreError::PtySpawn(e.to_string()))?;

        // SAFETY: Immediately exec or _exit inside the child branch without touching non-async-signal-safe allocators.
        match unsafe { fork() }.map_err(|e| CoreError::PtySpawn(e.to_string()))? {
            ForkResult::Child => {
                drop(master);
                let _ = setsid();
                let slave_raw = slave.as_raw_fd();
                let _ = dup2(slave_raw, 0);
                let _ = dup2(slave_raw, 1);
                let _ = dup2(slave_raw, 2);
                if slave_raw > 2 {
                    drop(slave);
                }

                let _ = std::env::set_current_dir(&req.cwd);

                if let Some(rootfs) = &req.rootfs_dir {
                    let proot = CString::new("libproot.so").unwrap();
                    let arg_r = CString::new("-r").unwrap();
                    let arg_rootfs = CString::new(rootfs.as_str()).unwrap();
                    let arg_b = CString::new("-b").unwrap();
                    let arg_dev = CString::new("/dev").unwrap();
                    let arg_shell = CString::new("/bin/bash").unwrap();
                    let _ = execvp(&proot, &[&proot, &arg_r, &arg_rootfs, &arg_b, &arg_dev, &arg_shell]);
                } else {
                    let sh = CString::new("/system/bin/sh").unwrap();
                    let _ = execvp(&sh, &[&sh]);
                }
                std::process::exit(127);
            }
            ForkResult::Parent { child } => {
                drop(slave);

                let dup_fd = nix::unistd::dup(master.as_raw_fd())
                    .map_err(|e| CoreError::PtySpawn(e.to_string()))?;
                let mut reader = unsafe { File::from_raw_fd(dup_fd) };

                let writer_dup = nix::unistd::dup(master.as_raw_fd())
                    .map_err(|e| CoreError::PtySpawn(e.to_string()))?;
                let master_writer = unsafe { File::from_raw_fd(writer_dup) };

                let sid = req.session_id.clone();
                std::thread::Builder::new()
                    .name(format!("pty-pump-{}", req.session_id))
                    .spawn(move || {
                        let mut buf = [0u8; 4096];
                        loop {
                            match reader.read(&mut buf) {
                                Ok(0) | Err(_) => break,
                                Ok(n) => {
                                    let text = String::from_utf8_lossy(&buf[..n]).to_string();
                                    let _ = events_tx.send(RuntimeEvent::PtyOutput {
                                        session_id: sid.clone(),
                                        chunk: text,
                                    });
                                }
                            }
                        }
                        let _ = events_tx.send(RuntimeEvent::SessionClosed { session_id: sid });
                    })
                    .map_err(|e| CoreError::PtySpawn(e.to_string()))?;

                Ok(Self {
                    pid: child,
                    master_fd: master,
                    master_writer,
                })
            }
        }
    }

    pub async fn write_input(&mut self, bytes: &[u8]) -> Result<()> {
        self.master_writer.write_all(bytes)?;
        self.master_writer.flush()?;
        Ok(())
    }

    pub fn resize(&self, rows: u16, cols: u16) -> Result<()> {
        let ws = winsize {
            ws_row: rows.max(12),
            ws_col: cols.max(40),
            ws_xpixel: 0,
            ws_ypixel: 0,
        };
        // SAFETY: TIOCSWINSZ operates on a valid PTY master file descriptor with a stack-allocated winsize struct.
        let rc = unsafe { nix::libc::ioctl(self.master_fd.as_raw_fd(), TIOCSWINSZ, &ws) };
        if rc < 0 {
            return Err(CoreError::Io(std::io::Error::last_os_error()));
        }
        Ok(())
    }

    pub fn terminate(&mut self) -> Result<()> {
        let _ = kill(self.pid, Signal::SIGTERM);
        Ok(())
    }
}

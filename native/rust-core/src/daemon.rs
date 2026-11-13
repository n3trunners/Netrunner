use crate::agent_bridge::{AgentKind, AgentRegistry, RuntimeEvent, ToolApprovalDecision};
use crate::pty::{PtyHandle, PtySpawnRequest};
use crate::{CoreConfig, CoreError, Result};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Arc;
use tokio::io::{AsyncBufReadExt, AsyncWriteExt, BufReader};
use tokio::net::{UnixListener, UnixStream};
use tokio::sync::{broadcast, mpsc, RwLock};
use tracing::{info, warn};

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "cmd", rename_all = "snake_case")]
pub enum DaemonCommand {
    StartAgentSession {
        session_id: String,
        project_id: String,
        agent: AgentKind,
        prompt: String,
        model: String,
        base_url: Option<String>,
        api_key: Option<String>,
    },
    RespondApproval {
        request_id: String,
        approved: bool,
    },
    SpawnPty {
        session_id: String,
        cwd: String,
        rows: u16,
        cols: u16,
        use_proot: bool,
    },
    WritePty {
        session_id: String,
        data: String,
    },
    ResizePty {
        session_id: String,
        rows: u16,
        cols: u16,
    },
    StopSession {
        session_id: String,
    },
    TrimCaches,
}

pub struct NetRunnerDaemon {
    config: CoreConfig,
    registry: Arc<AgentRegistry>,
    pty_sessions: Arc<RwLock<HashMap<String, PtyHandle>>>,
    approvals: Arc<RwLock<HashMap<String, mpsc::Sender<ToolApprovalDecision>>>>,
    events_tx: broadcast::Sender<RuntimeEvent>,
}

impl NetRunnerDaemon {
    pub fn new(config: CoreConfig) -> Arc<Self> {
        let (events_tx, _) = broadcast::channel(512);
        Arc::new(Self {
            config,
            registry: Arc::new(AgentRegistry::with_builtins()),
            pty_sessions: Arc::new(RwLock::new(HashMap::new())),
            approvals: Arc::new(RwLock::new(HashMap::new())),
            events_tx,
        })
    }

    pub fn subscribe(&self) -> broadcast::Receiver<RuntimeEvent> {
        self.events_tx.subscribe()
    }

    pub async fn serve_unix_socket(self: Arc<Self>) -> Result<()> {
        let sock = std::path::Path::new(&self.config.socket_path);
        if let Some(parent) = sock.parent() {
            tokio::fs::create_dir_all(parent).await?;
        }
        let _ = tokio::fs::remove_file(sock).await;

        let listener = UnixListener::bind(sock)?;
        info!(path = %self.config.socket_path, "netrunnerd Unix socket listening");

        loop {
            let (stream, _) = listener.accept().await?;
            let daemon = Arc::clone(&self);
            tokio::spawn(async move {
                if let Err(err) = daemon.handle_ipc_client(stream).await {
                    warn!(error = %err, "IPC client disconnected with error");
                }
            });
        }
    }

    async fn handle_ipc_client(self: Arc<Self>, stream: UnixStream) -> Result<()> {
        let (reader, mut writer) = stream.into_split();
        let mut lines = BufReader::new(reader).lines();
        let mut rx = self.subscribe();

        let writer_task = tokio::spawn(async move {
            while let Ok(event) = rx.recv().await {
                if let Ok(json) = serde_json::to_string(&event) {
                    if writer.write_all(json.as_bytes()).await.is_err() {
                        break;
                    }
                    if writer.write_all(b"\n").await.is_err() {
                        break;
                    }
                }
            }
        });

        while let Some(line) = lines.next_line().await? {
            if line.trim().is_empty() {
                continue;
            }
            match serde_json::from_str::<DaemonCommand>(&line) {
                Ok(cmd) => {
                    if let Err(err) = self.dispatch(cmd).await {
                        let _ = self.events_tx.send(RuntimeEvent::Error {
                            session_id: "daemon".to_string(),
                            message: err.to_string(),
                        });
                    }
                }
                Err(err) => {
                    let _ = self.events_tx.send(RuntimeEvent::Error {
                        session_id: "daemon".to_string(),
                        message: format!("invalid IPC command: {err}"),
                    });
                }
            }
        }

        writer_task.abort();
        Ok(())
    }

    pub async fn dispatch(self: &Arc<Self>, cmd: DaemonCommand) -> Result<()> {
        match cmd {
            DaemonCommand::StartAgentSession {
                session_id,
                project_id,
                agent,
                prompt,
                model,
                base_url,
                api_key,
            } => {
                let has_key = api_key.as_deref().map(str::trim).is_some_and(|k| !k.is_empty());
                if !has_key {
                    return Err(CoreError::Vault(
                        "BYOK token required: provide your own API key before launching an AI session".to_string(),
                    ));
                }
                let driver = self.registry.get(agent);
                let launch = driver.build_launch_config(
                    &self.config,
                    &project_id,
                    &model,
                    base_url.as_deref(),
                    api_key.as_deref(),
                );
                let (approval_tx, approval_rx) = mpsc::channel(8);
                self.approvals
                    .write()
                    .await
                    .insert(session_id.clone(), approval_tx);

                let events_tx = self.events_tx.clone();
                tokio::spawn(async move {
                    driver
                        .run_session(session_id, launch, prompt, approval_rx, events_tx)
                        .await;
                });
                Ok(())
            }
            DaemonCommand::RespondApproval {
                request_id,
                approved,
            } => {
                if let Some(tx) = self.approvals.read().await.get(&request_id) {
                    let _ = tx.send(ToolApprovalDecision { approved }).await;
                }
                Ok(())
            }
            DaemonCommand::SpawnPty {
                session_id,
                cwd,
                rows,
                cols,
                use_proot,
            } => {
                let req = PtySpawnRequest {
                    session_id: session_id.clone(),
                    cwd,
                    rows,
                    cols,
                    rootfs_dir: use_proot.then(|| self.config.rootfs_dir.clone()),
                };
                let handle = PtyHandle::spawn(req, self.events_tx.clone())?;
                self.pty_sessions.write().await.insert(session_id, handle);
                Ok(())
            }
            DaemonCommand::WritePty { session_id, data } => {
                let mut sessions = self.pty_sessions.write().await;
                let handle = sessions
                    .get_mut(&session_id)
                    .ok_or_else(|| CoreError::SessionNotFound(session_id))?;
                handle.write_input(data.as_bytes()).await
            }
            DaemonCommand::ResizePty {
                session_id,
                rows,
                cols,
            } => {
                let sessions = self.pty_sessions.read().await;
                let handle = sessions
                    .get(&session_id)
                    .ok_or_else(|| CoreError::SessionNotFound(session_id))?;
                handle.resize(rows, cols)
            }
            DaemonCommand::StopSession { session_id } => {
                if let Some(mut handle) = self.pty_sessions.write().await.remove(&session_id) {
                    handle.terminate()?;
                }
                self.approvals.write().await.remove(&session_id);
                Ok(())
            }
            DaemonCommand::TrimCaches => {
                info!("Trimming inactive Rust daemon buffers under Android memory pressure");
                self.approvals.write().await.shrink_to_fit();
                Ok(())
            }
        }
    }
}

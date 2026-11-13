pub mod agent_bridge;
pub mod daemon;
pub mod pty;
pub mod vault;

#[cfg(target_os = "android")]
pub mod jni_bridge;

use serde::{Deserialize, Serialize};
use thiserror::Error;

#[derive(Debug, Error)]
pub enum CoreError {
    #[error("daemon not initialized")]
    DaemonNotRunning,
    #[error("session not found: {0}")]
    SessionNotFound(String),
    #[error("pty spawn failed: {0}")]
    PtySpawn(String),
    #[error("ipc transport error: {0}")]
    Ipc(String),
    #[error("vault cryptographic error: {0}")]
    Vault(String),
    #[error("io error: {0}")]
    Io(#[from] std::io::Error),
}

pub type Result<T> = std::result::Result<T, CoreError>;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CoreConfig {
    pub data_dir: String,
    pub rootfs_dir: String,
    pub socket_path: String,
    pub max_history_chars: usize,
}

impl Default for CoreConfig {
    fn default() -> Self {
        Self {
            data_dir: "/data/user/0/com.netrunner.app/files".to_string(),
            rootfs_dir: "/data/user/0/com.netrunner.app/files/rootfs/ubuntu".to_string(),
            socket_path: "/data/user/0/com.netrunner.app/files/run/netrunnerd.sock".to_string(),
            max_history_chars: 48_000,
        }
    }
}

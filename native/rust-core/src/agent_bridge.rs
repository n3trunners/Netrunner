use crate::CoreConfig;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::{broadcast, mpsc};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum AgentKind {
    ClaudeCode,
    DeepSeekHarness,
    AntigravityCli,
    GeminiCli,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type", rename_all = "snake_case")]
pub enum RuntimeEvent {
    AgentDelta {
        session_id: String,
        text: String,
    },
    ToolApprovalRequested {
        session_id: String,
        request_id: String,
        tool_name: String,
        summary: String,
    },
    PtyOutput {
        session_id: String,
        chunk: String,
    },
    SessionClosed {
        session_id: String,
    },
    Error {
        session_id: String,
        message: String,
    },
}

#[derive(Debug, Clone)]
pub struct ToolApprovalDecision {
    pub approved: bool,
}

#[derive(Debug, Clone)]
pub struct AgentLaunchConfig {
    pub executable: String,
    pub args: Vec<String>,
    pub env: HashMap<String, String>,
    pub workspace_dir: String,
}

pub trait AgentDriver: Send + Sync {
    fn kind(&self) -> AgentKind;

    fn build_launch_config(
        &self,
        config: &CoreConfig,
        project_id: &str,
        model: &str,
        base_url: Option<&str>,
        api_key: Option<&str>,
    ) -> AgentLaunchConfig;

    fn run_session(
        &self,
        session_id: String,
        launch: AgentLaunchConfig,
        prompt: String,
        approval_rx: mpsc::Receiver<ToolApprovalDecision>,
        events_tx: broadcast::Sender<RuntimeEvent>,
    ) -> std::pin::Pin<Box<dyn std::future::Future<Output = ()> + Send>>;
}

pub struct BuiltinAgentDriver {
    kind: AgentKind,
}

impl BuiltinAgentDriver {
    pub fn new(kind: AgentKind) -> Self {
        Self { kind }
    }
}

impl AgentDriver for BuiltinAgentDriver {
    fn kind(&self) -> AgentKind {
        self.kind
    }

    fn build_launch_config(
        &self,
        config: &CoreConfig,
        project_id: &str,
        model: &str,
        base_url: Option<&str>,
        api_key: Option<&str>,
    ) -> AgentLaunchConfig {
        let mut env = HashMap::new();
        env.insert("DISABLE_AUTOUPDATER".to_string(), "1".to_string());
        env.insert("DISABLE_TELEMETRY".to_string(), "1".to_string());
        env.insert("NETRUNNER_MODEL".to_string(), model.to_string());
        if let Some(key) = api_key.map(str::trim).filter(|k| !k.is_empty()) {
            env.insert("NETRUNNER_API_KEY".to_string(), key.to_string());
            env.insert("GEMINI_API_KEY".to_string(), key.to_string());
        }
        if let Some(url) = base_url {
            env.insert("NETRUNNER_GATEWAY_URL".to_string(), url.trim_end_matches('/').to_string());
        }

        let executable = match self.kind {
            AgentKind::ClaudeCode => "/usr/local/bin/claude",
            AgentKind::DeepSeekHarness => "/usr/local/bin/dsh",
            AgentKind::AntigravityCli => "/usr/local/bin/agy",
            AgentKind::GeminiCli => "/usr/local/bin/gemini",
        };

        AgentLaunchConfig {
            executable: executable.to_string(),
            args: vec![
                "-p".to_string(),
                "--input-format".to_string(),
                "stream-json".to_string(),
                "--output-format".to_string(),
                "stream-json".to_string(),
            ],
            env,
            workspace_dir: format!("{}/projects/{}", config.data_dir, project_id),
        }
    }

    fn run_session(
        &self,
        session_id: String,
        launch: AgentLaunchConfig,
        _prompt: String,
        mut approval_rx: mpsc::Receiver<ToolApprovalDecision>,
        events_tx: broadcast::Sender<RuntimeEvent>,
    ) -> std::pin::Pin<Box<dyn std::future::Future<Output = ()> + Send>> {
        let kind = self.kind;
        Box::pin(async move {
            let _ = events_tx.send(RuntimeEvent::AgentDelta {
                session_id: session_id.clone(),
                text: format!(
                    "Rust daemon attached {:?} in {}",
                    kind, launch.workspace_dir
                ),
            });

            // Wait for cancellation or tool approval signal before closing stream
            let _ = approval_rx.recv().await;
            let _ = events_tx.send(RuntimeEvent::SessionClosed { session_id });
        })
    }
}

pub struct AgentRegistry {
    drivers: HashMap<AgentKind, Arc<dyn AgentDriver>>,
}

impl AgentRegistry {
    pub fn with_builtins() -> Self {
        let mut drivers: HashMap<AgentKind, Arc<dyn AgentDriver>> = HashMap::new();
        for kind in [
            AgentKind::ClaudeCode,
            AgentKind::DeepSeekHarness,
            AgentKind::AntigravityCli,
            AgentKind::GeminiCli,
        ] {
            drivers.insert(kind, Arc::new(BuiltinAgentDriver::new(kind)));
        }
        Self { drivers }
    }

    pub fn get(&self, kind: AgentKind) -> Arc<dyn AgentDriver> {
        Arc::clone(self.drivers.get(&kind).expect("builtin agent registered"))
    }
}

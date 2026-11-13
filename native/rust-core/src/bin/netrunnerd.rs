use netrunner_core::daemon::NetRunnerDaemon;
use netrunner_core::CoreConfig;
use tracing_subscriber::EnvFilter;

#[tokio::main]
async fn main() -> netrunner_core::Result<()> {
    tracing_subscriber::fmt()
        .with_env_filter(EnvFilter::from_default_env())
        .init();

    let socket_path = std::env::var("NETRUNNER_SOCKET")
        .unwrap_or_else(|_| "/tmp/netrunnerd.sock".to_string());

    let config = CoreConfig {
        socket_path,
        ..CoreConfig::default()
    };

    let daemon = NetRunnerDaemon::new(config);
    daemon.serve_unix_socket().await
}

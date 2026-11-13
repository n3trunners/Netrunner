use crate::daemon::{DaemonCommand, NetRunnerDaemon};
use crate::CoreConfig;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;
use std::sync::{Arc, OnceLock};
use tokio::runtime::Runtime;

static RUNTIME: OnceLock<Runtime> = OnceLock::new();
static DAEMON: OnceLock<Arc<NetRunnerDaemon>> = OnceLock::new();

fn runtime() -> &'static Runtime {
    RUNTIME.get_or_init(|| {
        tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .thread_name("netrunner-core")
            .build()
            .expect("tokio runtime init")
    })
}

#[no_mangle]
pub extern "system" fn Java_com_netrunner_core_RustDaemonBridge_nativeStartDaemon(
    mut env: JNIEnv,
    _class: JClass,
    data_dir: JString,
    rootfs_dir: JString,
    socket_path: JString,
) {
    let data_dir: String = env.get_string(&data_dir).map(Into::into).unwrap_or_default();
    let rootfs_dir: String = env.get_string(&rootfs_dir).map(Into::into).unwrap_or_default();
    let socket_path: String = env.get_string(&socket_path).map(Into::into).unwrap_or_default();

    let daemon = DAEMON.get_or_init(|| {
        let cfg = CoreConfig {
            data_dir,
            rootfs_dir,
            socket_path,
            ..CoreConfig::default()
        };
        let instance = NetRunnerDaemon::new(cfg);
        let bg = Arc::clone(&instance);
        runtime().spawn(async move {
            let _ = bg.serve_unix_socket().await;
        });
        instance
    });

    let _ = daemon;
}

#[no_mangle]
pub extern "system" fn Java_com_netrunner_core_RustDaemonBridge_nativeDispatchJson(
    mut env: JNIEnv,
    _class: JClass,
    command_json: JString,
) -> jstring {
    let raw: String = match env.get_string(&command_json) {
        Ok(s) => s.into(),
        Err(e) => {
            return env
                .new_string(format!(r#"{{"ok":false,"error":"{e}"}}"#))
                .unwrap()
                .into_raw()
        }
    };

    let response = match (DAEMON.get(), serde_json::from_str::<DaemonCommand>(&raw)) {
        (Some(daemon), Ok(cmd)) => {
            let d = Arc::clone(daemon);
            match runtime().block_on(async move { d.dispatch(cmd).await }) {
                Ok(()) => r#"{"ok":true}"#.to_string(),
                Err(e) => format!(r#"{{"ok":false,"error":{}}}"#, serde_json::json!(e.to_string())),
            }
        }
        (None, _) => r#"{"ok":false,"error":"daemon not started"}"#.to_string(),
        (_, Err(e)) => format!(r#"{{"ok":false,"error":{}}}"#, serde_json::json!(e.to_string())),
    };

    env.new_string(response).unwrap().into_raw()
}

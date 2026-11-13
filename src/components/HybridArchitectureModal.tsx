import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  X,
  Copy,
  Check,
  Download,
  Cpu,
  Smartphone,
  ArrowLeftRight,
  FileCode2,
  Sparkles,
  Terminal,
  Plus,
  Github,
  Play,
  RefreshCw,
  ExternalLink,
  CheckCircle2,
  Clock,
  XCircle,
} from 'lucide-react';
import { NetRunnerLogo } from './Icons';
import { GitHubRepo, GitHubUser } from '../types';

export interface HybridFileNode {
  path: string;
  name: string;
  layer: 'rust-core' | 'android-compose';
  language: string;
  bytes: number;
  lines: number;
}

interface WorkflowRunItem {
  id: number;
  name: string;
  status: string;
  conclusion: string | null;
  head_branch: string;
  head_sha: string;
  event: string;
  html_url: string;
  created_at: string;
}

interface HybridArchitectureModalProps {
  isOpen: boolean;
  onClose: () => void;
  onShowToast: (msg: string) => void;
  onAskGeminiWithFile?: (filePath: string, content: string) => void;
  onCreateHybridProject?: () => void;
  currentUser?: GitHubUser | null;
  userRepos?: GitHubRepo[];
  githubToken?: string | null;
  isDarkMode?: boolean;
}

type StudioTab = 'topology' | 'source' | 'build';
type LayerKey = 'android-compose' | 'ipc-bridge' | 'rust-core';

const LAYER_DETAILS: Record<
  LayerKey,
  {
    title: string;
    subtitle: string;
    stack: string;
    responsibilities: string[];
    defaultFile: string;
  }
> = {
  'android-compose': {
    title: 'Android UI Layer',
    subtitle: 'Declarative Material 3 interface and lifecycle host',
    stack: 'Kotlin 2 · Jetpack Compose · StateFlow',
    responsibilities: [
      'Edge-to-edge Compose screens for Dashboard, Settings, Chat, and PTY Terminal.',
      'Forwards Android onTrimMemory callbacks to scrub inactive daemon buffers.',
      'Collects typed DaemonEvent streams from RustDaemonBridge via SharedFlow.',
      'Hardware-backed Android Keystore wrapping for AES-256-GCM vault keys.',
    ],
    defaultFile: 'android-compose/app/src/main/java/com/netrunner/MainActivity.kt',
  },
  'ipc-bridge': {
    title: 'JNI & Unix Socket Boundary',
    subtitle: 'Zero-overhead native control and streaming event channel',
    stack: 'JNI C-ABI · LocalSocket UDS · NDJSON',
    responsibilities: [
      'nativeStartDaemon boots the multi-threaded Tokio runtime inside libnetrunner_core.so.',
      'nativeDispatchJson routes serialized DaemonCommand payloads synchronously to Rust.',
      'LocalSocket streams real-time AgentDelta, PtyOutput, and ToolApprovalRequested events.',
      'Compiled with 16 KB ELF page-size alignment (-Wl,-z,max-page-size=16384) for Android 15+.',
    ],
    defaultFile: 'rust-core/src/jni_bridge.rs',
  },
  'rust-core': {
    title: 'Core Engine & Daemon',
    subtitle: 'Memory-safe native orchestrator replacing C spawn shims',
    stack: 'Rust · Tokio · nix PTY · AES-GCM',
    responsibilities: [
      'Manages PRoot ARM64 Ubuntu userspace and PTY master/slave file descriptors.',
      'Multiplexes Claude Code, DeepSeek Harness, Antigravity CLI, and Gemini CLI drivers.',
      'Enforces character-budgeted history windows and interactive tool approval channels.',
      'Scrubs cryptographic key material from RAM on drop using zeroize.',
    ],
    defaultFile: 'rust-core/src/daemon.rs',
  },
};

export function HybridArchitectureModal({
  isOpen,
  onClose,
  onShowToast,
  onAskGeminiWithFile,
  onCreateHybridProject,
  currentUser = null,
  userRepos = [],
  githubToken = null,
  isDarkMode = true,
}: HybridArchitectureModalProps) {
  const [activeTab, setActiveTab] = useState<StudioTab>('topology');
  const [selectedLayer, setSelectedLayer] = useState<LayerKey>('rust-core');
  const [files, setFiles] = useState<HybridFileNode[]>([]);
  const [loadingTree, setLoadingTree] = useState(false);
  const [selectedPath, setSelectedPath] = useState<string>('');
  const [fileContent, setFileContent] = useState<string>('');
  const [loadingFile, setLoadingFile] = useState(false);
  const [copied, setCopied] = useState(false);
  const [copiedWorkflow, setCopiedWorkflow] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [workflowYaml, setWorkflowYaml] = useState<string>('');
  const [repoSlug, setRepoSlug] = useState<string>('');
  const [workflowRuns, setWorkflowRuns] = useState<WorkflowRunItem[]>([]);
  const [loadingRuns, setLoadingRuns] = useState(false);
  const [dispatching, setDispatching] = useState(false);
  const [actionsNotice, setActionsNotice] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen) return;
    let cancelled = false;
    setLoadingTree(true);
    setErrorMsg(null);

    fetch('/api/hybrid/tree')
      .then((res) => {
        if (!res.ok) throw new Error('Unable to load hybrid source tree');
        return res.json();
      })
      .then((data) => {
        if (cancelled) return;
        const list: HybridFileNode[] = Array.isArray(data.files) ? data.files : [];
        setFiles(list);
        if (list.length > 0 && !selectedPath) {
          const initial =
            list.find((f) => f.path.endsWith('daemon.rs'))?.path || list[0].path;
          setSelectedPath(initial);
        }
      })
      .catch((err) => {
        if (!cancelled) setErrorMsg(err.message || 'Failed to load files');
      })
      .finally(() => {
        if (!cancelled) setLoadingTree(false);
      });

    fetch('/api/ci/workflow')
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (!cancelled && data?.content) {
          setWorkflowYaml(data.content);
        }
      })
      .catch(() => {});

    if (currentUser && userRepos.length > 0 && !repoSlug) {
      setRepoSlug(`${currentUser.login}/${userRepos[0].name}`);
    }

    return () => {
      cancelled = true;
    };
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen || !selectedPath) return;
    let cancelled = false;
    setLoadingFile(true);

    fetch(`/api/hybrid/file?path=${encodeURIComponent(selectedPath)}`)
      .then((res) => {
        if (!res.ok) throw new Error('Failed to load file contents');
        return res.json();
      })
      .then((data) => {
        if (!cancelled) {
          setFileContent(data.content || '');
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setFileContent('');
          setErrorMsg(err.message || 'Could not read file');
        }
      })
      .finally(() => {
        if (!cancelled) setLoadingFile(false);
      });

    return () => {
      cancelled = true;
    };
  }, [isOpen, selectedPath]);

  if (!isOpen) return null;

  const handleCopyCode = () => {
    if (!fileContent) return;
    navigator.clipboard.writeText(fileContent);
    setCopied(true);
    onShowToast('Source copied to clipboard');
    setTimeout(() => setCopied(false), 1800);
  };

  const handleOpenFileInSource = (targetPath: string) => {
    setSelectedPath(targetPath);
    setActiveTab('source');
  };

  const handleDownloadBundle = () => {
    window.location.href = '/api/hybrid/bundle';
    onShowToast('Exporting Kotlin + Rust source bundle');
  };

  const handleCopyWorkflow = () => {
    if (!workflowYaml) return;
    navigator.clipboard.writeText(workflowYaml);
    setCopiedWorkflow(true);
    onShowToast('GitHub Actions workflow copied');
    setTimeout(() => setCopiedWorkflow(false), 1800);
  };

  const parseOwnerRepo = (slug: string): [string, string] | null => {
    const cleaned = slug.trim().replace(/^https?:\/\/github\.com\//i, '').replace(/\.git$/, '');
    const parts = cleaned.split('/').filter(Boolean);
    if (parts.length !== 2) return null;
    return [parts[0], parts[1]];
  };

  const handleLoadActions = async () => {
    const parsed = parseOwnerRepo(repoSlug);
    if (!parsed) {
      setActionsNotice('Enter a repository as owner/repo');
      return;
    }
    setLoadingRuns(true);
    setActionsNotice(null);
    try {
      const headers: Record<string, string> = {};
      if (githubToken) headers.Authorization = `Bearer ${githubToken}`;
      const res = await fetch(`/api/github/actions/${parsed[0]}/${parsed[1]}`, { headers });
      const data = await res.json();
      if (!res.ok) {
        setActionsNotice(data?.error || 'Could not load workflow runs');
        setWorkflowRuns([]);
      } else {
        setWorkflowRuns(Array.isArray(data.runs) ? data.runs : []);
        if (!data.runs?.length) {
          setActionsNotice('No workflow runs found yet. Push to GitHub to trigger a build.');
        }
      }
    } catch {
      setActionsNotice('Network error while checking GitHub Actions');
    } finally {
      setLoadingRuns(false);
    }
  };

  const handleDispatchBuild = async () => {
    const parsed = parseOwnerRepo(repoSlug);
    if (!parsed) {
      setActionsNotice('Enter a repository as owner/repo');
      return;
    }
    if (!githubToken) {
      setActionsNotice('Sign in with GitHub OAuth to trigger manual workflow dispatch.');
      return;
    }
    setDispatching(true);
    setActionsNotice(null);
    try {
      const res = await fetch(`/api/github/actions/${parsed[0]}/${parsed[1]}/dispatch`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${githubToken}`,
        },
        body: JSON.stringify({ ref: 'main', workflow: 'build.yml' }),
      });
      const data = await res.json();
      if (!res.ok) {
        setActionsNotice(data?.error || 'Dispatch failed');
      } else {
        onShowToast('GitHub Actions build triggered!');
        setActionsNotice(data.message || 'Build dispatched.');
        setTimeout(() => handleLoadActions(), 1500);
      }
    } catch {
      setActionsNotice('Network error dispatching workflow');
    } finally {
      setDispatching(false);
    }
  };

  const rustFiles = files.filter((f) => f.layer === 'rust-core');
  const kotlinFiles = files.filter((f) => f.layer === 'android-compose');
  const activeLayerSpec = LAYER_DETAILS[selectedLayer];

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-2 sm:p-4 bg-black/80 backdrop-blur-md">
        <motion.div
          initial={{ opacity: 0, scale: 0.96, y: 12 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.96, y: 12 }}
          transition={{ duration: 0.2, ease: [0.16, 1, 0.3, 1] }}
          className={`relative w-full max-w-4xl h-[88vh] max-h-[820px] flex flex-col rounded-3xl border shadow-2xl overflow-hidden ${
            isDarkMode
              ? 'bg-[#0b0d11] border-white/12 text-neutral-100'
              : 'bg-white border-neutral-200 text-neutral-900'
          }`}
        >
          {/* Header */}
          <div
            className={`px-5 py-3.5 border-b flex items-center justify-between gap-4 shrink-0 ${
              isDarkMode ? 'border-white/10 bg-white/[0.02]' : 'border-neutral-200 bg-neutral-50'
            }`}
          >
            <div className="flex items-center gap-3 min-w-0">
              <NetRunnerLogo className="w-5 h-5 shrink-0" isDark={isDarkMode} />
              <div className="min-w-0">
                <h3 className="font-semibold text-sm tracking-tight truncate">
                  Hybrid Architecture Studio
                </h3>
                <p className={`text-xs truncate ${isDarkMode ? 'text-neutral-400' : 'text-neutral-500'}`}>
                  Kotlin + Jetpack Compose UI · Rust Core Engine & Daemon
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2 shrink-0">
              {onCreateHybridProject && (
                <button
                  type="button"
                  onClick={() => {
                    onCreateHybridProject();
                    onClose();
                  }}
                  className={`hidden sm:inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                    isDarkMode
                      ? 'bg-white text-black hover:bg-neutral-200'
                      : 'bg-neutral-900 text-white hover:bg-black'
                  }`}
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Add to Workspace</span>
                </button>
              )}
              <button
                type="button"
                onClick={onClose}
                className={`p-2 rounded-xl transition-colors cursor-pointer ${
                  isDarkMode
                    ? 'text-neutral-400 hover:text-white hover:bg-white/10'
                    : 'text-neutral-500 hover:text-black hover:bg-neutral-200'
                }`}
                aria-label="Close Hybrid Architecture Studio"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Segmented Navigation Bar */}
          <div
            className={`px-5 py-2.5 border-b flex items-center justify-between gap-4 shrink-0 ${
              isDarkMode ? 'border-white/8 bg-[#0b0d11]' : 'border-neutral-200 bg-white'
            }`}
          >
            <div
              className={`inline-flex items-center gap-1 p-1 rounded-xl border ${
                isDarkMode ? 'bg-white/[0.03] border-white/10' : 'bg-neutral-100 border-neutral-200'
              }`}
            >
              {(
                [
                  { id: 'topology', label: 'Architecture' },
                  { id: 'source', label: `Source (${files.length})` },
                  { id: 'build', label: 'CI/CD & Build' },
                ] as const
              ).map((tab) => {
                const active = activeTab === tab.id;
                return (
                  <button
                    key={tab.id}
                    type="button"
                    onClick={() => setActiveTab(tab.id)}
                    className={`px-3.5 py-1.5 rounded-lg text-xs font-medium transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                      active
                        ? isDarkMode
                          ? 'bg-white text-black font-semibold'
                          : 'bg-white text-neutral-900 shadow-sm font-semibold'
                        : isDarkMode
                          ? 'text-neutral-400 hover:text-white'
                          : 'text-neutral-600 hover:text-neutral-900'
                    }`}
                  >
                    {tab.label}
                  </button>
                );
              })}
            </div>

            <button
              type="button"
              onClick={handleDownloadBundle}
              className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border text-xs font-medium transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                isDarkMode
                  ? 'border-white/12 text-neutral-200 hover:bg-white/10'
                  : 'border-neutral-300 text-neutral-700 hover:bg-neutral-100'
              }`}
            >
              <Download className="w-3.5 h-3.5" />
              <span>Export JSON</span>
            </button>
          </div>

          {/* Main Body */}
          <div className="flex-1 overflow-y-auto">
            {errorMsg && (
              <div className="m-5 p-3 rounded-xl border border-amber-500/30 bg-amber-500/10 text-amber-400 text-xs">
                {errorMsg}
              </div>
            )}

            {activeTab === 'topology' && (
              <div className="p-5 sm:p-6 space-y-6">
                <div className="space-y-1">
                  <h4 className="text-base font-semibold tracking-tight">
                    Two-Tier Native Execution Split
                  </h4>
                  <p className={`text-xs leading-relaxed max-w-2xl ${isDarkMode ? 'text-neutral-400' : 'text-neutral-600'}`}>
                    Jetpack Compose owns rendering, input ergonomics, and Android Keystore interaction while the Rust daemon manages PRoot ARM64 processes, PTY streams, and agent JSON-RPC loops off the main thread.
                  </p>
                </div>

                {/* 3-Layer Interactive Selector */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                  {(
                    [
                      {
                        key: 'android-compose' as LayerKey,
                        icon: Smartphone,
                        label: 'Android UI',
                        meta: 'Kotlin · Jetpack Compose',
                        count: `${kotlinFiles.length} files`,
                      },
                      {
                        key: 'ipc-bridge' as LayerKey,
                        icon: ArrowLeftRight,
                        label: 'FFI & IPC Channel',
                        meta: 'JNI · LocalSocket · NDJSON',
                        count: '16 KB aligned',
                      },
                      {
                        key: 'rust-core' as LayerKey,
                        icon: Cpu,
                        label: 'Core Engine / Daemon',
                        meta: 'Rust · Tokio · PRoot PTY',
                        count: `${rustFiles.length} files`,
                      },
                    ]
                  ).map((node) => {
                    const Icon = node.icon;
                    const isSelected = selectedLayer === node.key;
                    return (
                      <button
                        key={node.key}
                        type="button"
                        onClick={() => setSelectedLayer(node.key)}
                        className={`text-left p-4 rounded-2xl border transition-all cursor-pointer ${
                          isSelected
                            ? isDarkMode
                              ? 'bg-white/[0.08] border-white/35'
                              : 'bg-neutral-900 text-white border-neutral-900'
                            : isDarkMode
                              ? 'bg-white/[0.02] border-white/10 hover:bg-white/[0.05]'
                              : 'bg-neutral-50 border-neutral-200 hover:bg-neutral-100'
                        }`}
                      >
                        <div className="flex items-center justify-between mb-3">
                          <Icon className="w-5 h-5" />
                          <span
                            className={`text-[11px] font-mono tabular-nums ${
                              isSelected && !isDarkMode
                                ? 'text-neutral-300'
                                : isDarkMode
                                  ? 'text-neutral-400'
                                  : 'text-neutral-500'
                            }`}
                          >
                            {node.count}
                          </span>
                        </div>
                        <div className="font-semibold text-sm">{node.label}</div>
                        <div
                          className={`text-xs mt-1 ${
                            isSelected && !isDarkMode
                              ? 'text-neutral-300'
                              : isDarkMode
                                ? 'text-neutral-400'
                                : 'text-neutral-600'
                          }`}
                        >
                          {node.meta}
                        </div>
                      </button>
                    );
                  })}
                </div>

                {/* Selected Layer Contract Detail */}
                <div
                  className={`p-5 rounded-2xl border space-y-4 ${
                    isDarkMode ? 'bg-white/[0.02] border-white/10' : 'bg-neutral-50 border-neutral-200'
                  }`}
                >
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                      <div className="text-sm font-semibold">{activeLayerSpec.title}</div>
                      <div className={`text-xs mt-0.5 ${isDarkMode ? 'text-neutral-400' : 'text-neutral-600'}`}>
                        {activeLayerSpec.subtitle} · <span className="font-mono">{activeLayerSpec.stack}</span>
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => handleOpenFileInSource(activeLayerSpec.defaultFile)}
                      className={`inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-medium border transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                        isDarkMode
                          ? 'bg-white/5 border-white/15 hover:bg-white/10 text-white'
                          : 'bg-white border-neutral-300 hover:bg-neutral-100 text-neutral-900'
                      }`}
                    >
                      <FileCode2 className="w-3.5 h-3.5" />
                      <span>Inspect {activeLayerSpec.defaultFile.split('/').pop()}</span>
                    </button>
                  </div>

                  <div className={`border-t pt-4 space-y-2.5 ${isDarkMode ? 'border-white/8' : 'border-neutral-200'}`}>
                    {activeLayerSpec.responsibilities.map((item, idx) => (
                      <div key={idx} className="flex items-start gap-3 text-xs leading-relaxed">
                        <span className="font-mono tabular-nums opacity-50 shrink-0">
                          0{idx + 1}.
                        </span>
                        <span className={isDarkMode ? 'text-neutral-300' : 'text-neutral-700'}>
                          {item}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'source' && (
              <div className="grid grid-cols-1 md:grid-cols-12 h-full min-h-[480px]">
                {/* File Tree */}
                <div
                  className={`md:col-span-4 border-b md:border-b-0 md:border-r p-3 space-y-4 overflow-y-auto ${
                    isDarkMode ? 'border-white/10' : 'border-neutral-200'
                  }`}
                >
                  {loadingTree ? (
                    <div className="p-4 text-xs opacity-60">Loading source tree...</div>
                  ) : (
                    <>
                      <div className="space-y-1">
                        <div className="px-2 py-1 text-[11px] font-semibold opacity-60">
                          Rust Core Daemon ({rustFiles.length})
                        </div>
                        {rustFiles.map((file) => {
                          const isSelected = selectedPath === file.path;
                          return (
                            <button
                              key={file.path}
                              type="button"
                              onClick={() => setSelectedPath(file.path)}
                              className={`w-full text-left px-2.5 py-2 rounded-xl text-xs transition-colors flex items-center justify-between gap-2 cursor-pointer ${
                                isSelected
                                  ? isDarkMode
                                    ? 'bg-white/12 text-white font-medium'
                                    : 'bg-neutral-900 text-white font-medium'
                                  : isDarkMode
                                    ? 'text-neutral-400 hover:bg-white/[0.04] hover:text-white'
                                    : 'text-neutral-700 hover:bg-neutral-100'
                              }`}
                            >
                              <span className="truncate font-mono">{file.path.replace('rust-core/', '')}</span>
                              <span className="text-[10px] font-mono tabular-nums opacity-60 shrink-0">
                                {file.lines}L
                              </span>
                            </button>
                          );
                        })}
                      </div>

                      <div className="space-y-1">
                        <div className="px-2 py-1 text-[11px] font-semibold opacity-60">
                          Kotlin + Jetpack Compose ({kotlinFiles.length})
                        </div>
                        {kotlinFiles.map((file) => {
                          const isSelected = selectedPath === file.path;
                          const shortName = file.path
                            .replace('android-compose/app/src/main/java/com/netrunner/', '')
                            .replace('android-compose/', '');
                          return (
                            <button
                              key={file.path}
                              type="button"
                              onClick={() => setSelectedPath(file.path)}
                              className={`w-full text-left px-2.5 py-2 rounded-xl text-xs transition-colors flex items-center justify-between gap-2 cursor-pointer ${
                                isSelected
                                  ? isDarkMode
                                    ? 'bg-white/12 text-white font-medium'
                                    : 'bg-neutral-900 text-white font-medium'
                                  : isDarkMode
                                    ? 'text-neutral-400 hover:bg-white/[0.04] hover:text-white'
                                    : 'text-neutral-700 hover:bg-neutral-100'
                              }`}
                            >
                              <span className="truncate font-mono">{shortName}</span>
                              <span className="text-[10px] font-mono tabular-nums opacity-60 shrink-0">
                                {file.lines}L
                              </span>
                            </button>
                          );
                        })}
                      </div>
                    </>
                  )}
                </div>

                {/* File Viewer */}
                <div className="md:col-span-8 flex flex-col min-w-0">
                  <div
                    className={`px-4 py-2.5 border-b flex items-center justify-between gap-2 ${
                      isDarkMode ? 'border-white/10 bg-white/[0.02]' : 'border-neutral-200 bg-neutral-50'
                    }`}
                  >
                    <span className="text-xs font-mono truncate opacity-80">
                      native/{selectedPath || '...'}
                    </span>

                    <div className="flex items-center gap-1.5 shrink-0">
                      {onAskGeminiWithFile && selectedPath && fileContent && (
                        <button
                          type="button"
                          onClick={() => {
                            onAskGeminiWithFile(selectedPath, fileContent);
                            onClose();
                          }}
                          className={`inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-[11px] font-medium border transition-colors whitespace-nowrap cursor-pointer ${
                            isDarkMode
                              ? 'bg-white/10 border-white/15 hover:bg-white/15 text-white'
                              : 'bg-neutral-900 border-neutral-900 hover:bg-black text-white'
                          }`}
                        >
                          <Sparkles className="w-3 h-3" />
                          <span>Ask Gemini</span>
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={handleCopyCode}
                        disabled={!fileContent}
                        className={`inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-[11px] font-medium border transition-colors whitespace-nowrap disabled:opacity-40 cursor-pointer ${
                          isDarkMode
                            ? 'border-white/12 hover:bg-white/10 text-neutral-300'
                            : 'border-neutral-300 hover:bg-neutral-100 text-neutral-700'
                        }`}
                      >
                        {copied ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                        <span>{copied ? 'Copied' : 'Copy'}</span>
                      </button>
                    </div>
                  </div>

                  <div className="flex-1 overflow-auto p-4 font-mono text-xs leading-relaxed">
                    {loadingFile ? (
                      <div className="opacity-60">Reading file from native/...</div>
                    ) : (
                      <pre className="whitespace-pre select-text">
                        <code>{fileContent}</code>
                      </pre>
                    )}
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'build' && (
              <div className="p-5 sm:p-6 space-y-5">
                <div className="space-y-1">
                  <h4 className="text-base font-semibold tracking-tight">
                    GitHub Actions Auto-Build on Push
                  </h4>
                  <p className={`text-xs leading-relaxed ${isDarkMode ? 'text-neutral-400' : 'text-neutral-600'}`}>
                    Pushing to GitHub automatically triggers <span className="font-mono">.github/workflows/build.yml</span> — building the Vite web bundle, checking the Rust core daemon, and compiling the ARM64 Android Compose APK.
                  </p>
                </div>

                {/* Live .github/workflows/build.yml card */}
                <div
                  className={`p-4 rounded-2xl border space-y-3 ${
                    isDarkMode ? 'bg-[#07080b] border-white/10' : 'bg-neutral-900 border-neutral-800 text-neutral-100'
                  }`}
                >
                  <div className="flex items-center justify-between text-xs text-neutral-400">
                    <span className="flex items-center gap-2 font-medium text-neutral-200">
                      <Github className="w-3.5 h-3.5" />
                      <span className="font-mono">.github/workflows/build.yml</span>
                    </span>
                    <button
                      type="button"
                      onClick={handleCopyWorkflow}
                      disabled={!workflowYaml}
                      className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg border border-white/15 text-[11px] text-neutral-200 hover:bg-white/10 transition-colors cursor-pointer"
                    >
                      {copiedWorkflow ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                      <span>{copiedWorkflow ? 'Copied' : 'Copy YAML'}</span>
                    </button>
                  </div>
                  <pre className="text-[11px] font-mono leading-relaxed overflow-x-auto max-h-56 text-neutral-200 select-text">
                    {workflowYaml || 'Loading .github/workflows/build.yml...'}
                  </pre>
                </div>

                {/* GitHub Actions Run Inspector & Manual Dispatch */}
                <div
                  className={`p-4 rounded-2xl border space-y-3 ${
                    isDarkMode ? 'bg-white/[0.02] border-white/10' : 'bg-neutral-50 border-neutral-200'
                  }`}
                >
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <div>
                      <div className="text-xs font-semibold">Repository Actions Monitor</div>
                      <div className={`text-[11px] ${isDarkMode ? 'text-neutral-400' : 'text-neutral-600'}`}>
                        Check push builds or trigger workflow_dispatch
                      </div>
                    </div>
                  </div>

                  <div className="flex flex-col sm:flex-row gap-2">
                    <input
                      type="text"
                      value={repoSlug}
                      onChange={(e) => setRepoSlug(e.target.value)}
                      placeholder="owner/repository"
                      className={`flex-1 px-3 py-2 rounded-xl border text-xs font-mono focus:outline-none ${
                        isDarkMode
                          ? 'bg-white/[0.04] border-white/10 text-white placeholder-neutral-500'
                          : 'bg-white border-neutral-300 text-neutral-900 placeholder-neutral-400'
                      }`}
                    />
                    <div className="flex items-center gap-2 shrink-0">
                      <button
                        type="button"
                        onClick={handleLoadActions}
                        disabled={loadingRuns}
                        className={`px-3 py-2 rounded-xl border text-xs font-medium inline-flex items-center gap-1.5 transition-colors cursor-pointer ${
                          isDarkMode
                            ? 'bg-white/5 border-white/12 hover:bg-white/10 text-white'
                            : 'bg-white border-neutral-300 hover:bg-neutral-100 text-neutral-900'
                        }`}
                      >
                        <RefreshCw className={`w-3.5 h-3.5 ${loadingRuns ? 'animate-spin' : ''}`} />
                        <span>Check Runs</span>
                      </button>
                      <button
                        type="button"
                        onClick={handleDispatchBuild}
                        disabled={dispatching}
                        className={`px-3.5 py-2 rounded-xl text-xs font-semibold inline-flex items-center gap-1.5 transition-colors cursor-pointer ${
                          isDarkMode
                            ? 'bg-white text-black hover:bg-neutral-200'
                            : 'bg-neutral-900 text-white hover:bg-black'
                        }`}
                      >
                        <Play className="w-3.5 h-3.5" />
                        <span>Trigger Build</span>
                      </button>
                    </div>
                  </div>

                  {actionsNotice && (
                    <div className={`text-xs ${isDarkMode ? 'text-neutral-400' : 'text-neutral-600'}`}>
                      {actionsNotice}
                    </div>
                  )}

                  {workflowRuns.length > 0 && (
                    <div className="space-y-1.5 pt-1">
                      {workflowRuns.map((run) => (
                        <a
                          key={run.id}
                          href={run.html_url}
                          target="_blank"
                          rel="noreferrer"
                          className={`p-2.5 rounded-xl border flex items-center justify-between gap-3 text-xs transition-colors ${
                            isDarkMode
                              ? 'bg-white/[0.03] border-white/8 hover:bg-white/[0.06]'
                              : 'bg-white border-neutral-200 hover:bg-neutral-100'
                          }`}
                        >
                          <div className="flex items-center gap-2.5 min-w-0">
                            {run.conclusion === 'success' ? (
                              <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0" />
                            ) : run.conclusion === 'failure' ? (
                              <XCircle className="w-4 h-4 text-red-500 shrink-0" />
                            ) : (
                              <Clock className="w-4 h-4 text-amber-500 shrink-0" />
                            )}
                            <div className="min-w-0">
                              <div className="font-medium truncate">{run.name}</div>
                              <div className="text-[11px] font-mono opacity-60 truncate">
                                {run.head_branch} · {run.head_sha} · {run.event}
                              </div>
                            </div>
                          </div>
                          <ExternalLink className="w-3.5 h-3.5 opacity-60 shrink-0" />
                        </a>
                      ))}
                    </div>
                  )}
                </div>

                {/* Local NDK + Gradle Sequence */}
                <div
                  className={`p-4 rounded-2xl border space-y-3 ${
                    isDarkMode ? 'bg-[#07080b] border-white/10' : 'bg-neutral-900 border-neutral-800 text-neutral-100'
                  }`}
                >
                  <div className="flex items-center justify-between text-xs text-neutral-400">
                    <span className="flex items-center gap-2 font-medium">
                      <Terminal className="w-3.5 h-3.5" />
                      <span>Local Build Sequence</span>
                    </span>
                    <span className="font-mono">arm64-v8a · API 28–36</span>
                  </div>
                  <pre className="text-xs font-mono leading-relaxed overflow-x-auto text-neutral-200 select-text">
{`# 1. Web & Server Build
npm ci && npm run lint && npm run build

# 2. Build libnetrunner_core.so (16 KB ELF alignment)
cd native/rust-core
RUSTFLAGS="-C link-arg=-Wl,-z,max-page-size=16384" \\
  cargo ndk -t arm64-v8a --platform 28 \\
  -o ../android-compose/app/src/main/jniLibs build --release

# 3. Assemble Android Jetpack Compose APK
cd ../android-compose
gradle assembleDebug`}
                  </pre>
                </div>

                <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
                  <div className={`text-xs ${isDarkMode ? 'text-neutral-400' : 'text-neutral-600'}`}>
                    Includes {rustFiles.length} Rust core modules and {kotlinFiles.length} Kotlin Compose modules.
                  </div>
                  <button
                    type="button"
                    onClick={handleDownloadBundle}
                    className={`inline-flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-semibold transition-colors cursor-pointer ${
                      isDarkMode
                        ? 'bg-white text-black hover:bg-neutral-200'
                        : 'bg-neutral-900 text-white hover:bg-black'
                    }`}
                  >
                    <Download className="w-4 h-4" />
                    <span>Download Hybrid Source Bundle</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}

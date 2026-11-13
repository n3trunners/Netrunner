import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  X, Send, Square, Sparkles, Settings as SettingsIcon, 
  Trash2, AlertCircle, Bot, ArrowUp, RefreshCw,
  Key, Eye, EyeOff, ExternalLink, Loader2
} from 'lucide-react';
import { NetRunnerLogo } from './Icons';
import { MarkdownView } from './MarkdownView';
import { Project } from '../types';
import {
  BYOK_UPDATED_EVENT,
  ByokConfig,
  getByokConfig,
  saveByokConfig,
} from '../utils/byok';

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: string;
}

interface AIChatModalProps {
  isOpen: boolean;
  project?: Project | null;
  initialPrompt?: string | null;
  onClearInitialPrompt?: () => void;
  onClose: () => void;
  onOpenSettings: (modalType: 'models') => void;
  onShowToast: (msg: string) => void;
  isDarkMode?: boolean;
}

const createWelcomeMessage = (project?: Project | null): ChatMessage => ({
  id: `msg-init-${project?.id || 'global'}`,
  role: 'assistant',
  content: project
    ? `Developer workspace ready for **${project.name}** (using your personal AI token). Ask me to scaffold modules, write features, debug code, or plan architecture.`
    : "NetRunner Developer Studio ready. Powered by your own API token — ask me to scaffold apps, write code, debug issues, or design architecture.",
  timestamp: 'Just now'
});

export function AIChatModal({
  isOpen,
  project = null,
  initialPrompt = null,
  onClearInitialPrompt,
  onClose,
  onOpenSettings,
  onShowToast,
  isDarkMode = true
}: AIChatModalProps) {
  const projectKey = project?.id || 'global';
  const historyByProjectRef = useRef<Record<string, ChatMessage[]>>({});

  // In-memory conversation history (persisted per project during session)
  const [messages, setMessages] = useState<ChatMessage[]>(() => [createWelcomeMessage(project)]);
  const [input, setInput] = useState('');
  const [isStreaming, setIsStreaming] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [byok, setByok] = useState<ByokConfig>(() => getByokConfig());
  const [inlineToken, setInlineToken] = useState('');
  const [showInlineToken, setShowInlineToken] = useState(false);
  const [isUnlocking, setIsUnlocking] = useState(false);

  const abortControllerRef = useRef<AbortController | null>(null);
  const chatEndRef = useRef<HTMLDivElement | null>(null);
  const inputRef = useRef<HTMLTextAreaElement | null>(null);

  useEffect(() => {
    const syncByok = () => setByok(getByokConfig());
    window.addEventListener(BYOK_UPDATED_EVENT, syncByok);
    return () => window.removeEventListener(BYOK_UPDATED_EVENT, syncByok);
  }, []);

  // Sync active project conversation when switching projects
  useEffect(() => {
    if (!isOpen) return;
    setByok(getByokConfig());
    const saved = historyByProjectRef.current[projectKey];
    if (saved && saved.length > 0) {
      setMessages(saved);
    } else {
      const initial = [createWelcomeMessage(project)];
      historyByProjectRef.current[projectKey] = initial;
      setMessages(initial);
    }
    setErrorMessage(null);
  }, [projectKey, isOpen]);

  useEffect(() => {
    historyByProjectRef.current[projectKey] = messages;
  }, [messages, projectKey]);

  // Auto-scroll chat to bottom as stream chunks arrive
  useEffect(() => {
    if (isOpen) {
      chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isOpen]);

  // Focus input when opened and apply any prefilled prompt from Hybrid Studio
  useEffect(() => {
    if (isOpen) {
      if (initialPrompt) {
        setInput(initialPrompt);
        onClearInitialPrompt?.();
      }
      setTimeout(() => inputRef.current?.focus(), 150);
    }
  }, [isOpen, initialPrompt]);

  if (!isOpen) return null;

  // Stop button handler using AbortController
  const handleStopGeneration = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
      setIsStreaming(false);
      onShowToast('Generation stopped');
    }
  };

  const handleSaveInlineToken = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = inlineToken.trim();
    if (!trimmed) return;
    setIsUnlocking(true);
    setErrorMessage(null);

    try {
      const res = await fetch('/api/ai/verify', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-ai-api-key': trimmed,
          'x-ai-provider': byok.provider,
          ...(byok.baseUrl ? { 'x-ai-base-url': byok.baseUrl } : {}),
        },
        body: JSON.stringify({
          model: byok.model,
          provider: byok.provider,
          baseUrl: byok.baseUrl || undefined,
        }),
      });
      const data = await res.json();
      if (!res.ok || !data.ok) {
        setErrorMessage(data?.error || 'Token verification failed. Check your key.');
        setIsUnlocking(false);
        return;
      }

      const updated = saveByokConfig({ apiKey: trimmed });
      setByok(updated);
      setInlineToken('');
      onShowToast('Personal AI token verified & unlocked');
    } catch {
      setErrorMessage('Network error while verifying your token.');
    } finally {
      setIsUnlocking(false);
    }
  };

  // Chat send-message logic calling server-side BYOK AI streaming endpoint
  const handleSendMessage = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const trimmedInput = input.trim();
    if (!trimmedInput || isStreaming) return;

    const latestByok = getByokConfig();
    setByok(latestByok);
    if (!latestByok.hasToken) {
      setErrorMessage('Your own AI token is required. Paste your key below or in Settings > Models.');
      return;
    }

    setErrorMessage(null);

    const modelId = latestByok.model;

    // 1. Append user message to conversation history
    const userMsg: ChatMessage = {
      id: `usr-${Date.now()}`,
      role: 'user',
      content: trimmedInput,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    const newHistory = [...messages, userMsg];
    setMessages(newHistory);
    setInput('');

    // 2. Initialize assistant placeholder message for live streaming
    const assistantMsgId = `ast-${Date.now()}`;
    const assistantPlaceholder: ChatMessage = {
      id: assistantMsgId,
      role: 'assistant',
      content: '',
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setMessages([...newHistory, assistantPlaceholder]);
    setIsStreaming(true);

    // Prepare AbortController
    const controller = new AbortController();
    abortControllerRef.current = controller;

    try {
      // Format messages history for the server request, adding project & hybrid architecture context
      const isHybridProject =
        project?.category === 'Hybrid' || project?.architecture === 'hybrid-kotlin-rust';
      const payloadMessages = [
        ...(project
          ? [{
              role: 'system' as const,
              content: `You are the NetRunner AI builder working inside the project "${project.name}". Project description: ${project.description || 'Developer workspace'}.${
                isHybridProject
                  ? ' This workspace uses a Hybrid Architecture: Kotlin + Jetpack Compose for the Android UI (native/android-compose) and a Rust Core Engine/Daemon (native/rust-core using Tokio, nix PTY, PRoot ARM64, AES-256-GCM vault, and JNI/LocalSocket IPC). Generate idiomatic Kotlin Compose and Rust code that fits this boundary.'
                  : ''
              } Help the user build features, generate clean code, and debug issues within this project.`
            }]
          : []),
        ...newHistory.map(m => ({
          role: m.role,
          content: m.content
        }))
      ];

      // Call server-side BYOK AI streaming proxy with the user's own token
      const response = await fetch('/api/gemini/chat/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-ai-api-key': latestByok.apiKey,
          'x-ai-provider': latestByok.provider,
          ...(latestByok.baseUrl ? { 'x-ai-base-url': latestByok.baseUrl } : {}),
        },
        body: JSON.stringify({
          model: modelId,
          provider: latestByok.provider,
          baseUrl: latestByok.baseUrl || undefined,
          messages: payloadMessages,
        }),
        signal: controller.signal
      });

      // Handle HTTP Errors
      if (!response.ok) {
        let serverError = '';
        try {
          const errJson = await response.json();
          serverError = errJson?.error || '';
        } catch {
          serverError = await response.text().catch(() => '');
        }

        if (response.status === 400 || response.status === 401 || response.status === 403) {
          throw new Error(serverError || 'Invalid API key or model.');
        } else if (response.status === 429) {
          throw new Error(serverError || 'Quota reached. Try again later.');
        } else {
          throw new Error(serverError || `API error (${response.status})`);
        }
      }

      // Read SSE stream
      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('Response stream body is not readable.');
      }

      const decoder = new TextDecoder();
      let buffer = '';
      let accumulatedContent = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          const trimmed = line.trim();
          if (!trimmed || !trimmed.startsWith('data: ')) continue;

          const dataStr = trimmed.slice(6).trim();
          if (dataStr === '[DONE]') {
            setIsStreaming(false);
            abortControllerRef.current = null;
            return;
          }

          try {
            const parsed = JSON.parse(dataStr);
            if (parsed.error) {
              throw new Error(parsed.error);
            }
            const delta = parsed.delta ?? parsed.choices?.[0]?.delta?.content;
            if (delta) {
              accumulatedContent += delta;
              setMessages(prev => {
                const updated = [...prev];
                const lastIdx = updated.findIndex(m => m.id === assistantMsgId);
                if (lastIdx !== -1) {
                  updated[lastIdx] = {
                    ...updated[lastIdx],
                    content: accumulatedContent
                  };
                }
                return updated;
              });
            }
          } catch (chunkErr: any) {
            if (chunkErr instanceof SyntaxError) {
              // Ignore incomplete JSON fragments
            } else {
              throw chunkErr;
            }
          }
        }
      }
    } catch (err: any) {
      if (err.name === 'AbortError') {
        // Stopped by user
        return;
      }

      console.error('Gemini chat error:', err);

      let userFriendlyError = err.message || 'An unexpected error occurred.';
      if (err instanceof TypeError || err.message === 'Failed to fetch') {
        userFriendlyError = 'Network connection failure. Please check your internet connection.';
      }

      setErrorMessage(userFriendlyError);

      // Update the assistant bubble with the error message
      setMessages(prev => {
        const updated = [...prev];
        const lastIdx = updated.findIndex(m => m.id === assistantMsgId);
        if (lastIdx !== -1) {
          if (!updated[lastIdx].content) {
            updated[lastIdx] = {
              ...updated[lastIdx],
              content: `⚠️ **Error:** ${userFriendlyError}`
            };
          }
        }
        return updated;
      });
    } finally {
      setIsStreaming(false);
      abortControllerRef.current = null;
    }
  };

  const currentModelId = byok.model;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-2 sm:p-4 bg-black/80 backdrop-blur-md">
        <motion.div
          initial={{ opacity: 0, scale: 0.94, y: 15 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.94, y: 15 }}
          transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
          className={`relative w-full max-w-xl h-[85vh] max-h-[780px] flex flex-col rounded-3xl border shadow-2xl overflow-hidden transition-colors ${
            isDarkMode 
              ? 'bg-[#0c0e12] border-white/12 text-white' 
              : 'bg-white border-neutral-200 text-neutral-900 shadow-xl'
          }`}
        >
          {/* Top Header */}
          <div className={`px-5 py-3.5 border-b flex items-center justify-between shrink-0 ${
            isDarkMode ? 'border-white/10 bg-white/[0.02]' : 'border-neutral-100 bg-neutral-50'
          }`}>
            <div className="flex items-center gap-3 min-w-0">
              <NetRunnerLogo className="w-5 h-5" isDark={isDarkMode} />
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <h3 className="font-bold text-sm tracking-wide truncate max-w-[160px] sm:max-w-[220px]">
                    {project ? project.name : 'NetRunner Studio'}
                  </h3>
                  <button
                    type="button"
                    onClick={() => onOpenSettings('models')}
                    className={`text-[10px] font-mono px-2 py-0.5 rounded-full border shrink-0 transition-colors cursor-pointer ${
                      byok.hasToken
                        ? isDarkMode
                          ? 'bg-white/5 border-white/10 text-neutral-300 hover:bg-white/10'
                          : 'bg-neutral-100 border-neutral-300 text-neutral-700 hover:bg-neutral-200'
                        : 'bg-amber-500/10 border-amber-500/30 text-amber-400'
                    }`}
                    title="Configure Model & Personal Token"
                  >
                    {currentModelId} · {byok.hasToken ? 'BYOK' : 'No Token'}
                  </button>
                </div>
                {project && (
                  <p className={`text-[11px] truncate max-w-[230px] sm:max-w-[300px] ${
                    isDarkMode ? 'text-neutral-400' : 'text-neutral-500'
                  }`}>
                    BYOK Developer Studio · {byok.hasToken ? 'Your token active' : 'Token required'}
                  </p>
                )}
              </div>
            </div>

            <div className="flex items-center gap-1.5">
              <button
                onClick={() => onOpenSettings('models')}
                className={`p-1.5 rounded-xl transition-colors cursor-pointer ${
                  isDarkMode ? 'text-neutral-400 hover:text-white hover:bg-white/10' : 'text-neutral-500 hover:text-black hover:bg-neutral-100'
                }`}
                title="Configure Gemini Model"
              >
                <SettingsIcon className="w-4 h-4" />
              </button>
              <button
                onClick={() => {
                  const reset = [createWelcomeMessage(project)];
                  historyByProjectRef.current[projectKey] = reset;
                  setMessages(reset);
                  setErrorMessage(null);
                  onShowToast('Chat cleared');
                }}
                className={`p-1.5 rounded-xl transition-colors cursor-pointer ${
                  isDarkMode ? 'text-neutral-400 hover:text-white hover:bg-white/10' : 'text-neutral-500 hover:text-black hover:bg-neutral-100'
                }`}
                title="Clear Chat History"
              >
                <Trash2 className="w-4 h-4" />
              </button>
              <button
                onClick={onClose}
                className={`p-1.5 rounded-xl transition-colors cursor-pointer ${
                  isDarkMode ? 'text-neutral-400 hover:text-white hover:bg-white/10' : 'text-neutral-500 hover:text-black hover:bg-neutral-100'
                }`}
                title="Close Chat"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Error Banner */}
          {errorMessage && (
            <div className="px-5 py-2.5 bg-amber-500/10 border-b border-amber-500/20 text-amber-400 text-xs flex items-center justify-between shrink-0">
              <div className="flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{errorMessage}</span>
              </div>
              <button
                onClick={() => onOpenSettings('models')}
                className="underline hover:opacity-80 font-medium cursor-pointer"
              >
                Settings
              </button>
            </div>
          )}

          {/* Conversation History Body OR BYOK Token Gate */}
          <div className="flex-1 p-4 sm:p-5 overflow-y-auto space-y-4">
            {!byok.hasToken ? (
              <div className="h-full flex items-center justify-center py-6">
                <div className={`w-full max-w-md p-5 rounded-3xl border space-y-4 ${
                  isDarkMode
                    ? 'bg-white/[0.03] border-white/12 text-white'
                    : 'bg-neutral-50 border-neutral-200 text-neutral-900'
                }`}>
                  <div className="flex items-center gap-3">
                    <div className={`w-10 h-10 rounded-2xl flex items-center justify-center border ${
                      isDarkMode ? 'bg-white/10 border-white/15' : 'bg-neutral-900 border-neutral-900 text-white'
                    }`}>
                      <Key className="w-5 h-5" />
                    </div>
                    <div>
                      <h4 className="font-bold text-sm">Bring Your Own AI Token</h4>
                      <p className={`text-xs ${isDarkMode ? 'text-neutral-400' : 'text-neutral-600'}`}>
                        NetRunner provides the dev studio — not free tokens.
                      </p>
                    </div>
                  </div>

                  <p className={`text-xs leading-relaxed ${isDarkMode ? 'text-neutral-300' : 'text-neutral-700'}`}>
                    Paste your own Google Gemini API key (or configure an OpenAI-compatible provider in Model Settings) to run AI coding assistance in this workspace. Your key stays in your browser.
                  </p>

                  <form onSubmit={handleSaveInlineToken} className="space-y-3">
                    <div className="relative flex items-center">
                      <input
                        type={showInlineToken ? 'text' : 'password'}
                        value={inlineToken}
                        onChange={(e) => setInlineToken(e.target.value)}
                        placeholder={byok.provider === 'gemini' ? 'AIza... (your Gemini API key)' : 'sk-... (your provider token)'}
                        autoComplete="off"
                        className={`w-full pr-10 pl-3.5 py-2.5 rounded-2xl border text-xs font-mono focus:outline-none ${
                          isDarkMode
                            ? 'bg-white/[0.05] border-white/12 text-white placeholder-neutral-500 focus:border-white/30'
                            : 'bg-white border-neutral-300 text-neutral-900 placeholder-neutral-400 focus:border-neutral-600'
                        }`}
                      />
                      <button
                        type="button"
                        onClick={() => setShowInlineToken(!showInlineToken)}
                        className="absolute right-3 p-1 opacity-60 hover:opacity-100 cursor-pointer"
                        title={showInlineToken ? 'Hide token' : 'Show token'}
                      >
                        {showInlineToken ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                      </button>
                    </div>

                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <a
                        href="https://aistudio.google.com/apikey"
                        target="_blank"
                        rel="noreferrer"
                        className={`text-[11px] inline-flex items-center gap-1 hover:underline ${
                          isDarkMode ? 'text-neutral-400 hover:text-white' : 'text-neutral-600 hover:text-black'
                        }`}
                      >
                        <span>Get a Gemini API key</span>
                        <ExternalLink className="w-3 h-3" />
                      </a>

                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() => onOpenSettings('models')}
                          className={`px-3.5 py-2 rounded-full text-xs font-medium border transition-colors cursor-pointer ${
                            isDarkMode
                              ? 'border-white/12 text-neutral-300 hover:bg-white/10'
                              : 'border-neutral-300 text-neutral-700 hover:bg-neutral-100'
                          }`}
                        >
                          Provider Options
                        </button>
                        <button
                          type="submit"
                          disabled={!inlineToken.trim() || isUnlocking}
                          className={`px-4 py-2 rounded-full text-xs font-semibold disabled:opacity-40 flex items-center gap-1.5 transition-all cursor-pointer ${
                            isDarkMode
                              ? 'bg-white text-black hover:bg-neutral-200'
                              : 'bg-neutral-900 text-white hover:bg-black'
                          }`}
                        >
                          {isUnlocking && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                          <span>Verify & Unlock</span>
                        </button>
                      </div>
                    </div>
                  </form>
                </div>
              </div>
            ) : (
              <>
                {messages.map((msg) => {
                  const isUser = msg.role === 'user';
                  return (
                    <div
                      key={msg.id}
                      className={`flex items-start gap-3 ${isUser ? 'flex-row-reverse' : 'flex-row'}`}
                    >
                      {/* Avatar */}
                      <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 text-xs font-bold border ${
                        isUser
                          ? (isDarkMode ? 'bg-white text-black border-white' : 'bg-neutral-900 text-white border-neutral-900')
                          : (isDarkMode ? 'bg-white/10 border-white/15 text-white' : 'bg-neutral-100 border-neutral-300 text-neutral-800')
                      }`}>
                        {isUser ? 'You' : <NetRunnerLogo className="w-4 h-4" isDark={isDarkMode} />}
                      </div>

                      {/* Message Bubble */}
                      <div className={`max-w-[85%] rounded-2xl px-4 py-3 shadow-sm ${
                        isUser
                          ? (isDarkMode 
                              ? 'bg-neutral-800 border border-white/10 text-white rounded-tr-sm' 
                              : 'bg-neutral-900 text-white rounded-tr-sm')
                          : (isDarkMode 
                              ? 'bg-white/[0.035] border border-white/8 text-neutral-200 rounded-tl-sm' 
                              : 'bg-neutral-50 border border-neutral-200 text-neutral-900 rounded-tl-sm')
                      }`}>
                        {isUser ? (
                          <div className="text-sm whitespace-pre-wrap">{msg.content}</div>
                        ) : (
                          <div>
                            {msg.content ? (
                              <MarkdownView content={msg.content} isDarkMode={isDarkMode} />
                            ) : isStreaming ? (
                              <div className="flex items-center gap-1.5 py-1">
                                <span className="w-2 h-2 rounded-full bg-white animate-pulse" />
                                <span className="w-2 h-2 rounded-full bg-white animate-pulse delay-150" />
                                <span className="w-2 h-2 rounded-full bg-white animate-pulse delay-300" />
                              </div>
                            ) : (
                              <div className="text-xs text-neutral-500 italic">No response</div>
                            )}
                          </div>
                        )}

                        <div className="text-[10px] opacity-40 mt-1.5 text-right font-mono">
                          {msg.timestamp}
                        </div>
                      </div>
                    </div>
                  );
                })}
                <div ref={chatEndRef} />
              </>
            )}
          </div>

          {/* Bottom Chat Input Bar */}
          <div className={`p-4 border-t shrink-0 ${
            isDarkMode ? 'border-white/10 bg-white/[0.02]' : 'border-neutral-100 bg-neutral-50'
          }`}>
            {/* Stop Generation Button when streaming */}
            {isStreaming && (
              <div className="flex justify-center mb-2.5">
                <button
                  type="button"
                  onClick={handleStopGeneration}
                  className="px-4 py-1.5 rounded-full text-xs font-medium bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/30 flex items-center gap-1.5 transition-colors cursor-pointer shadow-sm"
                >
                  <Square className="w-3.5 h-3.5 fill-current" />
                  <span>Stop Generating</span>
                </button>
              </div>
            )}

            <form onSubmit={handleSendMessage} className="relative flex items-center">
              <textarea
                ref={inputRef}
                rows={1}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    handleSendMessage();
                  }
                }}
                placeholder="Ask Gemini anything or type code instructions... (Enter to send)"
                className={`w-full pr-12 pl-4 py-3 rounded-2xl border text-sm focus:outline-none resize-none transition-all ${
                  isDarkMode
                    ? 'bg-white/[0.04] border-white/10 text-white placeholder-neutral-500 focus:border-white/30'
                    : 'bg-white border-neutral-300 text-neutral-900 placeholder-neutral-400 focus:border-neutral-500 shadow-sm'
                }`}
              />

              <button
                type="submit"
                disabled={!input.trim() || isStreaming}
                className={`absolute right-2 p-2 rounded-xl transition-all disabled:opacity-30 cursor-pointer ${
                  isDarkMode
                    ? 'bg-white text-black hover:bg-neutral-200'
                    : 'bg-neutral-900 text-white hover:bg-black'
                }`}
                title="Send message"
              >
                <ArrowUp className="w-4 h-4" />
              </button>
            </form>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}

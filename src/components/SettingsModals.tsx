import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  X, Check, ExternalLink, Plus, Trash2, 
  Puzzle, Box, Bot, Zap, Globe, Bell, Sliders, 
  Database, ShieldCheck, Info, Github, User, ArrowRight,
  Sparkles, Key, Eye, EyeOff, Loader2, AlertCircle
} from 'lucide-react';
import { SettingsModalType, GitHubUser, UserCustomIntegration } from '../types';
import { NetRunnerLogo } from './Icons';
import {
  AIProviderType,
  BYOK_UPDATED_EVENT,
  clearByokToken,
  getByokConfig,
  maskToken,
  saveByokConfig,
} from '../utils/byok';

interface SettingsModalsProps {
  activeModal: SettingsModalType;
  onClose: () => void;
  onShowToast: (msg: string) => void;
  currentUser: GitHubUser | null;
  onOpenGitHubAuth: () => void;
  onSignOut: () => void;
  isDarkMode?: boolean;
}

export function SettingsModals({
  activeModal,
  onClose,
  onShowToast,
  currentUser,
  onOpenGitHubAuth,
  onSignOut,
  isDarkMode = true
}: SettingsModalsProps) {
  // Real user-configured states
  const [connectors, setConnectors] = useState<UserCustomIntegration[]>(() => {
    const saved = localStorage.getItem('netrunner_user_connectors');
    return saved ? JSON.parse(saved) : [];
  });
  const [models, setModels] = useState<UserCustomIntegration[]>(() => {
    const saved = localStorage.getItem('netrunner_user_models');
    return saved ? JSON.parse(saved) : [];
  });
  const [agents, setAgents] = useState<UserCustomIntegration[]>(() => {
    const saved = localStorage.getItem('netrunner_user_agents');
    return saved ? JSON.parse(saved) : [];
  });
  const [skills, setSkills] = useState<UserCustomIntegration[]>(() => {
    const saved = localStorage.getItem('netrunner_user_skills');
    return saved ? JSON.parse(saved) : [];
  });

  const [isAdding, setIsAdding] = useState(false);
  const [newName, setNewName] = useState('');
  const [newDetail, setNewDetail] = useState('');
  const [newEndpoint, setNewEndpoint] = useState('');

  const [selectedLanguage, setSelectedLanguage] = useState('en-US');
  const [notifications, setNotifications] = useState({
    buildAlerts: true,
    securityNotices: true,
    commitSync: true
  });

  // Bring-Your-Own-Token (BYOK) AI Configuration — NetRunner provides zero free tokens
  const [aiProvider, setAiProvider] = useState<AIProviderType>(() => getByokConfig().provider);
  const [apiTokenInput, setApiTokenInput] = useState<string>(() => getByokConfig().apiKey);
  const [aiBaseUrl, setAiBaseUrl] = useState<string>(() => getByokConfig().baseUrl);
  const [geminiModel, setGeminiModel] = useState<string>(() => getByokConfig().model);
  const [hasSavedToken, setHasSavedToken] = useState<boolean>(() => getByokConfig().hasToken);
  const [showToken, setShowToken] = useState(false);
  const [isVerifyingToken, setIsVerifyingToken] = useState(false);
  const [tokenVerifyMsg, setTokenVerifyMsg] = useState<{ ok: boolean; text: string } | null>(null);

  useEffect(() => {
    const sync = () => {
      const cfg = getByokConfig();
      setAiProvider(cfg.provider);
      setApiTokenInput(cfg.apiKey);
      setAiBaseUrl(cfg.baseUrl);
      setGeminiModel(cfg.model);
      setHasSavedToken(cfg.hasToken);
    };
    window.addEventListener(BYOK_UPDATED_EVENT, sync);
    return () => window.removeEventListener(BYOK_UPDATED_EVENT, sync);
  }, []);

  const handleSaveTokenAndModel = async (verifyFirst: boolean) => {
    const trimmedKey = apiTokenInput.trim();
    setTokenVerifyMsg(null);

    if (verifyFirst) {
      if (!trimmedKey) {
        setTokenVerifyMsg({ ok: false, text: 'Enter your personal API token first.' });
        return;
      }
      setIsVerifyingToken(true);
      try {
        const res = await fetch('/api/ai/verify', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'x-ai-api-key': trimmedKey,
            'x-ai-provider': aiProvider,
            ...(aiBaseUrl.trim() ? { 'x-ai-base-url': aiBaseUrl.trim() } : {}),
          },
          body: JSON.stringify({
            model: geminiModel.trim(),
            provider: aiProvider,
            baseUrl: aiBaseUrl.trim() || undefined,
          }),
        });
        const data = await res.json();
        if (!res.ok || !data.ok) {
          setTokenVerifyMsg({
            ok: false,
            text: data?.error || 'Token verification failed. Check your key and model.',
          });
          setIsVerifyingToken(false);
          return;
        }
        setTokenVerifyMsg({
          ok: true,
          text: `Token verified (${data.model || geminiModel}).`,
        });
      } catch {
        setTokenVerifyMsg({ ok: false, text: 'Network error while verifying token.' });
        setIsVerifyingToken(false);
        return;
      }
      setIsVerifyingToken(false);
    }

    const saved = saveByokConfig({
      apiKey: trimmedKey,
      provider: aiProvider,
      baseUrl: aiBaseUrl,
      model: geminiModel,
    });
    setGeminiModel(saved.model);
    setHasSavedToken(saved.hasToken);
    onShowToast(
      saved.hasToken
        ? `Personal token & ${saved.model} saved locally`
        : `Model set to ${saved.model} (add your own token to run AI)`
    );
  };

  const handleClearToken = () => {
    clearByokToken();
    setApiTokenInput('');
    setHasSavedToken(false);
    setTokenVerifyMsg(null);
    onShowToast('Personal API token removed');
  };

  if (!activeModal) return null;

  const handleAddIntegration = (type: 'connector' | 'model' | 'agent' | 'skill') => {
    if (!newName.trim()) return;

    const item: UserCustomIntegration = {
      id: `custom-${Date.now()}`,
      name: newName.trim(),
      type: newDetail.trim() || 'Custom',
      endpointOrKey: newEndpoint.trim() || undefined,
      status: 'configured',
      createdAt: new Date().toLocaleDateString()
    };

    if (type === 'connector') {
      const updated = [...connectors, item];
      setConnectors(updated);
      localStorage.setItem('netrunner_user_connectors', JSON.stringify(updated));
      onShowToast(`Connector "${item.name}" registered`);
    } else if (type === 'model') {
      const updated = [...models, item];
      setModels(updated);
      localStorage.setItem('netrunner_user_models', JSON.stringify(updated));
      onShowToast(`Model "${item.name}" configured`);
    } else if (type === 'agent') {
      const updated = [...agents, item];
      setAgents(updated);
      localStorage.setItem('netrunner_user_agents', JSON.stringify(updated));
      onShowToast(`Agent "${item.name}" initialized`);
    } else if (type === 'skill') {
      const updated = [...skills, item];
      setSkills(updated);
      localStorage.setItem('netrunner_user_skills', JSON.stringify(updated));
      onShowToast(`Skill "${item.name}" installed`);
    }

    setNewName('');
    setNewDetail('');
    setNewEndpoint('');
    setIsAdding(false);
  };

  const handleDeleteItem = (id: string, type: 'connector' | 'model' | 'agent' | 'skill') => {
    if (type === 'connector') {
      const updated = connectors.filter(i => i.id !== id);
      setConnectors(updated);
      localStorage.setItem('netrunner_user_connectors', JSON.stringify(updated));
    } else if (type === 'model') {
      const updated = models.filter(i => i.id !== id);
      setModels(updated);
      localStorage.setItem('netrunner_user_models', JSON.stringify(updated));
    } else if (type === 'agent') {
      const updated = agents.filter(i => i.id !== id);
      setAgents(updated);
      localStorage.setItem('netrunner_user_agents', JSON.stringify(updated));
    } else if (type === 'skill') {
      const updated = skills.filter(i => i.id !== id);
      setSkills(updated);
      localStorage.setItem('netrunner_user_skills', JSON.stringify(updated));
    }
    onShowToast('Removed item');
  };

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-md">
        <motion.div 
          initial={{ opacity: 0, scale: 0.94, y: 15 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.94, y: 15 }}
          transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
          className={`relative w-full max-w-md max-h-[85vh] flex flex-col rounded-3xl border shadow-2xl overflow-hidden transition-colors ${
            isDarkMode 
              ? 'bg-[#0c0e12] border-white/12 text-neutral-300' 
              : 'bg-white border-neutral-200 text-neutral-800'
          }`}
        >
          {/* Modal Header */}
          <div className={`flex items-center justify-between px-6 py-4 border-b ${
            isDarkMode ? 'border-white/10 bg-white/[0.02]' : 'border-neutral-100 bg-neutral-50'
          }`}>
            <div className="flex items-center gap-2.5">
              <NetRunnerLogo className="w-5 h-5" isDark={isDarkMode} />
              <h3 className="font-semibold tracking-wide text-sm capitalize">
                {activeModal.replace('-', ' ')}
              </h3>
            </div>
            <button 
              onClick={() => { setIsAdding(false); onClose(); }}
              className={`p-1.5 rounded-full transition-colors cursor-pointer ${
                isDarkMode ? 'text-neutral-400 hover:text-white hover:bg-white/10' : 'text-neutral-500 hover:text-black hover:bg-neutral-100'
              }`}
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Modal Content */}
          <div className="p-6 overflow-y-auto space-y-4 text-sm">
            {/* 1. Connectors */}
            {activeModal === 'connectors' && (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <p className="text-xs opacity-75">
                    Link external developer tools, MCP servers, and cloud endpoints.
                  </p>
                  {!isAdding && (
                    <button
                      onClick={() => setIsAdding(true)}
                      className={`px-3 py-1 rounded-full text-xs font-medium transition-colors flex items-center gap-1 cursor-pointer ${
                        isDarkMode ? 'bg-white text-black hover:bg-neutral-200' : 'bg-neutral-900 text-white hover:bg-black'
                      }`}
                    >
                      <Plus className="w-3.5 h-3.5" /> Add
                    </button>
                  )}
                </div>

                {isAdding && (
                  <div className={`p-4 rounded-2xl border space-y-3 ${
                    isDarkMode ? 'bg-white/[0.04] border-white/15' : 'bg-neutral-50 border-neutral-200'
                  }`}>
                    <div className="text-xs font-semibold">Add Custom Connector</div>
                    <input
                      type="text"
                      placeholder="Connector Name (e.g. Production Webhook)"
                      value={newName}
                      onChange={(e) => setNewName(e.target.value)}
                      className={`w-full px-3 py-2 rounded-xl border text-xs focus:outline-none ${
                        isDarkMode 
                          ? 'bg-white/[0.05] border-white/10 text-white placeholder-neutral-500' 
                          : 'bg-white border-neutral-300 text-neutral-900 placeholder-neutral-400'
                      }`}
                    />
                    <input
                      type="text"
                      placeholder="Service Type (e.g. MCP Server, REST, Discord)"
                      value={newDetail}
                      onChange={(e) => setNewDetail(e.target.value)}
                      className={`w-full px-3 py-2 rounded-xl border text-xs focus:outline-none ${
                        isDarkMode 
                          ? 'bg-white/[0.05] border-white/10 text-white placeholder-neutral-500' 
                          : 'bg-white border-neutral-300 text-neutral-900 placeholder-neutral-400'
                      }`}
                    />
                    <div className="flex justify-end gap-2 pt-1">
                      <button
                        onClick={() => setIsAdding(false)}
                        className="px-3 py-1.5 rounded-full text-xs opacity-70 hover:opacity-100 cursor-pointer"
                      >
                        Cancel
                      </button>
                      <button
                        onClick={() => handleAddIntegration('connector')}
                        disabled={!newName.trim()}
                        className={`px-4 py-1.5 rounded-full text-xs font-semibold disabled:opacity-40 cursor-pointer ${
                          isDarkMode ? 'bg-white text-black hover:bg-neutral-200' : 'bg-neutral-900 text-white hover:bg-black'
                        }`}
                      >
                        Save Connector
                      </button>
                    </div>
                  </div>
                )}

                {connectors.length === 0 && !isAdding ? (
                  <div className={`p-8 text-center rounded-2xl border space-y-2 ${
                    isDarkMode ? 'bg-white/[0.02] border-white/5' : 'bg-neutral-50 border-neutral-200'
                  }`}>
                    <Puzzle className="w-8 h-8 opacity-40 mx-auto" />
                    <div className="text-sm font-medium">No Connectors Configured</div>
                    <p className="text-xs opacity-60 max-w-xs mx-auto">
                      Add your real integration endpoints, MCP services, or API webhooks when you are ready.
                    </p>
                  </div>
                ) : (
                  <div className="space-y-2">
                    {connectors.map((c) => (
                      <div key={c.id} className={`p-3.5 rounded-2xl border flex items-center justify-between ${
                        isDarkMode ? 'bg-white/[0.03] border-white/8' : 'bg-neutral-50 border-neutral-200'
                      }`}>
                        <div>
                          <div className="font-medium text-sm">{c.name}</div>
                          <div className="text-xs opacity-60">{c.type} {c.endpointOrKey ? `• ${c.endpointOrKey}` : ''}</div>
                        </div>
                        <button
                          onClick={() => handleDeleteItem(c.id, 'connector')}
                          className="p-1.5 rounded-lg opacity-60 hover:opacity-100 hover:text-red-500 cursor-pointer"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* 2. Models */}
            {activeModal === 'models' && (
              <div className="space-y-5">
                <div className="flex items-center justify-between">
                  <div>
                    <h4 className="font-semibold text-xs tracking-wide uppercase opacity-80">
                      Model Providers
                    </h4>
                    <p className="text-xs opacity-75 mt-0.5">
                      Configure inference models, API keys, and local LLM endpoints.
                    </p>
                  </div>
                  {!isAdding && (
                    <button
                      onClick={() => setIsAdding(true)}
                      className={`px-3 py-1 rounded-full text-xs font-medium transition-colors flex items-center gap-1 cursor-pointer ${
                        isDarkMode ? 'bg-white/10 hover:bg-white/20 text-white' : 'bg-neutral-100 hover:bg-neutral-200 text-neutral-800'
                      }`}
                    >
                      <Plus className="w-3.5 h-3.5" /> Add Custom
                    </button>
                  )}
                </div>

                {/* Bring Your Own Token (BYOK) Provider Card */}
                <div className={`p-4 rounded-2xl border space-y-3.5 ${
                  isDarkMode 
                    ? 'bg-gradient-to-br from-white/[0.06] to-white/[0.02] border-white/15 shadow-lg' 
                    : 'bg-white border-neutral-300 shadow-md'
                }`}>
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center gap-2.5">
                      <div className={`p-2 rounded-xl ${isDarkMode ? 'bg-white/10 text-white' : 'bg-neutral-900 text-white'}`}>
                        <Key className="w-4 h-4" />
                      </div>
                      <div>
                        <div className="font-bold text-sm flex items-center gap-2 flex-wrap">
                          <span>Bring Your Own Token</span>
                          <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${
                            hasSavedToken
                              ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
                              : 'bg-amber-500/10 border-amber-500/30 text-amber-400'
                          }`}>
                            {hasSavedToken ? 'Personal Key Active' : 'Token Required'}
                          </span>
                        </div>
                        <div className="text-xs opacity-70">
                          Developer studio powered by your own API key
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Explicit No-Free-Tokens Notice */}
                  <div className={`p-3 rounded-xl border text-xs flex items-start gap-2.5 leading-relaxed ${
                    isDarkMode ? 'bg-white/[0.02] border-white/10 text-neutral-300' : 'bg-neutral-50 border-neutral-200 text-neutral-700'
                  }`}>
                    <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                    <div>
                      NetRunner does not supply free AI tokens. Your personal API key stays in your browser and is sent only with your requests.
                    </div>
                  </div>

                  {/* Provider Selector */}
                  <div className="space-y-1.5">
                    <label className="text-[11px] font-medium opacity-80 block">
                      AI Provider
                    </label>
                    <div className="grid grid-cols-2 gap-2">
                      {(
                        [
                          { id: 'gemini', label: 'Google Gemini' },
                          { id: 'openai-compatible', label: 'OpenAI-Compatible' },
                        ] as const
                      ).map((opt) => {
                        const selected = aiProvider === opt.id;
                        return (
                          <button
                            key={opt.id}
                            type="button"
                            onClick={() => {
                              setAiProvider(opt.id);
                              if (opt.id === 'gemini' && !geminiModel.startsWith('gemini')) {
                                setGeminiModel('gemini-4-hydrogen');
                              } else if (opt.id === 'openai-compatible' && geminiModel.startsWith('gemini')) {
                                setGeminiModel('gpt-4o-mini');
                              }
                            }}
                            className={`px-3 py-2 rounded-xl border text-xs font-medium transition-colors cursor-pointer ${
                              selected
                                ? isDarkMode
                                  ? 'bg-white text-black border-white font-semibold'
                                  : 'bg-neutral-900 text-white border-neutral-900 font-semibold'
                                : isDarkMode
                                  ? 'bg-white/[0.04] border-white/10 text-neutral-300 hover:bg-white/[0.08]'
                                  : 'bg-neutral-50 border-neutral-200 text-neutral-700 hover:bg-neutral-100'
                            }`}
                          >
                            {opt.label}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  {/* Personal API Token Input */}
                  <div className="space-y-1.5">
                    <div className="flex items-center justify-between text-[11px] font-medium opacity-80">
                      <span>Your API Token</span>
                      <a
                        href="https://aistudio.google.com/apikey"
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex items-center gap-1 hover:underline opacity-80 hover:opacity-100"
                      >
                        <span>Get Gemini Key</span>
                        <ExternalLink className="w-3 h-3" />
                      </a>
                    </div>

                    <div className="relative flex items-center">
                      <input
                        type={showToken ? 'text' : 'password'}
                        value={apiTokenInput}
                        onChange={(e) => setApiTokenInput(e.target.value)}
                        placeholder={aiProvider === 'gemini' ? 'AIza... (paste your Gemini key)' : 'sk-... (paste your provider token)'}
                        autoComplete="off"
                        className={`w-full pr-10 pl-3 py-2.5 rounded-xl border text-xs font-mono focus:outline-none ${
                          isDarkMode 
                            ? 'bg-white/[0.04] border-white/10 text-white placeholder-neutral-500 focus:border-white/30' 
                            : 'bg-white border-neutral-300 text-neutral-900 placeholder-neutral-400 focus:border-neutral-600'
                        }`}
                      />
                      <button
                        type="button"
                        onClick={() => setShowToken(!showToken)}
                        className="absolute right-2.5 p-1 opacity-60 hover:opacity-100 cursor-pointer"
                        title={showToken ? 'Hide token' : 'Show token'}
                      >
                        {showToken ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                      </button>
                    </div>
                    {hasSavedToken && (
                      <div className="text-[11px] font-mono opacity-60">
                        Saved in browser: {maskToken(getByokConfig().apiKey)}
                      </div>
                    )}
                  </div>

                  {/* Optional Base URL for OpenAI-compatible */}
                  {aiProvider === 'openai-compatible' && (
                    <div className="space-y-1.5">
                      <label className="text-[11px] font-medium opacity-80 block">
                        Base URL <span className="opacity-50 font-normal">(optional, defaults to OpenAI)</span>
                      </label>
                      <input
                        type="url"
                        value={aiBaseUrl}
                        onChange={(e) => setAiBaseUrl(e.target.value)}
                        placeholder="https://openrouter.ai/api/v1"
                        className={`w-full px-3 py-2 rounded-xl border text-xs font-mono focus:outline-none ${
                          isDarkMode 
                            ? 'bg-white/[0.04] border-white/10 text-white placeholder-neutral-500' 
                            : 'bg-white border-neutral-300 text-neutral-900 placeholder-neutral-400'
                        }`}
                      />
                    </div>
                  )}

                  {/* Model ID dropdown + editable text field */}
                  <div className="space-y-1.5">
                    <label className="text-[11px] font-medium opacity-80 flex items-center justify-between">
                      <span>Model ID</span>
                      <span className="text-[10px] opacity-60">Editable / Type custom</span>
                    </label>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                      {aiProvider === 'gemini' ? (
                        <select
                          value={['gemini-4-hydrogen', 'gemini-4-argon', 'gemini-4-carbon', 'gemini-3.1-flash-lite'].includes(geminiModel) ? geminiModel : 'custom'}
                          onChange={(e) => {
                            if (e.target.value !== 'custom') {
                              setGeminiModel(e.target.value);
                            }
                          }}
                          className={`px-3 py-2 rounded-xl border text-xs focus:outline-none cursor-pointer ${
                            isDarkMode 
                              ? 'bg-[#12141a] border-white/10 text-white' 
                              : 'bg-white border-neutral-300 text-neutral-900'
                          }`}
                        >
                          <option value="gemini-4-hydrogen">gemini-4-hydrogen (Default)</option>
                          <option value="gemini-4-argon">gemini-4-argon (Coding/Pro)</option>
                          <option value="gemini-4-carbon">gemini-4-carbon (Deep Reasoning)</option>
                          <option value="gemini-3.1-flash-lite">gemini-3.1-flash-lite (Fast)</option>
                          <option value="custom">Custom Model ID...</option>
                        </select>
                      ) : (
                        <select
                          value={['gpt-4o-mini', 'gpt-4o', 'deepseek-chat', 'claude-3-5-sonnet-latest'].includes(geminiModel) ? geminiModel : 'custom'}
                          onChange={(e) => {
                            if (e.target.value !== 'custom') {
                              setGeminiModel(e.target.value);
                            }
                          }}
                          className={`px-3 py-2 rounded-xl border text-xs focus:outline-none cursor-pointer ${
                            isDarkMode 
                              ? 'bg-[#12141a] border-white/10 text-white' 
                              : 'bg-white border-neutral-300 text-neutral-900'
                          }`}
                        >
                          <option value="gpt-4o-mini">gpt-4o-mini</option>
                          <option value="gpt-4o">gpt-4o</option>
                          <option value="deepseek-chat">deepseek-chat</option>
                          <option value="claude-3-5-sonnet-latest">claude-3-5-sonnet-latest</option>
                          <option value="custom">Custom Model ID...</option>
                        </select>
                      )}

                      <input
                        type="text"
                        value={geminiModel}
                        onChange={(e) => setGeminiModel(e.target.value)}
                        placeholder="Model ID"
                        className={`px-3 py-2 rounded-xl border text-xs font-mono focus:outline-none ${
                          isDarkMode 
                            ? 'bg-white/[0.04] border-white/10 text-white placeholder-neutral-500' 
                            : 'bg-white border-neutral-300 text-neutral-900 placeholder-neutral-400'
                        }`}
                      />
                    </div>
                  </div>

                  {tokenVerifyMsg && (
                    <div className={`p-2.5 rounded-xl border text-xs flex items-start gap-2 ${
                      tokenVerifyMsg.ok
                        ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
                        : 'bg-amber-500/10 border-amber-500/30 text-amber-400'
                    }`}>
                      {tokenVerifyMsg.ok ? <Check className="w-3.5 h-3.5 shrink-0 mt-0.5" /> : <AlertCircle className="w-3.5 h-3.5 shrink-0 mt-0.5" />}
                      <span>{tokenVerifyMsg.text}</span>
                    </div>
                  )}

                  {/* Save / Verify / Clear buttons */}
                  <div className={`flex flex-wrap items-center justify-between gap-2 pt-2 border-t ${isDarkMode ? 'border-white/10' : 'border-neutral-200'}`}>
                    {hasSavedToken ? (
                      <button
                        type="button"
                        onClick={handleClearToken}
                        className="px-3 py-1.5 rounded-xl text-xs font-medium text-red-400 hover:bg-red-500/10 transition-colors cursor-pointer"
                      >
                        Clear Token
                      </button>
                    ) : (
                      <span />
                    )}

                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        disabled={isVerifyingToken || !apiTokenInput.trim()}
                        onClick={() => handleSaveTokenAndModel(true)}
                        className={`px-3.5 py-1.5 rounded-xl border text-xs font-medium transition-all disabled:opacity-40 flex items-center gap-1.5 cursor-pointer ${
                          isDarkMode
                            ? 'bg-white/5 border-white/15 text-white hover:bg-white/10'
                            : 'bg-neutral-100 border-neutral-300 text-neutral-900 hover:bg-neutral-200'
                        }`}
                      >
                        {isVerifyingToken && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                        <span>Verify & Save</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => handleSaveTokenAndModel(false)}
                        className={`px-4 py-1.5 rounded-xl text-xs font-semibold shadow-md transition-all cursor-pointer ${
                          isDarkMode 
                            ? 'bg-white text-black hover:bg-neutral-200' 
                            : 'bg-neutral-900 text-white hover:bg-black'
                        }`}
                      >
                        Save
                      </button>
                    </div>
                  </div>
                </div>

                {isAdding && (
                  <div className={`p-4 rounded-2xl border space-y-3 ${
                    isDarkMode ? 'bg-white/[0.04] border-white/15' : 'bg-neutral-50 border-neutral-200'
                  }`}>
                    <div className="text-xs font-semibold">Add Custom Model Configuration</div>
                    <input
                      type="text"
                      placeholder="Model Name (e.g. gpt-4o, llama3, mistral)"
                      value={newName}
                      onChange={(e) => setNewName(e.target.value)}
                      className={`w-full px-3 py-2 rounded-xl border text-xs focus:outline-none ${
                        isDarkMode ? 'bg-white/[0.05] border-white/10 text-white' : 'bg-white border-neutral-300 text-neutral-900'
                      }`}
                    />
                    <input
                      type="text"
                      placeholder="Provider / Endpoint (e.g. Ollama http://localhost:11434)"
                      value={newDetail}
                      onChange={(e) => setNewDetail(e.target.value)}
                      className={`w-full px-3 py-2 rounded-xl border text-xs focus:outline-none ${
                        isDarkMode ? 'bg-white/[0.05] border-white/10 text-white' : 'bg-white border-neutral-300 text-neutral-900'
                      }`}
                    />
                    <div className="flex justify-end gap-2 pt-1">
                      <button
                        onClick={() => setIsAdding(false)}
                        className="px-3 py-1.5 rounded-full text-xs opacity-70 hover:opacity-100 cursor-pointer"
                      >
                        Cancel
                      </button>
                      <button
                        onClick={() => handleAddIntegration('model')}
                        disabled={!newName.trim()}
                        className={`px-4 py-1.5 rounded-full text-xs font-semibold disabled:opacity-40 cursor-pointer ${
                          isDarkMode ? 'bg-white text-black hover:bg-neutral-200' : 'bg-neutral-900 text-white hover:bg-black'
                        }`}
                      >
                        Save Model
                      </button>
                    </div>
                  </div>
                )}

                {/* Additional Custom Models */}
                {models.length > 0 && (
                  <div className="space-y-2 pt-2">
                    <div className="text-[11px] font-semibold uppercase tracking-wider opacity-60">
                      Additional Custom Endpoints ({models.length})
                    </div>
                    {models.map((m) => (
                      <div key={m.id} className={`p-3 rounded-2xl border flex items-center justify-between ${
                        isDarkMode ? 'bg-white/[0.03] border-white/8' : 'bg-neutral-50 border-neutral-200'
                      }`}>
                        <div>
                          <div className="font-medium text-xs">{m.name}</div>
                          <div className="text-[11px] opacity-60">{m.type}</div>
                        </div>
                        <button
                          onClick={() => handleDeleteItem(m.id, 'model')}
                          className="p-1.5 rounded-lg opacity-60 hover:opacity-100 hover:text-red-500 cursor-pointer"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* 3. Agents */}
            {activeModal === 'agents' && (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <p className="text-xs opacity-75">
                    Define and deploy autonomous developer agents.
                  </p>
                  {!isAdding && (
                    <button
                      onClick={() => setIsAdding(true)}
                      className={`px-3 py-1 rounded-full text-xs font-medium transition-colors flex items-center gap-1 cursor-pointer ${
                        isDarkMode ? 'bg-white text-black hover:bg-neutral-200' : 'bg-neutral-900 text-white hover:bg-black'
                      }`}
                    >
                      <Plus className="w-3.5 h-3.5" /> Create Agent
                    </button>
                  )}
                </div>

                {isAdding && (
                  <div className={`p-4 rounded-2xl border space-y-3 ${
                    isDarkMode ? 'bg-white/[0.04] border-white/15' : 'bg-neutral-50 border-neutral-200'
                  }`}>
                    <div className="text-xs font-semibold">Create Autonomous Agent</div>
                    <input
                      type="text"
                      placeholder="Agent Name (e.g. Git Reviewer, Bug Hunter)"
                      value={newName}
                      onChange={(e) => setNewName(e.target.value)}
                      className={`w-full px-3 py-2 rounded-xl border text-xs focus:outline-none ${
                        isDarkMode ? 'bg-white/[0.05] border-white/10 text-white' : 'bg-white border-neutral-300 text-neutral-900'
                      }`}
                    />
                    <input
                      type="text"
                      placeholder="Role / Objective"
                      value={newDetail}
                      onChange={(e) => setNewDetail(e.target.value)}
                      className={`w-full px-3 py-2 rounded-xl border text-xs focus:outline-none ${
                        isDarkMode ? 'bg-white/[0.05] border-white/10 text-white' : 'bg-white border-neutral-300 text-neutral-900'
                      }`}
                    />
                    <div className="flex justify-end gap-2 pt-1">
                      <button
                        onClick={() => setIsAdding(false)}
                        className="px-3 py-1.5 rounded-full text-xs opacity-70 hover:opacity-100 cursor-pointer"
                      >
                        Cancel
                      </button>
                      <button
                        onClick={() => handleAddIntegration('agent')}
                        disabled={!newName.trim()}
                        className={`px-4 py-1.5 rounded-full text-xs font-semibold disabled:opacity-40 cursor-pointer ${
                          isDarkMode ? 'bg-white text-black hover:bg-neutral-200' : 'bg-neutral-900 text-white hover:bg-black'
                        }`}
                      >
                        Save Agent
                      </button>
                    </div>
                  </div>
                )}

                {agents.length === 0 && !isAdding ? (
                  <div className={`p-8 text-center rounded-2xl border space-y-2 ${
                    isDarkMode ? 'bg-white/[0.02] border-white/5' : 'bg-neutral-50 border-neutral-200'
                  }`}>
                    <Bot className="w-8 h-8 opacity-40 mx-auto" />
                    <div className="text-sm font-medium">No Agents Configured</div>
                    <p className="text-xs opacity-60 max-w-xs mx-auto">
                      Build and integrate real developer agents whenever you are ready.
                    </p>
                  </div>
                ) : (
                  <div className="space-y-2">
                    {agents.map((a) => (
                      <div key={a.id} className={`p-3.5 rounded-2xl border flex items-center justify-between ${
                        isDarkMode ? 'bg-white/[0.03] border-white/8' : 'bg-neutral-50 border-neutral-200'
                      }`}>
                        <div>
                          <div className="font-medium text-sm">{a.name}</div>
                          <div className="text-xs opacity-60">{a.type}</div>
                        </div>
                        <button
                          onClick={() => handleDeleteItem(a.id, 'agent')}
                          className="p-1.5 rounded-lg opacity-60 hover:opacity-100 hover:text-red-500 cursor-pointer"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* 4. Skills */}
            {activeModal === 'skills' && (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <p className="text-xs opacity-75">
                    Add custom functions, tool schemas, or shell capabilities.
                  </p>
                  {!isAdding && (
                    <button
                      onClick={() => setIsAdding(true)}
                      className={`px-3 py-1 rounded-full text-xs font-medium transition-colors flex items-center gap-1 cursor-pointer ${
                        isDarkMode ? 'bg-white text-black hover:bg-neutral-200' : 'bg-neutral-900 text-white hover:bg-black'
                      }`}
                    >
                      <Plus className="w-3.5 h-3.5" /> Add Skill
                    </button>
                  )}
                </div>

                {isAdding && (
                  <div className={`p-4 rounded-2xl border space-y-3 ${
                    isDarkMode ? 'bg-white/[0.04] border-white/15' : 'bg-neutral-50 border-neutral-200'
                  }`}>
                    <div className="text-xs font-semibold">Add Custom Skill</div>
                    <input
                      type="text"
                      placeholder="Skill Identifier (e.g. docker-deploy, db-migrate)"
                      value={newName}
                      onChange={(e) => setNewName(e.target.value)}
                      className={`w-full px-3 py-2 rounded-xl border text-xs focus:outline-none ${
                        isDarkMode ? 'bg-white/[0.05] border-white/10 text-white' : 'bg-white border-neutral-300 text-neutral-900'
                      }`}
                    />
                    <input
                      type="text"
                      placeholder="Description / Version"
                      value={newDetail}
                      onChange={(e) => setNewDetail(e.target.value)}
                      className={`w-full px-3 py-2 rounded-xl border text-xs focus:outline-none ${
                        isDarkMode ? 'bg-white/[0.05] border-white/10 text-white' : 'bg-white border-neutral-300 text-neutral-900'
                      }`}
                    />
                    <div className="flex justify-end gap-2 pt-1">
                      <button
                        onClick={() => setIsAdding(false)}
                        className="px-3 py-1.5 rounded-full text-xs opacity-70 hover:opacity-100 cursor-pointer"
                      >
                        Cancel
                      </button>
                      <button
                        onClick={() => handleAddIntegration('skill')}
                        disabled={!newName.trim()}
                        className={`px-4 py-1.5 rounded-full text-xs font-semibold disabled:opacity-40 cursor-pointer ${
                          isDarkMode ? 'bg-white text-black hover:bg-neutral-200' : 'bg-neutral-900 text-white hover:bg-black'
                        }`}
                      >
                        Save Skill
                      </button>
                    </div>
                  </div>
                )}

                {skills.length === 0 && !isAdding ? (
                  <div className={`p-8 text-center rounded-2xl border space-y-2 ${
                    isDarkMode ? 'bg-white/[0.02] border-white/5' : 'bg-neutral-50 border-neutral-200'
                  }`}>
                    <Zap className="w-8 h-8 opacity-40 mx-auto" />
                    <div className="text-sm font-medium">No Custom Skills Installed</div>
                    <p className="text-xs opacity-60 max-w-xs mx-auto">
                      Link your own runtime skill packages or scripts here.
                    </p>
                  </div>
                ) : (
                  <div className="space-y-2">
                    {skills.map((s) => (
                      <div key={s.id} className={`p-3.5 rounded-2xl border flex items-center justify-between ${
                        isDarkMode ? 'bg-white/[0.03] border-white/8' : 'bg-neutral-50 border-neutral-200'
                      }`}>
                        <div>
                          <div className="font-medium text-sm">{s.name}</div>
                          <div className="text-xs opacity-60">{s.type}</div>
                        </div>
                        <button
                          onClick={() => handleDeleteItem(s.id, 'skill')}
                          className="p-1.5 rounded-lg opacity-60 hover:opacity-100 hover:text-red-500 cursor-pointer"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* Profile Detail */}
            {activeModal === 'profile-detail' && (
              <div className="space-y-4">
                {currentUser ? (
                  <div className="space-y-4">
                    <div className={`flex items-center gap-3.5 p-4 rounded-2xl border ${
                      isDarkMode ? 'bg-white/[0.03] border-white/8' : 'bg-neutral-50 border-neutral-200'
                    }`}>
                      <img
                        src={currentUser.avatar_url}
                        alt={currentUser.login}
                        className="w-14 h-14 rounded-full border border-neutral-300 object-cover"
                      />
                      <div>
                        <div className="font-bold text-base">{currentUser.name || currentUser.login}</div>
                        <a
                          href={currentUser.html_url}
                          target="_blank"
                          rel="noreferrer"
                          className="text-xs opacity-75 hover:opacity-100 flex items-center gap-1 mt-0.5"
                        >
                          <span>@{currentUser.login}</span>
                          <ExternalLink className="w-3 h-3" />
                        </a>
                      </div>
                    </div>

                    <div className={`p-3 rounded-2xl border text-xs space-y-1.5 ${
                      isDarkMode ? 'bg-white/[0.02] border-white/5 opacity-80' : 'bg-neutral-50 border-neutral-200 text-neutral-700'
                    }`}>
                      <div className="flex justify-between">
                        <span>Public Repositories:</span>
                        <span className="font-mono font-bold">{currentUser.public_repos}</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Followers:</span>
                        <span className="font-mono font-bold">{currentUser.followers}</span>
                      </div>
                    </div>

                    <div className="flex gap-2 pt-2">
                      <button
                        onClick={onSignOut}
                        className="flex-1 py-2.5 rounded-xl bg-red-500/10 hover:bg-red-500/20 text-red-500 text-xs font-medium border border-red-500/20 transition-colors cursor-pointer"
                      >
                        Sign Out
                      </button>
                      <button
                        onClick={onOpenGitHubAuth}
                        className={`flex-1 py-2.5 rounded-xl border text-xs font-medium transition-colors cursor-pointer ${
                          isDarkMode ? 'bg-white/5 hover:bg-white/10 border-white/10 text-white' : 'bg-neutral-100 hover:bg-neutral-200 border-neutral-300 text-neutral-800'
                        }`}
                      >
                        Switch Account
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-4 text-center py-2">
                    <div className={`w-14 h-14 rounded-full border mx-auto flex items-center justify-center ${
                      isDarkMode ? 'bg-white/5 border-white/10' : 'bg-neutral-100 border-neutral-300 text-neutral-900'
                    }`}>
                      <Github className="w-7 h-7" />
                    </div>
                    <div>
                      <h4 className="text-base font-bold">No GitHub Account Connected</h4>
                      <p className="text-xs opacity-70 mt-1 max-w-xs mx-auto">
                        Sign in to import your real GitHub stats, public repositories, and activity.
                      </p>
                    </div>
                    <button
                      onClick={() => { onClose(); onOpenGitHubAuth(); }}
                      className={`w-full py-3 rounded-full font-semibold text-xs transition-colors flex items-center justify-center gap-2 cursor-pointer ${
                        isDarkMode ? 'bg-white text-black hover:bg-neutral-200' : 'bg-neutral-900 text-white hover:bg-black'
                      }`}
                    >
                      <Github className="w-4 h-4" />
                      <span>Sign In with GitHub</span>
                    </button>
                  </div>
                )}
              </div>
            )}

            {/* Language */}
            {activeModal === 'language' && (
              <div className="space-y-2">
                <p className="text-xs opacity-75 mb-2">Select interface locale.</p>
                {['English (US)', 'English (UK)', '日本語', 'Deutsch', 'Español'].map((lang) => (
                  <div
                    key={lang}
                    onClick={() => { setSelectedLanguage(lang); onShowToast(`Locale set to ${lang}`); }}
                    className={`p-3 rounded-2xl border flex items-center justify-between cursor-pointer transition-colors ${
                      selectedLanguage.includes(lang.slice(0, 3))
                        ? (isDarkMode ? 'bg-white/10 border-white/30 font-semibold' : 'bg-neutral-100 border-neutral-400 font-semibold')
                        : (isDarkMode ? 'bg-white/[0.03] border-white/8 hover:bg-white/[0.06]' : 'bg-neutral-50 border-neutral-200 hover:bg-neutral-100')
                    }`}
                  >
                    <span>{lang}</span>
                    {selectedLanguage.includes(lang.slice(0, 3)) && <Check className="w-4 h-4" />}
                  </div>
                ))}
              </div>
            )}

            {/* Notifications */}
            {activeModal === 'notifications' && (
              <div className="space-y-3">
                <p className="text-xs opacity-75">Configure real push alerts and commit sync notices.</p>
                {[
                  { key: 'buildAlerts', label: 'Build & Compilation Alerts' },
                  { key: 'securityNotices', label: 'Security & Dependency Advisories' },
                  { key: 'commitSync', label: 'GitHub Activity Sync' }
                ].map((item) => (
                  <div key={item.key} className={`p-3.5 rounded-2xl border flex items-center justify-between ${
                    isDarkMode ? 'bg-white/[0.03] border-white/8' : 'bg-neutral-50 border-neutral-200'
                  }`}>
                    <span className="text-xs font-medium">{item.label}</span>
                    <button
                      onClick={() => {
                        const k = item.key as keyof typeof notifications;
                        setNotifications(prev => ({ ...prev, [k]: !prev[k] }));
                      }}
                      className={`w-10 h-5 rounded-full p-0.5 transition-colors cursor-pointer ${
                        notifications[item.key as keyof typeof notifications] 
                          ? (isDarkMode ? 'bg-white' : 'bg-neutral-900') 
                          : 'bg-neutral-400'
                      }`}
                    >
                      <div className={`w-4 h-4 rounded-full transition-transform ${
                        notifications[item.key as keyof typeof notifications] 
                          ? (isDarkMode ? 'bg-black ml-auto' : 'bg-white ml-auto') 
                          : 'bg-white'
                      }`} />
                    </button>
                  </div>
                ))}
              </div>
            )}

            {/* Editor Preferences */}
            {activeModal === 'editor' && (
              <div className="space-y-3">
                <p className="text-xs opacity-75">Editor typography and code layout.</p>
                <div className={`p-3.5 rounded-2xl border space-y-3 text-xs ${
                  isDarkMode ? 'bg-white/[0.03] border-white/8' : 'bg-neutral-50 border-neutral-200'
                }`}>
                  <div className="flex justify-between items-center">
                    <span>Font Family</span>
                    <span className={`font-mono px-2 py-0.5 rounded ${isDarkMode ? 'bg-white/10' : 'bg-neutral-200'}`}>JetBrains Mono</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span>Tab Size</span>
                    <span>2 Spaces</span>
                  </div>
                </div>
              </div>
            )}

            {/* Data & Storage */}
            {activeModal === 'data-storage' && (
              <div className="space-y-3">
                <p className="text-xs opacity-75">Local repository cache and Firestore cloud storage.</p>
                <div className={`p-3.5 rounded-2xl border text-xs space-y-2 ${
                  isDarkMode ? 'bg-white/[0.03] border-white/8' : 'bg-neutral-50 border-neutral-200'
                }`}>
                  <div className="flex justify-between">
                    <span>Firestore Database:</span>
                    <span className="font-mono text-emerald-500 font-medium">Connected (Enterprise)</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Local Cache:</span>
                    <span className="font-mono">Clean</span>
                  </div>
                </div>
              </div>
            )}

            {/* Security */}
            {activeModal === 'security' && (
              <div className="space-y-3">
                <p className="text-xs opacity-75">Authentication, BYOK token isolation, and Firestore rules.</p>
                <div className={`p-3.5 rounded-2xl border text-xs space-y-2 ${
                  isDarkMode ? 'bg-white/[0.03] border-white/8' : 'bg-neutral-50 border-neutral-200'
                }`}>
                  <div className="flex justify-between items-center">
                    <span>Auth Provider:</span>
                    <span className="text-emerald-500 font-mono">Firebase Auth (GitHub)</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span>AI Token Policy:</span>
                    <span className="text-emerald-500 font-mono">BYOK (Local Browser Only)</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span>Firestore Rules:</span>
                    <span className="text-emerald-500 font-mono">Hardened Rules Active</span>
                  </div>
                </div>
              </div>
            )}

            {/* About */}
            {activeModal === 'about' && (
              <div className="space-y-4 text-center py-2">
                <div className={`w-16 h-16 mx-auto rounded-3xl border flex items-center justify-center shadow-lg ${
                  isDarkMode ? 'bg-white/[0.06] border-white/15' : 'bg-neutral-100 border-neutral-300'
                }`}>
                  <NetRunnerLogo className="w-9 h-9" isDark={isDarkMode} />
                </div>
                <div>
                  <h4 className="text-lg font-bold tracking-wider">NETRUNNER</h4>
                  <p className="text-xs opacity-75 mt-1">Version 1.0.0 • Production Build</p>
                </div>
                <div className={`p-3 rounded-2xl border text-xs space-y-1.5 text-left ${
                  isDarkMode ? 'bg-white/[0.02] border-white/5 opacity-80' : 'bg-neutral-50 border-neutral-200 text-neutral-700'
                }`}>
                  <div className="flex justify-between gap-3">
                    <span>Android UI Layer:</span>
                    <span className="font-mono">Kotlin + Jetpack Compose</span>
                  </div>
                  <div className="flex justify-between gap-3">
                    <span>Core Engine / Daemon:</span>
                    <span className="font-mono">Rust (Tokio + PRoot + JNI)</span>
                  </div>
                  <div className="flex justify-between gap-3">
                    <span>Backend Platform:</span>
                    <span className="font-mono">Firebase Firestore & Auth</span>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Modal Footer */}
          <div className={`px-6 py-3.5 border-t flex justify-end ${
            isDarkMode ? 'border-white/10 bg-white/[0.02]' : 'border-neutral-100 bg-neutral-50'
          }`}>
            <button
              onClick={() => { setIsAdding(false); onClose(); }}
              className={`px-5 py-2 rounded-full text-xs font-medium transition-colors cursor-pointer ${
                isDarkMode ? 'bg-white text-black hover:bg-neutral-200' : 'bg-neutral-900 text-white hover:bg-black'
              }`}
            >
              Done
            </button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}

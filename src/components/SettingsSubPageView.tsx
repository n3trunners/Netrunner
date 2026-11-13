import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  ChevronLeft,
  Plus,
  Sparkles,
  Eye,
  EyeOff,
  Copy,
  Pencil,
  Trash2,
  Check,
  ExternalLink,
  Puzzle,
  Bot,
  Zap,
} from 'lucide-react';
import { UserCustomIntegration } from '../types';
import { ConfirmDeleteModal } from './ConfirmDeleteModal';
import {
  BYOK_UPDATED_EVENT,
  SavedApiKeyEntry,
  deleteApiKeyEntry,
  getSavedApiKeys,
  maskToken,
  saveApiKeyEntry,
} from '../utils/byok';

export type IntegrationSubPageType = 'connectors' | 'models' | 'agents' | 'skills';

interface SettingsSubPageViewProps {
  pageType: IntegrationSubPageType;
  onBack: () => void;
  onShowToast: (msg: string) => void;
  isDarkMode: boolean;
}

const PAGE_TITLES: Record<IntegrationSubPageType, string> = {
  connectors: 'Connectors',
  models: 'Models & API Token',
  agents: 'Agents',
  skills: 'Skills',
};

export function SettingsSubPageView({
  pageType,
  onBack,
  onShowToast,
  isDarkMode,
}: SettingsSubPageViewProps) {
  // Models & API Token states
  const [savedKeys, setSavedKeys] = useState<SavedApiKeyEntry[]>(() => getSavedApiKeys());
  const [showSupportedApis, setShowSupportedApis] = useState(false);
  const [selectedApiForNew, setSelectedApiForNew] = useState<'gemini-api' | null>(null);
  const [newKeyInput, setNewKeyInput] = useState('');
  const [showNewKeyInput, setShowNewKeyInput] = useState(false);

  const [expandedKeyId, setExpandedKeyId] = useState<string | null>(null);
  const [visibleKeyIds, setVisibleKeyIds] = useState<Record<string, boolean>>({});
  const [editingKeyId, setEditingKeyId] = useState<string | null>(null);
  const [editKeyInput, setEditKeyInput] = useState('');
  const [copiedKeyId, setCopiedKeyId] = useState<string | null>(null);

  // Connectors / Agents / Skills states
  const [connectors, setConnectors] = useState<UserCustomIntegration[]>(() => {
    const saved = localStorage.getItem('netrunner_user_connectors');
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

  const [isAddingCustom, setIsAddingCustom] = useState(false);
  const [customName, setCustomName] = useState('');
  const [customDetail, setCustomDetail] = useState('');
  const [pendingDelete, setPendingDelete] = useState<{
    kind: 'key' | 'custom';
    id: string;
    label: string;
  } | null>(null);

  useEffect(() => {
    const sync = () => setSavedKeys(getSavedApiKeys());
    window.addEventListener(BYOK_UPDATED_EVENT, sync);
    return () => window.removeEventListener(BYOK_UPDATED_EVENT, sync);
  }, []);

  // Reset transient inputs when switching page
  useEffect(() => {
    setShowSupportedApis(false);
    setSelectedApiForNew(null);
    setNewKeyInput('');
    setExpandedKeyId(null);
    setEditingKeyId(null);
    setIsAddingCustom(false);
    setCustomName('');
    setCustomDetail('');
  }, [pageType]);

  const handleTopRightPlus = () => {
    if (pageType === 'models') {
      const next = !showSupportedApis;
      setShowSupportedApis(next);
      if (!next) {
        setSelectedApiForNew(null);
        setNewKeyInput('');
      }
      setEditingKeyId(null);
    } else {
      setIsAddingCustom((prev) => !prev);
    }
  };

  const handleSaveNewGeminiKey = () => {
    if (!newKeyInput.trim()) return;
    const updated = saveApiKeyEntry(newKeyInput.trim());
    setSavedKeys(updated);
    setNewKeyInput('');
    setSelectedApiForNew(null);
    setShowSupportedApis(false);
    onShowToast('Gemini API key saved');
  };

  const handleSaveEditedKey = (id: string) => {
    if (!editKeyInput.trim()) return;
    const updated = saveApiKeyEntry(editKeyInput.trim(), id);
    setSavedKeys(updated);
    setEditingKeyId(null);
    setEditKeyInput('');
    onShowToast('API key updated');
  };

  const handleDeleteKey = (id: string) => {
    const updated = deleteApiKeyEntry(id);
    setSavedKeys(updated);
    if (expandedKeyId === id) setExpandedKeyId(null);
    if (editingKeyId === id) setEditingKeyId(null);
    onShowToast('API key deleted');
  };

  const handleCopyKey = async (entry: SavedApiKeyEntry) => {
    try {
      await navigator.clipboard.writeText(entry.apiKey);
      setCopiedKeyId(entry.id);
      onShowToast('API key copied to clipboard');
      setTimeout(() => setCopiedKeyId((prev) => (prev === entry.id ? null : prev)), 1800);
    } catch {
      onShowToast('Unable to copy automatically');
    }
  };

  const toggleKeyVisibility = (id: string) => {
    setVisibleKeyIds((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const activeCustomList =
    pageType === 'connectors' ? connectors : pageType === 'agents' ? agents : skills;

  const handleSaveCustomItem = () => {
    if (!customName.trim() || pageType === 'models') return;
    const item: UserCustomIntegration = {
      id: `custom-${Date.now()}`,
      name: customName.trim(),
      type: customDetail.trim() || 'Custom',
      status: 'configured',
      createdAt: new Date().toLocaleDateString(),
    };
    if (pageType === 'connectors') {
      const updated = [...connectors, item];
      setConnectors(updated);
      localStorage.setItem('netrunner_user_connectors', JSON.stringify(updated));
      onShowToast(`Connector "${item.name}" registered`);
    } else if (pageType === 'agents') {
      const updated = [...agents, item];
      setAgents(updated);
      localStorage.setItem('netrunner_user_agents', JSON.stringify(updated));
      onShowToast(`Agent "${item.name}" initialized`);
    } else if (pageType === 'skills') {
      const updated = [...skills, item];
      setSkills(updated);
      localStorage.setItem('netrunner_user_skills', JSON.stringify(updated));
      onShowToast(`Skill "${item.name}" installed`);
    }
    setCustomName('');
    setCustomDetail('');
    setIsAddingCustom(false);
  };

  const handleDeleteCustomItem = (id: string) => {
    if (pageType === 'connectors') {
      const updated = connectors.filter((i) => i.id !== id);
      setConnectors(updated);
      localStorage.setItem('netrunner_user_connectors', JSON.stringify(updated));
    } else if (pageType === 'agents') {
      const updated = agents.filter((i) => i.id !== id);
      setAgents(updated);
      localStorage.setItem('netrunner_user_agents', JSON.stringify(updated));
    } else if (pageType === 'skills') {
      const updated = skills.filter((i) => i.id !== id);
      setSkills(updated);
      localStorage.setItem('netrunner_user_skills', JSON.stringify(updated));
    }
    onShowToast('Removed item');
  };

  const isPlusActive = pageType === 'models' ? showSupportedApis : isAddingCustom;

  return (
    <div className="relative w-full h-full select-none">
      <div className="relative w-full h-full pb-12 overflow-y-auto">
        <div className="relative z-10 px-5 pt-4 space-y-5">
        {/* Top Bar: Back + Title + Top-Right Plus Icon */}
        <div className="flex items-center justify-between pt-1">
          <div className="flex items-center gap-3.5 min-w-0">
            <button
              type="button"
              onClick={onBack}
              className="p-1.5 -ml-1.5 rounded-full app-text-title transition-colors cursor-pointer"
              aria-label="Back to Settings"
            >
              <ChevronLeft className="w-6 h-6 stroke-[2]" />
            </button>
            <div className="h-6 w-[1.5px] app-divider shrink-0" />
            <h1 className="text-lg font-bold tracking-tight app-text-title truncate">
              {PAGE_TITLES[pageType]}
            </h1>
          </div>

          <button
            type="button"
            onClick={handleTopRightPlus}
            className={`w-9 h-9 rounded-full border flex items-center justify-center shrink-0 transition-all duration-300 cursor-pointer active:scale-95 ${
              isPlusActive
                ? isDarkMode
                  ? 'bg-white text-black border-white rotate-45'
                  : 'bg-neutral-900 text-white border-neutral-900 rotate-45'
                : isDarkMode
                  ? 'bg-white/[0.08] border-white/15 text-white hover:bg-white/15'
                  : 'bg-white border-neutral-300 text-neutral-900 hover:bg-neutral-100 shadow-sm'
            }`}
            aria-label={`Add ${PAGE_TITLES[pageType]}`}
            title={`Add ${PAGE_TITLES[pageType]}`}
          >
            <Plus className="w-4 h-4" />
          </button>
        </div>

        {/* PAGE CONTENT: MODELS & API TOKEN */}
        {pageType === 'models' ? (
          <div className="space-y-4">
            {/* Step 1: Supported APIs dropdown/card when top-right + is clicked */}
            <AnimatePresence initial={false}>
              {showSupportedApis && (
                <motion.div
                  initial={{ opacity: 0, height: 0, y: -6 }}
                  animate={{ opacity: 1, height: 'auto', y: 0 }}
                  exit={{ opacity: 0, height: 0, y: -6 }}
                  transition={{ duration: 0.28, ease: [0.16, 1, 0.3, 1] }}
                  className="overflow-hidden"
                >
                  <div className={`p-4 rounded-2xl border space-y-3 ${
                    isDarkMode
                      ? 'bg-white/[0.04] border-white/14'
                      : 'bg-white/90 border-neutral-200 shadow-sm'
                  }`}>
                    <div className="text-xs font-medium app-text-sub">
                      Supported APIs
                    </div>

                    <button
                      type="button"
                      onClick={() => setSelectedApiForNew('gemini-api')}
                      className={`w-full p-3.5 rounded-xl border flex items-center justify-between transition-all cursor-pointer ${
                        selectedApiForNew === 'gemini-api'
                          ? isDarkMode
                            ? 'bg-white text-black border-white font-semibold'
                            : 'bg-neutral-900 text-white border-neutral-900 font-semibold'
                          : isDarkMode
                            ? 'bg-white/[0.05] border-white/12 text-white hover:bg-white/[0.09]'
                            : 'bg-neutral-50 border-neutral-200 text-neutral-900 hover:bg-neutral-100'
                      }`}
                    >
                      <div className="flex items-center gap-2.5">
                        <Sparkles className="w-4 h-4" />
                        <span className="text-sm font-semibold">Gemini API</span>
                      </div>
                      <span className="text-[11px] font-mono opacity-70">Google AI Studio</span>
                    </button>

                    {/* Step 2: New concise card appears when Gemini API is selected */}
                    <AnimatePresence initial={false}>
                      {selectedApiForNew === 'gemini-api' && (
                        <motion.div
                          initial={{ opacity: 0, height: 0, y: -6 }}
                          animate={{ opacity: 1, height: 'auto', y: 0 }}
                          exit={{ opacity: 0, height: 0, y: -6 }}
                          transition={{ duration: 0.28, ease: [0.16, 1, 0.3, 1] }}
                          className="overflow-hidden"
                        >
                          <div className={`mt-1 p-4 rounded-2xl border space-y-3 ${
                            isDarkMode
                              ? 'bg-[#0b0e15] border-white/15'
                              : 'bg-neutral-50 border-neutral-200'
                          }`}>
                            <div className="flex items-center justify-between text-xs app-text-sub">
                              <span className="font-medium">Gemini API Key</span>
                              <a
                                href="https://aistudio.google.com/apikey"
                                target="_blank"
                                rel="noreferrer"
                                className="inline-flex items-center gap-1 hover:underline"
                              >
                                <span>Get key</span>
                                <ExternalLink className="w-3 h-3" />
                              </a>
                            </div>

                            <div className="relative flex items-center">
                              <input
                                type={showNewKeyInput ? 'text' : 'password'}
                                value={newKeyInput}
                                onChange={(e) => setNewKeyInput(e.target.value)}
                                placeholder="AIza..."
                                autoComplete="off"
                                autoFocus
                                className={`w-full pr-10 pl-3.5 py-2.5 rounded-xl border text-xs font-mono focus:outline-none ${
                                  isDarkMode
                                    ? 'bg-white/[0.06] border-white/15 text-white placeholder-neutral-500 focus:border-white/35'
                                    : 'bg-white border-neutral-300 text-neutral-900 placeholder-neutral-400 focus:border-neutral-600'
                                }`}
                              />
                              <button
                                type="button"
                                onClick={() => setShowNewKeyInput((prev) => !prev)}
                                className="absolute right-2.5 p-1 app-text-muted hover:opacity-100 cursor-pointer"
                                aria-label={showNewKeyInput ? 'Hide key' : 'Show key'}
                              >
                                {showNewKeyInput ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                              </button>
                            </div>

                            <div className="flex justify-end gap-2 pt-1">
                              <button
                                type="button"
                                onClick={() => {
                                  setShowSupportedApis(false);
                                  setSelectedApiForNew(null);
                                  setNewKeyInput('');
                                }}
                                className="px-3.5 py-2 rounded-full text-xs app-text-sub hover:opacity-100 cursor-pointer"
                              >
                                Cancel
                              </button>
                              <button
                                type="button"
                                disabled={!newKeyInput.trim()}
                                onClick={handleSaveNewGeminiKey}
                                className={`px-5 py-2 rounded-full text-xs font-semibold disabled:opacity-40 transition-all cursor-pointer ${
                                  isDarkMode
                                    ? 'bg-white text-black hover:bg-neutral-200'
                                    : 'bg-neutral-900 text-white hover:bg-black'
                                }`}
                              >
                                Save
                              </button>
                            </div>
                          </div>
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Saved Keys in Concise Order */}
            {savedKeys.length === 0 && !showSupportedApis ? (
              <div className="py-16 text-center space-y-2">
                <p className="text-xs app-text-sub">
                  Tap + in the top right to add a Gemini API key.
                </p>
              </div>
            ) : (
              <div className="space-y-2.5">
                {savedKeys.map((entry, idx) => {
                  const isExpanded = expandedKeyId === entry.id;
                  const isVisible = Boolean(visibleKeyIds[entry.id]);
                  const isEditing = editingKeyId === entry.id;

                  return (
                    <motion.div
                      key={entry.id}
                      initial={{ opacity: 0, y: 8 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, y: -6 }}
                      transition={{ duration: 0.2, ease: [0.16, 1, 0.3, 1] }}
                      className={`rounded-2xl border overflow-hidden transition-colors ${
                        isDarkMode
                          ? 'bg-white/[0.04] border-white/10'
                          : 'bg-white border-neutral-200 shadow-sm'
                      }`}
                    >
                      {/* Concise Row: Clickable to expand Copy/Edit/Delete + Right-side View Icon */}
                      <div
                        onClick={() => {
                          setExpandedKeyId((prev) => (prev === entry.id ? null : entry.id));
                        }}
                        className="p-3.5 flex items-center justify-between gap-3 cursor-pointer"
                      >
                        <div className="flex items-center gap-3 min-w-0">
                          <div className={`w-8 h-8 rounded-xl flex items-center justify-center shrink-0 border ${
                            isDarkMode
                              ? 'bg-white/[0.07] border-white/12 text-white'
                              : 'bg-neutral-900 border-neutral-900 text-white'
                          }`}>
                            <Sparkles className="w-4 h-4" />
                          </div>
                          <div className="min-w-0">
                            <div className="text-sm font-semibold app-text-title truncate">
                              {entry.providerLabel} {savedKeys.length > 1 ? `· #${idx + 1}` : ''}
                            </div>
                            <div className="text-xs font-mono app-text-sub truncate">
                              {isVisible ? entry.apiKey : maskToken(entry.apiKey)}
                            </div>
                          </div>
                        </div>

                        {/* View / Mask Icon on the Right Side */}
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            toggleKeyVisibility(entry.id);
                          }}
                          className={`p-2 rounded-xl transition-colors shrink-0 cursor-pointer ${
                            isDarkMode
                              ? 'text-neutral-400 hover:text-white hover:bg-white/10'
                              : 'text-neutral-500 hover:text-black hover:bg-neutral-100'
                          }`}
                          aria-label={isVisible ? 'Hide key' : 'View key'}
                          title={isVisible ? 'Hide key' : 'View key'}
                        >
                          {isVisible ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                        </button>
                      </div>

                      {/* Expanded Actions Below: Copy, Edit, Delete */}
                      <AnimatePresence initial={false}>
                        {isExpanded && (
                          <motion.div
                            initial={{ opacity: 0, height: 0 }}
                            animate={{ opacity: 1, height: 'auto' }}
                            exit={{ opacity: 0, height: 0 }}
                            transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
                            className="overflow-hidden"
                          >
                            <div className={`px-3.5 pb-3.5 pt-2.5 border-t space-y-2.5 ${
                              isDarkMode ? 'border-white/8' : 'border-neutral-100'
                            }`}>
                              {isEditing ? (
                                <div className="space-y-2.5">
                                  <input
                                    type="text"
                                    value={editKeyInput}
                                    onChange={(e) => setEditKeyInput(e.target.value)}
                                    placeholder="AIza..."
                                    autoFocus
                                    className={`w-full px-3 py-2 rounded-xl border text-xs font-mono focus:outline-none ${
                                      isDarkMode
                                        ? 'bg-white/[0.06] border-white/15 text-white'
                                        : 'bg-neutral-50 border-neutral-300 text-neutral-900'
                                    }`}
                                  />
                                  <div className="flex justify-end gap-2">
                                    <button
                                      type="button"
                                      onClick={() => {
                                        setEditingKeyId(null);
                                        setEditKeyInput('');
                                      }}
                                      className="px-3 py-1.5 rounded-full text-xs app-text-sub cursor-pointer"
                                    >
                                      Cancel
                                    </button>
                                    <button
                                      type="button"
                                      disabled={!editKeyInput.trim()}
                                      onClick={() => handleSaveEditedKey(entry.id)}
                                      className={`px-4 py-1.5 rounded-full text-xs font-semibold disabled:opacity-40 cursor-pointer ${
                                        isDarkMode
                                          ? 'bg-white text-black'
                                          : 'bg-neutral-900 text-white'
                                      }`}
                                    >
                                      Save
                                    </button>
                                  </div>
                                </div>
                              ) : (
                                <div className="grid grid-cols-3 gap-2">
                                  <button
                                    type="button"
                                    onClick={() => handleCopyKey(entry)}
                                    className={`py-2 px-3 rounded-xl border text-xs font-medium flex items-center justify-center gap-1.5 transition-colors cursor-pointer ${
                                      isDarkMode
                                        ? 'bg-white/[0.05] border-white/10 text-neutral-200 hover:bg-white/10'
                                        : 'bg-neutral-50 border-neutral-200 text-neutral-800 hover:bg-neutral-100'
                                    }`}
                                  >
                                    {copiedKeyId === entry.id ? (
                                      <Check className="w-3.5 h-3.5 text-emerald-400" />
                                    ) : (
                                      <Copy className="w-3.5 h-3.5" />
                                    )}
                                    <span>{copiedKeyId === entry.id ? 'Copied' : 'Copy'}</span>
                                  </button>

                                  <button
                                    type="button"
                                    onClick={() => {
                                      setEditingKeyId(entry.id);
                                      setEditKeyInput(entry.apiKey);
                                    }}
                                    className={`py-2 px-3 rounded-xl border text-xs font-medium flex items-center justify-center gap-1.5 transition-colors cursor-pointer ${
                                      isDarkMode
                                        ? 'bg-white/[0.05] border-white/10 text-neutral-200 hover:bg-white/10'
                                        : 'bg-neutral-50 border-neutral-200 text-neutral-800 hover:bg-neutral-100'
                                    }`}
                                  >
                                    <Pencil className="w-3.5 h-3.5" />
                                    <span>Edit</span>
                                  </button>

                                  <button
                                    type="button"
                                    onClick={() =>
                                      setPendingDelete({
                                        kind: 'key',
                                        id: entry.id,
                                        label: `${entry.providerLabel} key`,
                                      })
                                    }
                                    className="py-2 px-3 rounded-xl border border-red-500/20 bg-red-500/10 text-red-400 hover:bg-red-500/20 text-xs font-medium flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
                                  >
                                    <Trash2 className="w-3.5 h-3.5" />
                                    <span>Delete</span>
                                  </button>
                                </div>
                              )}
                            </div>
                          </motion.div>
                        )}
                      </AnimatePresence>
                    </motion.div>
                  );
                })}
              </div>
            )}
          </div>
        ) : (
          /* SEPARATE PAGES FOR CONNECTORS / AGENTS / SKILLS */
          <div className="space-y-4">
            <AnimatePresence initial={false}>
              {isAddingCustom && (
                <motion.div
                  initial={{ opacity: 0, height: 0, y: -6 }}
                  animate={{ opacity: 1, height: 'auto', y: 0 }}
                  exit={{ opacity: 0, height: 0, y: -6 }}
                  transition={{ duration: 0.26, ease: [0.16, 1, 0.3, 1] }}
                  className="overflow-hidden"
                >
                  <div className={`p-4 rounded-2xl border space-y-3 ${
                    isDarkMode
                      ? 'bg-white/[0.04] border-white/12'
                      : 'bg-white border-neutral-200 shadow-sm'
                  }`}>
                    <div className="text-xs font-semibold app-text-title">
                      New {PAGE_TITLES[pageType].slice(0, -1)}
                    </div>
                    <input
                      type="text"
                      placeholder={
                        pageType === 'connectors'
                          ? 'Connector name (e.g. MCP Server)'
                          : pageType === 'agents'
                            ? 'Agent name (e.g. Code Reviewer)'
                            : 'Skill identifier (e.g. docker-deploy)'
                      }
                      value={customName}
                      onChange={(e) => setCustomName(e.target.value)}
                      autoFocus
                      className={`w-full px-3.5 py-2.5 rounded-xl border text-xs focus:outline-none ${
                        isDarkMode
                          ? 'bg-white/[0.05] border-white/12 text-white placeholder-neutral-500'
                          : 'bg-neutral-50 border-neutral-300 text-neutral-900 placeholder-neutral-400'
                      }`}
                    />
                    <input
                      type="text"
                      placeholder={
                        pageType === 'connectors'
                          ? 'Type or endpoint'
                          : pageType === 'agents'
                            ? 'Role or objective'
                            : 'Description'
                      }
                      value={customDetail}
                      onChange={(e) => setCustomDetail(e.target.value)}
                      className={`w-full px-3.5 py-2.5 rounded-xl border text-xs focus:outline-none ${
                        isDarkMode
                          ? 'bg-white/[0.05] border-white/12 text-white placeholder-neutral-500'
                          : 'bg-neutral-50 border-neutral-300 text-neutral-900 placeholder-neutral-400'
                      }`}
                    />
                    <div className="flex justify-end gap-2">
                      <button
                        type="button"
                        onClick={() => setIsAddingCustom(false)}
                        className="px-3.5 py-1.5 rounded-full text-xs app-text-sub cursor-pointer"
                      >
                        Cancel
                      </button>
                      <button
                        type="button"
                        disabled={!customName.trim()}
                        onClick={handleSaveCustomItem}
                        className={`px-4 py-1.5 rounded-full text-xs font-semibold disabled:opacity-40 cursor-pointer ${
                          isDarkMode ? 'bg-white text-black' : 'bg-neutral-900 text-white'
                        }`}
                      >
                        Save
                      </button>
                    </div>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

            {activeCustomList.length === 0 && !isAddingCustom ? (
              <div className={`p-10 text-center rounded-2xl border space-y-2.5 ${
                isDarkMode ? 'bg-white/[0.025] border-white/8' : 'bg-white/80 border-neutral-200'
              }`}>
                <div className="opacity-45 flex justify-center">
                  {pageType === 'connectors' ? (
                    <Puzzle className="w-8 h-8" />
                  ) : pageType === 'agents' ? (
                    <Bot className="w-8 h-8" />
                  ) : (
                    <Zap className="w-8 h-8" />
                  )}
                </div>
                <div className="text-sm font-medium app-text-title">
                  No {PAGE_TITLES[pageType]} Configured
                </div>
                <p className="text-xs app-text-sub">
                  Tap + in the top right to add one.
                </p>
              </div>
            ) : (
              <div className="space-y-2.5">
                {activeCustomList.map((item) => (
                  <div
                    key={item.id}
                    className={`p-3.5 rounded-2xl border flex items-center justify-between ${
                      isDarkMode
                        ? 'bg-white/[0.04] border-white/10'
                        : 'bg-white border-neutral-200 shadow-sm'
                    }`}
                  >
                    <div className="min-w-0">
                      <div className="font-medium text-sm app-text-title truncate">{item.name}</div>
                      <div className="text-xs app-text-sub truncate">{item.type}</div>
                    </div>
                    <button
                      type="button"
                      onClick={() =>
                        setPendingDelete({
                          kind: 'custom',
                          id: item.id,
                          label: item.name,
                        })
                      }
                      className="p-2 rounded-xl text-neutral-400 hover:text-red-500 transition-colors cursor-pointer"
                      aria-label={`Delete ${item.name}`}
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
        </div>
      </div>

      <ConfirmDeleteModal
        isOpen={Boolean(pendingDelete)}
        title={`Delete ${pendingDelete?.label || 'Item'}?`}
        description="This will permanently remove this item from your local workspace."
        confirmLabel="Delete"
        onCancel={() => setPendingDelete(null)}
        onConfirm={() => {
          if (!pendingDelete) return;
          if (pendingDelete.kind === 'key') {
            handleDeleteKey(pendingDelete.id);
          } else {
            handleDeleteCustomItem(pendingDelete.id);
          }
          setPendingDelete(null);
        }}
        isDarkMode={isDarkMode}
      />
    </div>
  );
}

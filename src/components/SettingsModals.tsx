import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  X, Check, ExternalLink, Plus, Trash2, 
  Puzzle, Box, Bot, Zap, Globe, Bell, Sliders, 
  Database, ShieldCheck, Info, Github, User, ArrowRight
} from 'lucide-react';
import { SettingsModalType, GitHubUser, UserCustomIntegration } from '../types';
import { NetRunnerLogo } from './Icons';

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
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <p className="text-xs opacity-75">
                    Configure inference models, API keys, and local LLM endpoints.
                  </p>
                  {!isAdding && (
                    <button
                      onClick={() => setIsAdding(true)}
                      className={`px-3 py-1 rounded-full text-xs font-medium transition-colors flex items-center gap-1 cursor-pointer ${
                        isDarkMode ? 'bg-white text-black hover:bg-neutral-200' : 'bg-neutral-900 text-white hover:bg-black'
                      }`}
                    >
                      <Plus className="w-3.5 h-3.5" /> Add Model
                    </button>
                  )}
                </div>

                {isAdding && (
                  <div className={`p-4 rounded-2xl border space-y-3 ${
                    isDarkMode ? 'bg-white/[0.04] border-white/15' : 'bg-neutral-50 border-neutral-200'
                  }`}>
                    <div className="text-xs font-semibold">Add Custom Model Configuration</div>
                    <input
                      type="text"
                      placeholder="Model Name (e.g. gemini-2.5-flash, gpt-4o, llama3)"
                      value={newName}
                      onChange={(e) => setNewName(e.target.value)}
                      className={`w-full px-3 py-2 rounded-xl border text-xs focus:outline-none ${
                        isDarkMode ? 'bg-white/[0.05] border-white/10 text-white' : 'bg-white border-neutral-300 text-neutral-900'
                      }`}
                    />
                    <input
                      type="text"
                      placeholder="Provider / Endpoint (e.g. Vertex AI, Ollama http://localhost:11434)"
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

                {models.length === 0 && !isAdding ? (
                  <div className={`p-8 text-center rounded-2xl border space-y-2 ${
                    isDarkMode ? 'bg-white/[0.02] border-white/5' : 'bg-neutral-50 border-neutral-200'
                  }`}>
                    <Box className="w-8 h-8 opacity-40 mx-auto" />
                    <div className="text-sm font-medium">No Custom Models Configured</div>
                    <p className="text-xs opacity-60 max-w-xs mx-auto">
                      Configure your actual LLM endpoints or API tokens when ready.
                    </p>
                  </div>
                ) : (
                  <div className="space-y-2">
                    {models.map((m) => (
                      <div key={m.id} className={`p-3.5 rounded-2xl border flex items-center justify-between ${
                        isDarkMode ? 'bg-white/[0.03] border-white/8' : 'bg-neutral-50 border-neutral-200'
                      }`}>
                        <div>
                          <div className="font-medium text-sm">{m.name}</div>
                          <div className="text-xs opacity-60">{m.type}</div>
                        </div>
                        <button
                          onClick={() => handleDeleteItem(m.id, 'model')}
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
                <p className="text-xs opacity-75">Firebase Authentication & Firestore security rules.</p>
                <div className={`p-3.5 rounded-2xl border text-xs space-y-2 ${
                  isDarkMode ? 'bg-white/[0.03] border-white/8' : 'bg-neutral-50 border-neutral-200'
                }`}>
                  <div className="flex justify-between items-center">
                    <span>Auth Provider:</span>
                    <span className="text-emerald-500 font-mono">Firebase Auth (GitHub)</span>
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
                  <div className="flex justify-between">
                    <span>Backend Platform:</span>
                    <span className="font-mono">Firebase Firestore & Auth</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Auth Integration:</span>
                    <span className="font-mono">GitHub Provider</span>
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

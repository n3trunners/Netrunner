import React, { useState, useEffect } from 'react';
import { motion } from 'motion/react';
import { 
  Plus, Search, ChevronRight, Settings, 
  Trash2, ExternalLink, Github, X, Key 
} from 'lucide-react';
import { NetRunnerLogo, GlossyFolderGraphic } from './Icons';
import { Project, GitHubUser, GitHubRepo } from '../types';
import { BYOK_UPDATED_EVENT, getByokConfig } from '../utils/byok';

interface DashboardViewProps {
  projects: Project[];
  onOpenNewProjectModal: () => void;
  onOpenSettings: () => void;
  onOpenTokenSettings: () => void;
  onOpenProjectWorkspace: (project: Project) => void;
  onOpenHybridStudio: () => void;
  onDeleteProject: (id: string) => void;
  onShowToast: (msg: string) => void;
  currentUser: GitHubUser | null;
  userRepos: GitHubRepo[];
  onOpenGitHubAuth: () => void;
  onImportGitHubRepo: (repo: GitHubRepo) => void;
  isDarkMode?: boolean;
}

export function DashboardView({
  projects,
  onOpenNewProjectModal,
  onOpenSettings,
  onOpenTokenSettings,
  onOpenProjectWorkspace,
  onOpenHybridStudio,
  onDeleteProject,
  onShowToast,
  currentUser,
  userRepos,
  onOpenGitHubAuth,
  onImportGitHubRepo,
  isDarkMode = true
}: DashboardViewProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTag, setSelectedTag] = useState<string | null>(null);
  const [byokState, setByokState] = useState(() => getByokConfig());

  useEffect(() => {
    const sync = () => setByokState(getByokConfig());
    window.addEventListener(BYOK_UPDATED_EVENT, sync);
    return () => window.removeEventListener(BYOK_UPDATED_EVENT, sync);
  }, []);

  const filteredProjects = projects.filter(p => {
    const matchesSearch = 
      p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.category.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesTag = !selectedTag || p.category === selectedTag;
    return matchesSearch && matchesTag;
  });

  return (
    <div className="relative w-full h-full pb-28 overflow-y-auto select-none">
      <div className="relative z-10 px-5 pt-4 space-y-6">
        {/* Top Header: Real NetRunner Logo + Divider + Brand Text + Settings Gear Button */}
        <div className="flex items-center justify-between pt-1">
          <div className="flex items-center gap-3">
            <NetRunnerLogo className="w-7 h-7" isDark={isDarkMode} />
            <div className="h-5 w-[1.5px] app-divider" />
            <span className="font-semibold tracking-[0.25em] text-sm uppercase app-text-title">
              NETRUNNER
            </span>
          </div>

          <div className="flex items-center gap-2">
            {currentUser ? (
              <div 
                onClick={onOpenSettings}
                className={`w-8 h-8 rounded-full border overflow-hidden cursor-pointer hover:ring-2 transition-all ${
                  isDarkMode ? 'border-white/20 hover:ring-white/40' : 'border-neutral-300 hover:ring-neutral-400'
                }`}
                title={`Signed in as @${currentUser.login}`}
              >
                <img src={currentUser.avatar_url} alt={currentUser.login} className="w-full h-full object-cover" />
              </div>
            ) : (
              <button
                onClick={onOpenGitHubAuth}
                className={`px-3 py-1 rounded-full border text-[11px] font-medium flex items-center gap-1.5 transition-colors cursor-pointer ${
                  isDarkMode 
                    ? 'bg-white/5 hover:bg-white/10 border-white/10 text-neutral-200' 
                    : 'bg-neutral-100 hover:bg-neutral-200 border-neutral-300 text-neutral-800'
                }`}
                title="Sign in with GitHub"
              >
                <Github className="w-3.5 h-3.5" />
                <span>Sign In</span>
              </button>
            )}

            <button
              onClick={onOpenSettings}
              className={`w-9 h-9 rounded-full border active:scale-95 transition-all flex items-center justify-center shadow-md group cursor-pointer ${
                isDarkMode 
                  ? 'bg-white/[0.05] border-white/12 hover:bg-white/[0.1] text-white/90' 
                  : 'bg-neutral-100 border-neutral-300 hover:bg-neutral-200 text-neutral-800'
              }`}
              aria-label="Open Settings"
            >
              <Settings className="w-4 h-4 group-hover:rotate-45 transition-transform duration-300" />
            </button>
          </div>
        </div>

        {/* Welcome Section */}
        <motion.div 
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.35, ease: 'easeOut' }}
          className="pt-2"
        >
          <div className="app-text-sub font-medium text-lg leading-tight">
            Welcome back,
          </div>
          <h1 className="text-4xl sm:text-5xl font-bold tracking-tight mt-1 truncate app-text-title">
            {currentUser ? (currentUser.name ? currentUser.name.split(' ')[0] : currentUser.login) : 'Developer'}.
          </h1>
          <p className="app-text-sub text-sm mt-1.5 font-normal tracking-wide">
            Build. Code. Innovate.
          </p>
          <div className={`w-12 h-[2.5px] rounded-full mt-3.5 ${isDarkMode ? 'bg-neutral-600' : 'bg-neutral-400'}`} />
        </motion.div>

        {/* Action Capsule Button: + New Project > */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2.5">
          <motion.button
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.35, delay: 0.1 }}
            onClick={onOpenNewProjectModal}
            className="app-pill-btn rounded-full py-3.5 px-6 flex items-center justify-between font-medium text-sm group cursor-pointer active:scale-[0.98] transition-all flex-1"
          >
            <div className="flex items-center gap-3">
              <Plus className="w-5 h-5 group-hover:scale-110 transition-transform" />
              <span className="font-semibold tracking-wide text-[15px]">New Project</span>
            </div>
            <ChevronRight className="w-4 h-4 opacity-70 group-hover:opacity-100 group-hover:translate-x-1 transition-all" />
          </motion.button>

          {/* Quick Import from GitHub if logged in */}
          {currentUser && userRepos.length > 0 && (
            <button
              onClick={() => {
                const firstRepo = userRepos[0];
                if (firstRepo) {
                  onImportGitHubRepo(firstRepo);
                }
              }}
              className={`px-4 py-2 rounded-full border text-xs font-medium transition-colors flex items-center justify-center gap-1.5 ${
                isDarkMode 
                  ? 'bg-white/5 hover:bg-white/10 border-white/10 text-neutral-300 hover:text-white' 
                  : 'bg-white hover:bg-neutral-50 border-neutral-300 text-neutral-800 shadow-sm'
              }`}
              title="Import your latest GitHub repo into NetRunner"
            >
              <Github className="w-3.5 h-3.5" />
              <span>Import @{currentUser.login}/{userRepos[0]?.name}</span>
            </button>
          )}
        </div>

        {/* Search Bar Input */}
        <motion.div 
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.35, delay: 0.15 }}
          className="relative"
        >
          <div className="w-full flex items-center app-input rounded-full px-4 py-3 text-sm transition-all shadow-inner">
            <Search className="w-4 h-4 app-text-muted mr-3 flex-shrink-0" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search projects..."
              className="w-full bg-transparent placeholder-neutral-400 text-sm focus:outline-none"
            />
            {searchQuery && (
              <button 
                onClick={() => setSearchQuery('')}
                className="p-1 rounded-full text-neutral-400 hover:text-neutral-600"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        </motion.div>

        {/* Quick Access Cards: Hybrid Studio & BYOK AI Token */}
        <div className="space-y-2.5">
          <button
            type="button"
            onClick={onOpenTokenSettings}
            className="w-full p-4 rounded-2xl app-card transition-all flex items-center justify-between text-left group cursor-pointer"
          >
            <div className="flex items-center gap-3 min-w-0 pr-3">
              <div className={`w-9 h-9 rounded-xl border flex items-center justify-center shrink-0 ${
                byokState.hasToken
                  ? 'bg-emerald-500/10 border-emerald-500/25 text-emerald-400'
                  : 'bg-amber-500/10 border-amber-500/25 text-amber-400'
              }`}>
                <Key className="w-4 h-4" />
              </div>
              <div className="min-w-0">
                <div className="font-semibold text-sm app-text-title truncate">
                  {byokState.hasToken ? 'Personal AI Token Active' : 'Add Your AI Token (BYOK)'}
                </div>
                <div className="text-xs app-text-sub mt-0.5 truncate">
                  {byokState.hasToken
                    ? `${byokState.model} · Using your own key`
                    : 'No free tokens provided · Paste your Gemini key'}
                </div>
              </div>
            </div>
            <ChevronRight className="w-4 h-4 app-text-muted group-hover:translate-x-0.5 transition-transform shrink-0" />
          </button>

          <button
            type="button"
            onClick={onOpenHybridStudio}
            className="w-full p-4 rounded-2xl app-card transition-all flex items-center justify-between text-left group cursor-pointer"
          >
            <div className="min-w-0 pr-3">
              <div className="font-semibold text-sm app-text-title truncate">
                Hybrid Architecture & CI/CD
              </div>
              <div className="text-xs app-text-sub mt-0.5 truncate">
                Kotlin + Jetpack Compose · Rust Daemon · GitHub Actions
              </div>
            </div>
            <ChevronRight className="w-4 h-4 app-text-muted group-hover:translate-x-0.5 transition-transform shrink-0" />
          </button>
        </div>

        {/* Main Content Area: Empty State OR Projects List */}
        {projects.length === 0 ? (
          /* Empty State - Matching Image 1 perfectly */
          <motion.div 
            initial={{ opacity: 0, scale: 0.96 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.4, delay: 0.2 }}
            className="pt-10 pb-6 flex flex-col items-center justify-center text-center"
          >
            <div className="mb-4">
              <GlossyFolderGraphic className="w-32 h-32" />
            </div>

            <h3 className="text-xl font-bold tracking-tight app-text-title">
              No Projects Yet
            </h3>
            <p className="text-sm mt-1 max-w-xs app-text-sub">
              Create your first project to get started.
            </p>

            <button
              onClick={onOpenNewProjectModal}
              className={`mt-6 px-6 py-2.5 rounded-full border font-medium text-sm flex items-center gap-2 active:scale-95 transition-all shadow-lg group cursor-pointer ${
                isDarkMode 
                  ? 'bg-white/[0.07] hover:bg-white/[0.12] border-white/15 text-white' 
                  : 'bg-neutral-900 hover:bg-black border-neutral-900 text-white'
              }`}
            >
              <Plus className="w-4 h-4 group-hover:rotate-90 transition-transform duration-200" />
              <span>Create Project</span>
            </button>
          </motion.div>
        ) : (
          /* Projects List */
          <div className="space-y-4 pt-2">
            <div className="flex items-center justify-between text-xs font-semibold tracking-wide app-text-sub">
              <span>Your Projects ({filteredProjects.length})</span>
              <button
                onClick={() => {
                  projects.forEach(p => onDeleteProject(p.id));
                  onShowToast('Cleared workspace projects to empty state');
                }}
                className="hover:underline transition-colors cursor-pointer"
              >
                Reset to Empty State
              </button>
            </div>

            <div className="flex items-center gap-1.5 overflow-x-auto pb-1 text-xs">
              <button
                onClick={() => setSelectedTag(null)}
                className={`px-3 py-1 rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                  selectedTag === null 
                    ? (isDarkMode ? 'bg-white text-black font-semibold' : 'bg-neutral-900 text-white font-semibold')
                    : (isDarkMode ? 'bg-white/5 text-neutral-400 hover:text-white' : 'bg-neutral-200 text-neutral-700 hover:text-black')
                }`}
              >
                All
              </button>
              {['Hybrid', 'AI Agent', 'Fullstack', 'Frontend', 'API'].map(cat => (
                <button
                  key={cat}
                  onClick={() => setSelectedTag(cat)}
                  className={`px-3 py-1 rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                    selectedTag === cat 
                      ? (isDarkMode ? 'bg-white text-black font-semibold' : 'bg-neutral-900 text-white font-semibold')
                      : (isDarkMode ? 'bg-white/5 text-neutral-400 hover:text-white' : 'bg-neutral-200 text-neutral-700 hover:text-black')
                  }`}
                >
                  {cat}
                </button>
              ))}
            </div>

            {filteredProjects.length === 0 ? (
              <div className="p-8 text-center text-sm rounded-2xl app-card app-text-sub">
                No projects matched "{searchQuery}"
              </div>
            ) : (
              <div className="space-y-2.5">
                {filteredProjects.map((proj) => (
                  <motion.div
                    key={proj.id}
                    layout
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.95 }}
                    onClick={() => onOpenProjectWorkspace(proj)}
                    className="p-4 rounded-2xl app-card transition-all flex items-center justify-between group cursor-pointer"
                  >
                    <div className="space-y-1 pr-3 min-w-0">
                      <div className="font-semibold text-[15px] truncate app-text-title">
                        {proj.name}
                      </div>
                      <p className="text-xs app-text-sub line-clamp-1">{proj.description}</p>
                      <div className="flex items-center gap-2 text-[11px] app-text-muted font-mono tabular-nums pt-1">
                        <span>{proj.category}</span>
                        <span aria-hidden="true">·</span>
                        <span>Updated {proj.updatedAt}</span>
                        <span aria-hidden="true">·</span>
                        <span>{proj.filesCount} modules</span>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0" onClick={(e) => e.stopPropagation()}>
                      <button
                        onClick={() => onOpenProjectWorkspace(proj)}
                        className={`p-2 rounded-xl transition-colors cursor-pointer ${
                          isDarkMode ? 'bg-white/5 hover:bg-white/15 text-neutral-300' : 'bg-neutral-100 hover:bg-neutral-200 text-neutral-700'
                        }`}
                        title="Open Project Workspace"
                      >
                        <ExternalLink className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => {
                          onDeleteProject(proj.id);
                          onShowToast(`Deleted ${proj.name}`);
                        }}
                        className="p-2 rounded-xl bg-red-500/10 hover:bg-red-500/20 text-red-400 transition-colors cursor-pointer"
                        title="Delete Project"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </motion.div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

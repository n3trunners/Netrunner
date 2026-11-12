import React from 'react';
import { motion } from 'motion/react';
import { 
  Github, Star, GitFork, BookOpen, ExternalLink, 
  MapPin, Building, Globe, ArrowLeft, LogOut, RefreshCw, 
  Settings as SettingsIcon, ShieldCheck 
} from 'lucide-react';
import { GitHubUser, GitHubStats, GitHubRepo } from '../types';

interface ProfileViewProps {
  onBackToHome: () => void;
  onOpenSettings: () => void;
  onShowToast: (msg: string) => void;
  currentUser: GitHubUser | null;
  userStats: GitHubStats | null;
  userRepos: GitHubRepo[];
  onOpenGitHubAuth: () => void;
  onSignOut: () => void;
  onRefreshStats: () => void;
  isRefreshing: boolean;
  isDarkMode?: boolean;
}

export function ProfileView({
  onBackToHome,
  onOpenSettings,
  onShowToast,
  currentUser,
  userStats,
  userRepos,
  onOpenGitHubAuth,
  onSignOut,
  onRefreshStats,
  isRefreshing,
  isDarkMode = true
}: ProfileViewProps) {
  return (
    <div className="relative w-full h-full pb-28 overflow-y-auto select-none">
      <div className="relative z-10 px-5 pt-4 space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between pt-1">
          <div className="flex items-center gap-3">
            <button 
              onClick={onBackToHome}
              className="p-1 -ml-1 app-text-sub hover:app-text-title transition-colors cursor-pointer"
              aria-label="Back to workspace"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            <div className="h-4 w-[1px] app-divider" />
            <h1 className="text-base font-bold tracking-wide app-text-title">Developer Profile</h1>
          </div>

          <div className="flex items-center gap-1.5">
            {currentUser && (
              <button 
                onClick={onRefreshStats}
                disabled={isRefreshing}
                className={`p-2 rounded-full transition-colors cursor-pointer ${
                  isDarkMode ? 'bg-white/5 hover:bg-white/10 text-neutral-300' : 'bg-neutral-100 hover:bg-neutral-200 text-neutral-700'
                }`}
                title="Refresh Real GitHub Stats"
              >
                <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
              </button>
            )}
            <button 
              onClick={onOpenSettings}
              className={`p-2 rounded-full transition-colors cursor-pointer ${
                isDarkMode ? 'bg-white/5 hover:bg-white/10 text-neutral-300' : 'bg-neutral-100 hover:bg-neutral-200 text-neutral-700'
              }`}
              title="Settings"
            >
              <SettingsIcon className="w-4 h-4" />
            </button>
          </div>
        </div>

        {currentUser ? (
          /* REAL GITHUB PROFILE & REAL STATS */
          <div className="space-y-5">
            {/* Real Profile Card */}
            <motion.div 
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className="p-5 rounded-3xl app-card flex flex-col items-center text-center shadow-md relative overflow-hidden"
            >
              <div className={`relative w-20 h-20 rounded-full border-2 overflow-hidden shadow-md mb-3 ${
                isDarkMode ? 'border-white/20' : 'border-neutral-300'
              }`}>
                <img
                  src={currentUser.avatar_url}
                  alt={currentUser.login}
                  className="w-full h-full object-cover"
                />
              </div>

              <h2 className="text-xl font-bold app-text-title">
                {currentUser.name || currentUser.login}
              </h2>
              
              <a
                href={currentUser.html_url}
                target="_blank"
                rel="noreferrer"
                className="text-xs app-text-sub hover:app-text-title flex items-center gap-1 mt-0.5 group"
              >
                <Github className="w-3.5 h-3.5" />
                <span>@{currentUser.login}</span>
                <ExternalLink className="w-3 h-3 opacity-60 group-hover:opacity-100" />
              </a>

              {currentUser.bio && (
                <p className="text-xs app-text-sub mt-2.5 max-w-xs leading-relaxed">
                  {currentUser.bio}
                </p>
              )}

              {/* Meta details */}
              <div className="flex flex-wrap items-center justify-center gap-3 mt-3 text-[11px] app-text-muted">
                {currentUser.location && (
                  <span className="flex items-center gap-1">
                    <MapPin className="w-3 h-3" />
                    <span>{currentUser.location}</span>
                  </span>
                )}
                {currentUser.company && (
                  <span className="flex items-center gap-1">
                    <Building className="w-3 h-3" />
                    <span>{currentUser.company}</span>
                  </span>
                )}
                {currentUser.blog && (
                  <a 
                    href={currentUser.blog.startsWith('http') ? currentUser.blog : `https://${currentUser.blog}`} 
                    target="_blank" 
                    rel="noreferrer"
                    className="flex items-center gap-1 hover:underline"
                  >
                    <Globe className="w-3 h-3" />
                    <span>{currentUser.blog.replace(/^https?:\/\//, '')}</span>
                  </a>
                )}
              </div>
            </motion.div>

            {/* REAL STATS GRID */}
            <div className="space-y-2">
              <div className="flex items-center justify-between text-xs font-semibold app-text-sub">
                <span>Real GitHub Metrics</span>
                <span className="text-[10px] text-emerald-500 font-mono">Live Synced</span>
              </div>

              <div className="grid grid-cols-4 gap-2">
                <div className="p-3 rounded-2xl app-card text-center">
                  <div className="text-base sm:text-lg font-bold app-text-title font-mono">
                    {currentUser.public_repos}
                  </div>
                  <div className="text-[10px] app-text-sub mt-0.5">Repos</div>
                </div>

                <div className="p-3 rounded-2xl app-card text-center">
                  <div className="text-base sm:text-lg font-bold text-amber-500 font-mono flex items-center justify-center gap-0.5">
                    <Star className="w-3 h-3 fill-amber-500" />
                    <span>{userStats ? userStats.totalStars : 0}</span>
                  </div>
                  <div className="text-[10px] app-text-sub mt-0.5">Stars</div>
                </div>

                <div className="p-3 rounded-2xl app-card text-center">
                  <div className="text-base sm:text-lg font-bold app-text-title font-mono">
                    {currentUser.followers}
                  </div>
                  <div className="text-[10px] app-text-sub mt-0.5">Followers</div>
                </div>

                <div className="p-3 rounded-2xl app-card text-center">
                  <div className="text-base sm:text-lg font-bold app-text-title font-mono">
                    {currentUser.following}
                  </div>
                  <div className="text-[10px] app-text-sub mt-0.5">Following</div>
                </div>
              </div>
            </div>

            {/* Top Languages */}
            {userStats && userStats.topLanguages.length > 0 && (
              <div className="p-3.5 rounded-2xl app-card space-y-2">
                <div className="text-xs font-semibold app-text-sub">Primary Languages</div>
                <div className="flex flex-wrap gap-2 pt-1">
                  {userStats.topLanguages.slice(0, 5).map((lang) => (
                    <span 
                      key={lang.language} 
                      className={`px-2.5 py-1 rounded-full border text-xs flex items-center gap-1.5 ${
                        isDarkMode ? 'bg-white/5 border-white/10 text-neutral-200' : 'bg-neutral-100 border-neutral-200 text-neutral-800'
                      }`}
                    >
                      <span className={`w-2 h-2 rounded-full ${isDarkMode ? 'bg-white/70' : 'bg-neutral-800'}`} />
                      <span>{lang.language}</span>
                      <span className="text-[10px] app-text-muted font-mono">({lang.count})</span>
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* REAL PUBLIC REPOSITORIES */}
            <div className="space-y-2.5">
              <div className="flex items-center justify-between text-xs font-semibold app-text-sub">
                <span>Recent Repositories</span>
                <a
                  href={`${currentUser.html_url}?tab=repositories`}
                  target="_blank"
                  rel="noreferrer"
                  className="hover:underline flex items-center gap-1"
                >
                  <span>View all on GitHub</span>
                  <ExternalLink className="w-3 h-3" />
                </a>
              </div>

              {userRepos.length === 0 ? (
                <div className="p-6 text-center text-xs app-text-sub rounded-2xl app-card">
                  No public repositories found.
                </div>
              ) : (
                <div className="space-y-2">
                  {userRepos.slice(0, 6).map((repo) => (
                    <a
                      key={repo.id}
                      href={repo.html_url}
                      target="_blank"
                      rel="noreferrer"
                      className="p-3 rounded-2xl app-card transition-all flex items-center justify-between group block"
                    >
                      <div className="space-y-1 pr-3 flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <BookOpen className="w-3.5 h-3.5 app-text-muted shrink-0" />
                          <span className="font-semibold text-xs truncate group-hover:underline app-text-title">
                            {repo.name}
                          </span>
                        </div>
                        {repo.description && (
                          <p className="text-[11px] app-text-sub truncate">
                            {repo.description}
                          </p>
                        )}
                        <div className="flex items-center gap-3 text-[10px] app-text-muted font-mono">
                          {repo.language && (
                            <span className="font-medium">{repo.language}</span>
                          )}
                          <span className="flex items-center gap-0.5">
                            <Star className="w-2.5 h-2.5 text-amber-500" />
                            <span>{repo.stars}</span>
                          </span>
                          <span className="flex items-center gap-0.5">
                            <GitFork className="w-2.5 h-2.5" />
                            <span>{repo.forks}</span>
                          </span>
                        </div>
                      </div>
                      <ExternalLink className="w-4 h-4 app-text-muted group-hover:app-text-title shrink-0" />
                    </a>
                  ))}
                </div>
              )}
            </div>

            {/* Account Actions */}
            <div className="pt-2 flex gap-2">
              <button
                onClick={onOpenGitHubAuth}
                className={`flex-1 py-2.5 rounded-2xl border text-xs font-medium transition-colors cursor-pointer ${
                  isDarkMode ? 'bg-white/5 hover:bg-white/10 border-white/10 text-neutral-200' : 'bg-neutral-100 hover:bg-neutral-200 border-neutral-300 text-neutral-800'
                }`}
              >
                Switch Account
              </button>
              <button
                onClick={onSignOut}
                className="px-4 py-2.5 rounded-2xl bg-red-500/10 hover:bg-red-500/20 border border-red-500/20 text-xs font-medium text-red-500 transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span>Sign Out</span>
              </button>
            </div>
          </div>
        ) : (
          /* NOT LOGGED IN */
          <motion.div 
            initial={{ opacity: 0, scale: 0.96 }}
            animate={{ opacity: 1, scale: 1 }}
            className="pt-8 pb-12 flex flex-col items-center text-center space-y-4 max-w-sm mx-auto"
          >
            <div className={`w-20 h-20 rounded-3xl border flex items-center justify-center shadow-xl ${
              isDarkMode ? 'bg-white/[0.05] border-white/15 text-white' : 'bg-neutral-900 border-neutral-800 text-white'
            }`}>
              <Github className="w-10 h-10" />
            </div>

            <div>
              <h2 className="text-xl font-bold tracking-tight app-text-title">
                Connect GitHub Account
              </h2>
              <p className="text-xs app-text-sub mt-1.5 leading-relaxed">
                Sign in with GitHub to view your real statistics, explore your public repositories, and sync your developer identity.
              </p>
            </div>

            <div className="w-full pt-2">
              <button
                onClick={onOpenGitHubAuth}
                className={`w-full py-3.5 px-6 rounded-full font-semibold text-xs active:scale-[0.98] transition-all flex items-center justify-center gap-2 shadow-lg cursor-pointer ${
                  isDarkMode ? 'bg-white text-black hover:bg-neutral-200' : 'bg-neutral-900 text-white hover:bg-black'
                }`}
              >
                <Github className="w-4 h-4" />
                <span>Sign In with GitHub</span>
              </button>
            </div>
          </motion.div>
        )}
      </div>
    </div>
  );
}

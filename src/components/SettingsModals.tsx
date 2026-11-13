import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { X, Check, ExternalLink, Github, Clock } from 'lucide-react';
import { SettingsModalType, GitHubUser } from '../types';
import { NetRunnerLogo, DiscordLogo, XLogo } from './Icons';

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
  const [selectedLanguage, setSelectedLanguage] = useState('en-US');
  const [notifications, setNotifications] = useState({
    buildAlerts: true,
    securityNotices: true,
    commitSync: true
  });

  if (!activeModal) return null;

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        transition={{ duration: 0.18 }}
        onClick={onClose}
        className={`absolute inset-0 z-50 flex items-center justify-center p-4 ${
          isDarkMode ? 'bg-black/28' : 'bg-slate-900/12'
        }`}
      >
        <motion.div
          initial={{ opacity: 0, scale: 0.94, y: 14 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.94, y: 14 }}
          transition={{ duration: 0.24, ease: [0.16, 1, 0.3, 1] }}
          onClick={(e) => e.stopPropagation()}
          className={`relative w-full max-w-md max-h-[85vh] flex flex-col rounded-[28px] border backdrop-blur-[28px] backdrop-saturate-150 overflow-hidden transition-colors ${
            isDarkMode
              ? 'bg-[#0b0f18]/44 border-white/22 text-neutral-200 shadow-[0_32px_90px_rgba(0,0,0,0.75),inset_0_1px_0_rgba(255,255,255,0.22)]'
              : 'bg-white/56 border-white/90 text-neutral-800 shadow-[0_28px_72px_rgba(15,23,42,0.16),inset_0_1px_0_rgba(255,255,255,0.95)]'
          }`}
        >
          {/* Top specular edge gleam */}
          <div
            aria-hidden="true"
            className={`pointer-events-none absolute inset-x-6 top-0 h-px ${
              isDarkMode
                ? 'bg-gradient-to-r from-transparent via-white/45 to-transparent'
                : 'bg-gradient-to-r from-transparent via-white to-transparent'
            }`}
          />

          {/* Modal Header */}
          <div className={`flex items-center justify-between px-6 py-4 border-b ${
            isDarkMode ? 'border-white/10 bg-white/[0.03]' : 'border-black/5 bg-white/30'
          }`}>
            <div className="flex items-center gap-2.5">
              <NetRunnerLogo className="w-5 h-5" isDark={isDarkMode} />
              <h3 className="font-semibold tracking-wide text-sm capitalize">
                {activeModal === 'about' ? 'About NetRunner' : activeModal.replace('-', ' ')}
              </h3>
            </div>
            <button
              onClick={onClose}
              className={`p-1.5 rounded-full transition-colors cursor-pointer ${
                isDarkMode ? 'text-neutral-300 hover:text-white hover:bg-white/12' : 'text-neutral-500 hover:text-black hover:bg-black/8'
              }`}
              aria-label="Close"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Modal Content */}
          <div className="p-6 overflow-y-auto space-y-4 text-sm">
            {/* Profile Detail */}
            {activeModal === 'profile-detail' && (
              <div className="space-y-4">
                {currentUser ? (
                  <div className="space-y-4">
                    <div className={`flex items-center gap-3.5 p-4 rounded-2xl border ${
                      isDarkMode ? 'bg-white/[0.06] border-white/12' : 'bg-white/60 border-white/80'
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

                    <div className={`p-3.5 rounded-2xl border text-xs space-y-1.5 ${
                      isDarkMode ? 'bg-white/[0.04] border-white/10 opacity-85' : 'bg-white/50 border-white/80 text-neutral-700'
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
                        className="flex-1 py-2.5 rounded-xl bg-red-500/15 hover:bg-red-500/25 text-red-400 text-xs font-medium border border-red-500/25 transition-colors cursor-pointer"
                      >
                        Sign Out
                      </button>
                      <button
                        onClick={onOpenGitHubAuth}
                        className={`flex-1 py-2.5 rounded-xl border text-xs font-medium transition-colors cursor-pointer ${
                          isDarkMode ? 'bg-white/8 hover:bg-white/15 border-white/15 text-white' : 'bg-white/70 hover:bg-white border-neutral-300 text-neutral-800'
                        }`}
                      >
                        Switch Account
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-4 text-center py-2">
                    <div className={`w-14 h-14 rounded-full border mx-auto flex items-center justify-center ${
                      isDarkMode ? 'bg-white/8 border-white/15' : 'bg-white/70 border-neutral-300 text-neutral-900'
                    }`}>
                      <Github className="w-7 h-7" />
                    </div>
                    <div>
                      <h4 className="text-base font-bold">No GitHub Account Connected</h4>
                      <p className="text-xs opacity-75 mt-1 max-w-xs mx-auto">
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
                        ? (isDarkMode ? 'bg-white/15 border-white/30 font-semibold' : 'bg-white/85 border-neutral-400 font-semibold')
                        : (isDarkMode ? 'bg-white/[0.05] border-white/10 hover:bg-white/[0.09]' : 'bg-white/45 border-white/75 hover:bg-white/75')
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
                    isDarkMode ? 'bg-white/[0.05] border-white/10' : 'bg-white/55 border-white/80'
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

            {/* Editor Preferences / Data & Storage / Security -> Currently Unavailable */}
            {(activeModal === 'editor' || activeModal === 'data-storage' || activeModal === 'security') && (
              <div className="py-8 px-4 flex flex-col items-center justify-center text-center space-y-3">
                <div className={`w-12 h-12 rounded-2xl border flex items-center justify-center ${
                  isDarkMode ? 'bg-white/[0.07] border-white/15 text-neutral-300' : 'bg-white/70 border-neutral-300/80 text-neutral-700'
                }`}>
                  <Clock className="w-5 h-5" />
                </div>
                <div className="space-y-1">
                  <h4 className="text-sm font-semibold">Currently Unavailable</h4>
                  <p className="text-xs opacity-70 max-w-[240px]">
                    This section is currently unavailable and will be enabled in a future release.
                  </p>
                </div>
              </div>
            )}

            {/* About */}
            {activeModal === 'about' && (
              <div className="py-6 flex flex-col items-center justify-center text-center space-y-5">
                <div className={`w-16 h-16 rounded-3xl border flex items-center justify-center shadow-lg ${
                  isDarkMode ? 'bg-white/[0.08] border-white/18' : 'bg-white/75 border-white/95'
                }`}>
                  <NetRunnerLogo className="w-9 h-9" isDark={isDarkMode} />
                </div>
                <h4 className="text-lg font-bold tracking-[0.2em]">NETRUNNER</h4>
                <div className="flex items-center justify-center gap-3.5">
                  <a
                    href="https://discord.gg/YKCdUCdGE"
                    target="_blank"
                    rel="noopener noreferrer"
                    aria-label="Join NetRunner Discord"
                    title="Discord"
                    className={`w-11 h-11 rounded-full border flex items-center justify-center transition-all hover:scale-105 active:scale-95 ${
                      isDarkMode
                        ? 'bg-[#5865F2]/18 border-[#5865F2]/40 text-[#7289DA] hover:bg-[#5865F2]/28'
                        : 'bg-[#5865F2]/12 border-[#5865F2]/35 text-[#5865F2] hover:bg-[#5865F2]/22'
                    }`}
                  >
                    <DiscordLogo className="w-5 h-5" />
                  </a>
                  <a
                    href="https://x.com/Netrunnerapp"
                    target="_blank"
                    rel="noopener noreferrer"
                    aria-label="Follow NetRunner on X"
                    title="X (@Netrunnerapp)"
                    className={`w-11 h-11 rounded-full border flex items-center justify-center transition-all hover:scale-105 active:scale-95 ${
                      isDarkMode
                        ? 'bg-white/[0.09] border-white/22 text-white hover:bg-white/[0.16]'
                        : 'bg-neutral-900/8 border-neutral-900/20 text-neutral-900 hover:bg-neutral-900/14'
                    }`}
                  >
                    <XLogo className="w-4 h-4" />
                  </a>
                </div>
              </div>
            )}
          </div>

          {/* Modal Footer */}
          <div className={`px-6 py-3.5 border-t flex justify-end ${
            isDarkMode ? 'border-white/10 bg-white/[0.02]' : 'border-black/5 bg-white/25'
          }`}>
            <button
              onClick={onClose}
              className={`px-5 py-2 rounded-full text-xs font-medium transition-colors cursor-pointer ${
                isDarkMode ? 'bg-white text-black hover:bg-neutral-200' : 'bg-neutral-900 text-white hover:bg-black'
              }`}
            >
              Done
            </button>
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
}

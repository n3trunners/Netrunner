import React from 'react';
import { motion } from 'motion/react';
import { 
  ChevronLeft, ChevronRight, Puzzle, Box, Bot, Zap, 
  Moon, Sun, Globe, Bell, Sliders, Database, ShieldCheck, Info, Github 
} from 'lucide-react';
import { NetRunnerLogo, WatermarkStar } from './Icons';
import { SettingsModalType, GitHubUser } from '../types';

interface SettingsViewProps {
  onBack: () => void;
  onOpenModal: (type: SettingsModalType) => void;
  isDarkMode: boolean;
  onToggleTheme: () => void;
  currentUser: GitHubUser | null;
  onOpenGitHubAuth: () => void;
}

export function SettingsView({ 
  onBack, 
  onOpenModal, 
  isDarkMode, 
  onToggleTheme,
  currentUser,
  onOpenGitHubAuth
}: SettingsViewProps) {
  return (
    <div className="relative w-full h-full pb-16 overflow-y-auto select-none">
      {/* Background watermark star in dark mode */}
      {isDarkMode && (
        <div className="absolute -top-10 -right-14 pointer-events-none z-0">
          <WatermarkStar className="w-80 h-80 opacity-[0.06]" />
        </div>
      )}

      <div className="relative z-10 px-5 pt-4 space-y-6">
        {/* Top Header */}
        <div className="flex items-center gap-4 pt-1">
          <button 
            onClick={onBack}
            className="p-1 -ml-1 app-text-title transition-colors cursor-pointer"
            aria-label="Back"
          >
            <ChevronLeft className="w-6 h-6 stroke-[2]" />
          </button>
          
          <div className="h-6 w-[1.5px] app-divider" />

          <div>
            <h1 className="text-xl font-bold tracking-tight app-text-title">Settings</h1>
            <p className="text-xs app-text-sub">Customize your NetRunner experience</p>
          </div>
        </div>

        {/* Real Account Profile Card */}
        <motion.div 
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25 }}
          onClick={() => {
            if (currentUser) {
              onOpenModal('profile-detail');
            } else {
              onOpenGitHubAuth();
            }
          }}
          className="w-full p-4 rounded-2xl app-card transition-all cursor-pointer flex items-center justify-between group shadow-sm"
        >
          <div className="flex items-center gap-3.5">
            {currentUser ? (
              <img
                src={currentUser.avatar_url}
                alt={currentUser.login}
                className="w-12 h-12 rounded-full border border-neutral-300 object-cover shadow-sm group-hover:scale-105 transition-transform"
              />
            ) : (
              <div className={`w-12 h-12 rounded-full flex items-center justify-center group-hover:scale-105 transition-transform ${
                isDarkMode ? 'bg-white/[0.08] text-white' : 'bg-neutral-100 text-neutral-800 border border-neutral-200'
              }`}>
                <Github className="w-6 h-6" />
              </div>
            )}
            <div>
              <div className="font-bold text-base app-text-title">
                {currentUser ? (currentUser.name || currentUser.login) : 'Sign In with GitHub'}
              </div>
              <div className="text-xs app-text-sub">
                {currentUser ? `@${currentUser.login}` : 'Connect your developer account'}
              </div>
            </div>
          </div>
          <ChevronRight className="w-5 h-5 app-text-muted group-hover:translate-x-0.5 transition-all" />
        </motion.div>

        {/* Section 1: AI & Integrations */}
        <div className="space-y-2.5">
          <div className="flex items-center gap-3 text-xs font-semibold app-text-sub tracking-wide">
            <span>AI & Integrations</span>
            <div className="h-[1px] flex-1 app-divider" />
          </div>

          <div className="space-y-2">
            <SettingRow
              icon={<Puzzle className="w-5 h-5" />}
              title="Connectors"
              subtitle="Link external tools and services"
              onClick={() => onOpenModal('connectors')}
              isDarkMode={isDarkMode}
            />

            <SettingRow
              icon={<Box className="w-5 h-5" />}
              title="Models & API Token"
              subtitle="Bring your own token & choose AI models"
              onClick={() => onOpenModal('models')}
              isDarkMode={isDarkMode}
            />

            <SettingRow
              icon={<Bot className="w-5 h-5" />}
              title="Agents"
              subtitle="Create and manage AI agents"
              onClick={() => onOpenModal('agents')}
              isDarkMode={isDarkMode}
            />

            <SettingRow
              icon={<Zap className="w-5 h-5" />}
              title="Skills"
              subtitle="Add skills to extend capabilities"
              onClick={() => onOpenModal('skills')}
              isDarkMode={isDarkMode}
            />
          </div>
        </div>

        {/* Section 2: Preferences */}
        <div className="space-y-2.5">
          <div className="flex items-center gap-3 text-xs font-semibold app-text-sub tracking-wide">
            <span>Preferences</span>
            <div className="h-[1px] flex-1 app-divider" />
          </div>

          <div className="space-y-2">
            {/* Appearance with Working Dark/Light Toggle pill */}
            <div className="w-full p-3.5 rounded-2xl app-card flex items-center justify-between">
              <div className="flex items-center gap-3.5">
                <div className="relative w-5 h-5 app-text-title">
                  <Moon className={`w-5 h-5 absolute inset-0 transition-all duration-500 ease-[cubic-bezier(0.22,1,0.36,1)] ${isDarkMode ? 'opacity-100 rotate-0 scale-100' : 'opacity-0 -rotate-45 scale-75'}`} />
                  <Sun className={`w-5 h-5 absolute inset-0 transition-all duration-500 ease-[cubic-bezier(0.22,1,0.36,1)] ${!isDarkMode ? 'opacity-100 rotate-0 scale-100' : 'opacity-0 rotate-45 scale-75'}`} />
                </div>
                <div>
                  <div className="font-semibold text-[15px] app-text-title">Appearance</div>
                  <div className="text-xs app-text-sub">{isDarkMode ? 'Dark mode' : 'Light mode'}</div>
                </div>
              </div>

              {/* Animated Pill Switch */}
              <button
                type="button"
                onClick={onToggleTheme}
                className={`relative flex items-center border rounded-full p-1 cursor-pointer transition-colors duration-500 ease-[cubic-bezier(0.22,1,0.36,1)] shadow-inner ${
                  isDarkMode 
                    ? 'bg-[#15171d] border-white/12 hover:border-white/25' 
                    : 'bg-neutral-200/80 border-neutral-300 hover:border-neutral-400'
                }`}
                aria-label="Toggle dark/light mode"
              >
                <motion.div
                  className={`absolute top-1 bottom-1 w-7 h-7 rounded-full shadow-md transition-colors duration-500 ${
                    isDarkMode ? 'bg-white/20' : 'bg-white'
                  }`}
                  animate={{ x: isDarkMode ? 0 : 28 }}
                  transition={{ type: "spring", stiffness: 320, damping: 28, mass: 0.9 }}
                />

                <div 
                  className={`relative z-10 w-7 h-7 rounded-full flex items-center justify-center transition-colors duration-500 ${
                    isDarkMode ? 'text-white' : 'text-neutral-500'
                  }`}
                >
                  <Moon className="w-3.5 h-3.5 fill-current" />
                </div>
                <div 
                  className={`relative z-10 w-7 h-7 rounded-full flex items-center justify-center transition-colors duration-500 ${
                    !isDarkMode ? 'text-neutral-900' : 'text-neutral-400'
                  }`}
                >
                  <Sun className="w-3.5 h-3.5" />
                </div>
              </button>
            </div>

            {/* Language */}
            <SettingRow
              icon={<Globe className="w-5 h-5" />}
              title="Language"
              subtitle="English (US)"
              onClick={() => onOpenModal('language')}
              isDarkMode={isDarkMode}
            />

            {/* Notifications */}
            <SettingRow
              icon={<Bell className="w-5 h-5" />}
              title="Notifications"
              subtitle="Project updates and more"
              onClick={() => onOpenModal('notifications')}
              isDarkMode={isDarkMode}
            />

            {/* Editor Preferences */}
            <SettingRow
              icon={<Sliders className="w-5 h-5" />}
              title="Editor Preferences"
              subtitle="Customize your editor"
              onClick={() => onOpenModal('editor')}
              isDarkMode={isDarkMode}
            />
          </div>
        </div>

        {/* Section 3: Data & Storage */}
        <div className="space-y-2.5">
          <div className="flex items-center gap-3 text-xs font-semibold app-text-sub tracking-wide">
            <span>Data & Storage</span>
            <div className="h-[1px] flex-1 app-divider" />
          </div>

          <SettingRow
            icon={<Database className="w-5 h-5" />}
            title="Data & Storage"
            subtitle="Manage your data and storage"
            onClick={() => onOpenModal('data-storage')}
            isDarkMode={isDarkMode}
          />
        </div>

        {/* Section 4: Security */}
        <div className="space-y-2.5">
          <div className="flex items-center gap-3 text-xs font-semibold app-text-sub tracking-wide">
            <span>Security</span>
            <div className="h-[1px] flex-1 app-divider" />
          </div>

          <SettingRow
            icon={<ShieldCheck className="w-5 h-5" />}
            title="Security"
            subtitle="Permissions, privacy and keys"
            onClick={() => onOpenModal('security')}
            isDarkMode={isDarkMode}
          />
        </div>

        {/* Section 5: About */}
        <div className="space-y-2.5">
          <div className="flex items-center gap-3 text-xs font-semibold app-text-sub tracking-wide">
            <span>About</span>
            <div className="h-[1px] flex-1 app-divider" />
          </div>

          <SettingRow
            icon={<Info className="w-5 h-5" />}
            title="About NetRunner"
            subtitle="Version 1.0.0 • Built for developers"
            onClick={() => onOpenModal('about')}
            isDarkMode={isDarkMode}
          />
        </div>
      </div>
    </div>
  );
}

function SettingRow({ 
  icon, 
  title, 
  subtitle, 
  onClick,
  isDarkMode
}: { 
  icon: React.ReactNode; 
  title: string; 
  subtitle: string; 
  onClick: () => void;
  isDarkMode?: boolean;
}) {
  return (
    <div
      onClick={onClick}
      className="w-full p-3.5 rounded-2xl app-card transition-all cursor-pointer flex items-center justify-between group shadow-sm"
    >
      <div className="flex items-center gap-3.5">
        <div className="app-text-title group-hover:scale-105 transition-transform">
          {icon}
        </div>
        <div>
          <div className="font-semibold text-[15px] app-text-title">{title}</div>
          <div className="text-xs app-text-sub">{subtitle}</div>
        </div>
      </div>
      <ChevronRight className="w-5 h-5 app-text-muted group-hover:translate-x-0.5 transition-all" />
    </div>
  );
}

import React from 'react';
import { motion } from 'motion/react';
import { Home, Compass, User } from 'lucide-react';
import { ActiveTab } from '../types';

interface BottomDockProps {
  activeTab: ActiveTab;
  onChangeTab: (tab: ActiveTab) => void;
  isDarkMode?: boolean;
}

export function BottomDock({ activeTab, onChangeTab, isDarkMode = true }: BottomDockProps) {
  const tabs = [
    { id: 'home' as ActiveTab, icon: Home, label: 'Workspace' },
    { id: 'browse' as ActiveTab, icon: Compass, label: 'Browse' },
    { id: 'profile' as ActiveTab, icon: User, label: 'Profile' }
  ];

  return (
    <div className="absolute bottom-5 inset-x-0 px-6 flex justify-center z-30 pointer-events-none">
      <div className={`rounded-full px-8 py-3.5 flex items-center justify-between w-full max-w-[340px] pointer-events-auto transition-all app-dock-bar`}>
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;

          return (
            <button
              key={tab.id}
              onClick={() => onChangeTab(tab.id)}
              className="relative flex flex-col items-center justify-center p-2 transition-colors cursor-pointer group"
              aria-label={tab.label}
            >
              <Icon 
                className={`w-6 h-6 transition-all duration-200 ${
                  isActive 
                    ? (isDarkMode 
                        ? 'text-white scale-110 drop-shadow-[0_0_8px_rgba(255,255,255,0.4)]' 
                        : 'text-neutral-900 scale-110 font-bold') 
                    : (isDarkMode ? 'text-neutral-400 group-hover:text-neutral-200' : 'text-neutral-500 group-hover:text-neutral-800')
                }`} 
                strokeWidth={isActive ? 2.4 : 1.8}
              />

              {/* Active Tab Indicator Bar underneath the icon */}
              {isActive && (
                <motion.div
                  layoutId="dock-indicator"
                  transition={{ type: 'spring', stiffness: 500, damping: 35 }}
                  className={`absolute -bottom-1.5 w-6 h-[2.5px] rounded-full ${
                    isDarkMode 
                      ? 'bg-white shadow-[0_0_8px_rgba(255,255,255,0.8)]' 
                      : 'bg-neutral-900 shadow-[0_0_6px_rgba(0,0,0,0.3)]'
                  }`}
                />
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}

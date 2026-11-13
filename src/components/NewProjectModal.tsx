import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { X, Plus, ArrowRight } from 'lucide-react';
import { Project } from '../types';

interface NewProjectModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreateProject: (project: Omit<Project, 'id' | 'updatedAt' | 'status' | 'filesCount'>) => void;
  isDarkMode?: boolean;
}

export function NewProjectModal({ isOpen, onClose, onCreateProject, isDarkMode = true }: NewProjectModalProps) {
  const [projectName, setProjectName] = useState('');
  const [description, setDescription] = useState('');
  const [stack, setStack] = useState<'Hybrid' | 'Fullstack' | 'AI Agent' | 'API'>('Hybrid');

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!projectName.trim()) return;

    const isHybrid = stack === 'Hybrid';
    onCreateProject({
      name: projectName.trim(),
      description:
        description.trim() ||
        (isHybrid
          ? 'Kotlin + Jetpack Compose UI with Rust Core Daemon'
          : 'NetRunner workspace'),
      category: stack,
      architecture: isHybrid ? 'hybrid-kotlin-rust' : 'fullstack',
      tags: isHybrid ? ['Kotlin', 'Compose', 'Rust'] : ['NetRunner', 'Workspace'],
    });

    setProjectName('');
    setDescription('');
    onClose();
  };

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-md">
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 12 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 12 }}
          transition={{ duration: 0.24, ease: [0.22, 1, 0.36, 1] }}
          className={`relative w-full max-w-sm rounded-3xl border backdrop-blur-2xl overflow-hidden transition-colors duration-500 ${
            isDarkMode
              ? 'bg-[#0e1118]/75 border-white/15 text-white shadow-[0_24px_64px_rgba(0,0,0,0.7),inset_0_1px_0_rgba(255,255,255,0.14)]'
              : 'bg-white/75 border-white/80 text-neutral-900 shadow-[0_24px_60px_rgba(15,23,42,0.14),inset_0_1px_0_rgba(255,255,255,0.9)]'
          }`}
        >
          {/* Subtle Top Glass Specular Rim */}
          <div
            className={`pointer-events-none absolute inset-x-0 top-0 h-px ${
              isDarkMode
                ? 'bg-gradient-to-r from-transparent via-white/30 to-transparent'
                : 'bg-gradient-to-r from-transparent via-white to-transparent'
            }`}
          />

          {/* Header */}
          <div className={`flex items-center justify-between px-6 py-4 border-b ${
            isDarkMode ? 'border-white/10 bg-white/[0.03]' : 'border-neutral-200/70 bg-white/40'
          }`}>
            <div className="flex items-center gap-2.5">
              <span className={`p-1.5 rounded-xl border ${
                isDarkMode
                  ? 'bg-white/10 border-white/15 text-white'
                  : 'bg-neutral-900/5 border-neutral-300/60 text-neutral-900'
              }`}>
                <Plus className="w-4 h-4" />
              </span>
              <div>
                <h3 className="font-semibold tracking-tight text-sm">New Project</h3>
                <p className={`text-[11px] ${isDarkMode ? 'text-neutral-400' : 'text-neutral-500'}`}>
                  Initialize a clean workspace
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              className={`p-1.5 rounded-full transition-colors cursor-pointer ${
                isDarkMode ? 'text-neutral-400 hover:text-white hover:bg-white/10' : 'text-neutral-500 hover:text-black hover:bg-black/5'
              }`}
              aria-label="Close modal"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="p-6 space-y-4">
            <div>
              <label className={`block text-xs font-medium mb-1.5 ${isDarkMode ? 'text-neutral-300' : 'text-neutral-700'}`}>
                Architecture Stack
              </label>
              <div className="grid grid-cols-2 gap-1.5">
                {(
                  [
                    { id: 'Hybrid', label: 'Hybrid (Kotlin + Rust)' },
                    { id: 'Fullstack', label: 'Fullstack Web' },
                    { id: 'AI Agent', label: 'AI Agent' },
                    { id: 'API', label: 'API Service' },
                  ] as const
                ).map((option) => {
                  const selected = stack === option.id;
                  return (
                    <button
                      key={option.id}
                      type="button"
                      onClick={() => setStack(option.id)}
                      className={`px-3 py-2 rounded-xl border text-xs font-medium transition-colors text-left truncate cursor-pointer ${
                        selected
                          ? isDarkMode
                            ? 'bg-white text-black border-white font-semibold'
                            : 'bg-neutral-900 text-white border-neutral-900 font-semibold'
                          : isDarkMode
                            ? 'bg-white/[0.04] border-white/10 text-neutral-300 hover:bg-white/[0.08]'
                            : 'bg-white/70 border-neutral-300/80 text-neutral-700 hover:bg-white'
                      }`}
                    >
                      {option.label}
                    </button>
                  );
                })}
              </div>
            </div>

            <div>
              <label className={`block text-xs font-medium mb-1.5 ${isDarkMode ? 'text-neutral-300' : 'text-neutral-700'}`}>
                Project Name
              </label>
              <input
                type="text"
                required
                autoFocus
                value={projectName}
                onChange={(e) => setProjectName(e.target.value)}
                placeholder={stack === 'Hybrid' ? 'e.g. netrunner-android-daemon' : 'e.g. neural-runner'}
                className={`w-full px-4 py-2.5 rounded-2xl border backdrop-blur-md text-sm focus:outline-none transition-all ${
                  isDarkMode
                    ? 'bg-white/[0.06] border-white/12 text-white placeholder-neutral-500 focus:border-white/30 focus:bg-white/[0.09]'
                    : 'bg-white/80 border-neutral-300/80 text-neutral-900 placeholder-neutral-400 focus:border-neutral-600 focus:bg-white'
                }`}
              />
            </div>

            <div>
              <label className={`block text-xs font-medium mb-1.5 ${isDarkMode ? 'text-neutral-300' : 'text-neutral-700'}`}>
                Description <span className="opacity-50 font-normal">(optional)</span>
              </label>
              <input
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="What are you building?"
                className={`w-full px-4 py-2.5 rounded-2xl border backdrop-blur-md text-sm focus:outline-none transition-all ${
                  isDarkMode
                    ? 'bg-white/[0.06] border-white/12 text-white placeholder-neutral-500 focus:border-white/30 focus:bg-white/[0.09]'
                    : 'bg-white/80 border-neutral-300/80 text-neutral-900 placeholder-neutral-400 focus:border-neutral-600 focus:bg-white'
                }`}
              />
            </div>

            {/* Footer Actions */}
            <div className={`pt-3 flex items-center justify-end gap-2.5 border-t ${
              isDarkMode ? 'border-white/10' : 'border-neutral-200/70'
            }`}>
              <button
                type="button"
                onClick={onClose}
                className={`px-4 py-2 rounded-full text-xs font-medium transition-colors cursor-pointer ${
                  isDarkMode ? 'text-neutral-400 hover:text-white hover:bg-white/5' : 'text-neutral-600 hover:text-black hover:bg-black/5'
                }`}
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={!projectName.trim()}
                className={`px-5 py-2 rounded-full text-xs font-semibold disabled:opacity-40 transition-all flex items-center gap-1.5 shadow-lg cursor-pointer active:scale-[0.98] ${
                  isDarkMode
                    ? 'bg-white text-black hover:bg-neutral-200'
                    : 'bg-neutral-900 text-white hover:bg-black'
                }`}
              >
                <span>Create Project</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}

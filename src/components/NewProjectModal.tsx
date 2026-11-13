import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { X, ArrowRight } from 'lucide-react';
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

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!projectName.trim()) return;

    onCreateProject({
      name: projectName.trim(),
      description: description.trim() || 'NetRunner workspace',
      category: 'Fullstack',
      architecture: 'fullstack',
      tags: ['NetRunner'],
    });

    setProjectName('');
    setDescription('');
    onClose();
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.18 }}
          onClick={onClose}
          className={`absolute inset-0 z-50 flex items-center justify-center p-5 ${
            isDarkMode ? 'bg-black/25' : 'bg-slate-900/10'
          }`}
        >
          <motion.div
            initial={{ opacity: 0, scale: 0.94, y: 14 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.94, y: 14 }}
            transition={{ duration: 0.24, ease: [0.16, 1, 0.3, 1] }}
            onClick={(e) => e.stopPropagation()}
            className={`relative w-full max-w-sm rounded-[28px] border backdrop-blur-[28px] backdrop-saturate-150 overflow-hidden transition-colors duration-300 ${
              isDarkMode
                ? 'bg-[#0b0f18]/38 border-white/22 text-white shadow-[0_32px_90px_rgba(0,0,0,0.72),inset_0_1px_0_rgba(255,255,255,0.24)]'
                : 'bg-white/52 border-white/90 text-neutral-900 shadow-[0_28px_72px_rgba(15,23,42,0.16),inset_0_1px_0_rgba(255,255,255,0.95)]'
            }`}
          >
            {/* Luminous frosted specular top highlight */}
            <div
              aria-hidden="true"
              className={`pointer-events-none absolute inset-x-6 top-0 h-px ${
                isDarkMode
                  ? 'bg-gradient-to-r from-transparent via-white/50 to-transparent'
                  : 'bg-gradient-to-r from-transparent via-white to-transparent'
              }`}
            />

            {/* Header */}
            <div className="flex items-center justify-between px-6 pt-6 pb-2">
              <h3 className="text-base font-semibold tracking-tight">New Project</h3>
              <button
                type="button"
                onClick={onClose}
                className={`w-8 h-8 rounded-full flex items-center justify-center transition-colors cursor-pointer ${
                  isDarkMode
                    ? 'bg-white/[0.08] text-neutral-300 hover:bg-white/15 hover:text-white'
                    : 'bg-black/[0.06] text-neutral-600 hover:bg-black/12 hover:text-black'
                }`}
                aria-label="Close"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="px-6 pt-3 pb-6 space-y-4">
              <div>
                <label
                  htmlFor="new-project-name"
                  className={`block text-xs font-medium mb-1.5 ${
                    isDarkMode ? 'text-neutral-200' : 'text-neutral-700'
                  }`}
                >
                  Project Name
                </label>
                <input
                  id="new-project-name"
                  type="text"
                  required
                  autoFocus
                  value={projectName}
                  onChange={(e) => setProjectName(e.target.value)}
                  placeholder="Enter project name"
                  className={`w-full px-4 py-3 rounded-2xl border text-sm focus:outline-none transition-all ${
                    isDarkMode
                      ? 'bg-white/[0.08] border-white/16 text-white placeholder-neutral-400 focus:border-white/40 focus:bg-white/[0.12]'
                      : 'bg-white/65 border-white/80 text-neutral-900 placeholder-neutral-500 focus:border-neutral-600 focus:bg-white/90'
                  }`}
                />
              </div>

              <div>
                <label
                  htmlFor="new-project-description"
                  className={`block text-xs font-medium mb-1.5 ${
                    isDarkMode ? 'text-neutral-200' : 'text-neutral-700'
                  }`}
                >
                  Project Description
                </label>
                <textarea
                  id="new-project-description"
                  rows={3}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Describe your project"
                  className={`w-full px-4 py-3 rounded-2xl border text-sm resize-none focus:outline-none transition-all ${
                    isDarkMode
                      ? 'bg-white/[0.08] border-white/16 text-white placeholder-neutral-400 focus:border-white/40 focus:bg-white/[0.12]'
                      : 'bg-white/65 border-white/80 text-neutral-900 placeholder-neutral-500 focus:border-neutral-600 focus:bg-white/90'
                  }`}
                />
              </div>

              <div className="pt-2 flex items-center justify-end gap-2.5">
                <button
                  type="button"
                  onClick={onClose}
                  className={`px-4 py-2.5 rounded-full text-xs font-medium transition-colors cursor-pointer ${
                    isDarkMode
                      ? 'text-neutral-200 hover:text-white hover:bg-white/10'
                      : 'text-neutral-700 hover:text-black hover:bg-black/5'
                  }`}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={!projectName.trim()}
                  className={`px-5 py-2.5 rounded-full text-xs font-semibold disabled:opacity-40 transition-all flex items-center gap-1.5 shadow-lg cursor-pointer active:scale-[0.98] ${
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
        </motion.div>
      )}
    </AnimatePresence>
  );
}

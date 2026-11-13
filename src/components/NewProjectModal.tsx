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
          transition={{ duration: 0.2 }}
          className="fixed inset-0 z-50 flex items-center justify-center p-5 bg-black/55 backdrop-blur-xl"
        >
          <motion.div
            initial={{ opacity: 0, scale: 0.94, y: 14 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.94, y: 14 }}
            transition={{ duration: 0.26, ease: [0.16, 1, 0.3, 1] }}
            className={`relative w-full max-w-sm rounded-[28px] border backdrop-blur-2xl overflow-hidden transition-colors duration-300 ${
              isDarkMode
                ? 'bg-[#10141e]/65 border-white/18 text-white shadow-[0_32px_90px_rgba(0,0,0,0.75),inset_0_1px_0_rgba(255,255,255,0.22)]'
                : 'bg-white/68 border-white/90 text-neutral-900 shadow-[0_28px_72px_rgba(15,23,42,0.16),inset_0_1px_0_rgba(255,255,255,0.95)]'
            }`}
          >
            {/* Luminous frosted specular top highlight */}
            <div
              aria-hidden="true"
              className={`pointer-events-none absolute inset-x-6 top-0 h-px ${
                isDarkMode
                  ? 'bg-gradient-to-r from-transparent via-white/45 to-transparent'
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
                    ? 'bg-white/[0.07] text-neutral-300 hover:bg-white/15 hover:text-white'
                    : 'bg-black/[0.05] text-neutral-600 hover:bg-black/10 hover:text-black'
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
                    isDarkMode ? 'text-neutral-300' : 'text-neutral-700'
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
                  className={`w-full px-4 py-3 rounded-2xl border backdrop-blur-md text-sm focus:outline-none transition-all ${
                    isDarkMode
                      ? 'bg-white/[0.07] border-white/14 text-white placeholder-neutral-500 focus:border-white/35 focus:bg-white/[0.11]'
                      : 'bg-white/75 border-neutral-300/70 text-neutral-900 placeholder-neutral-400 focus:border-neutral-600 focus:bg-white/95'
                  }`}
                />
              </div>

              <div>
                <label
                  htmlFor="new-project-description"
                  className={`block text-xs font-medium mb-1.5 ${
                    isDarkMode ? 'text-neutral-300' : 'text-neutral-700'
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
                  className={`w-full px-4 py-3 rounded-2xl border backdrop-blur-md text-sm resize-none focus:outline-none transition-all ${
                    isDarkMode
                      ? 'bg-white/[0.07] border-white/14 text-white placeholder-neutral-500 focus:border-white/35 focus:bg-white/[0.11]'
                      : 'bg-white/75 border-neutral-300/70 text-neutral-900 placeholder-neutral-400 focus:border-neutral-600 focus:bg-white/95'
                  }`}
                />
              </div>

              <div className="pt-2 flex items-center justify-end gap-2.5">
                <button
                  type="button"
                  onClick={onClose}
                  className={`px-4 py-2.5 rounded-full text-xs font-medium transition-colors cursor-pointer ${
                    isDarkMode
                      ? 'text-neutral-300 hover:text-white hover:bg-white/10'
                      : 'text-neutral-600 hover:text-black hover:bg-black/5'
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

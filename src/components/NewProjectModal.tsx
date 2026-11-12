import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { X, Plus, Terminal, Code2, Cpu, Globe, ArrowRight } from 'lucide-react';
import { Project } from '../types';

interface NewProjectModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreateProject: (project: Omit<Project, 'id' | 'updatedAt' | 'status' | 'filesCount'>) => void;
  isDarkMode?: boolean;
}

const TEMPLATES = [
  { id: 'ai-agent', name: 'Autonomous Agent', category: 'AI Agent' as const, icon: Cpu, desc: 'Multi-turn reasoning loop with tool execution' },
  { id: 'fullstack', name: 'Fullstack App', category: 'Fullstack' as const, icon: Terminal, desc: 'Vite React + Express API with live database' },
  { id: 'frontend', name: 'Cyber Frontend', category: 'Frontend' as const, icon: Code2, desc: 'Tailwind V4 + Motion micro-interactions' },
  { id: 'api', name: 'Neural API Endpoint', category: 'API' as const, icon: Globe, desc: 'High-throughput low-latency inference router' }
];

export function NewProjectModal({ isOpen, onClose, onCreateProject, isDarkMode = true }: NewProjectModalProps) {
  const [projectName, setProjectName] = useState('');
  const [description, setDescription] = useState('');
  const [selectedTemplate, setSelectedTemplate] = useState(TEMPLATES[0]);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!projectName.trim()) return;

    onCreateProject({
      name: projectName.trim(),
      description: description.trim() || selectedTemplate.desc,
      category: selectedTemplate.category,
      tags: [selectedTemplate.category, 'NetRunner', 'v1.0']
    });

    setProjectName('');
    setDescription('');
    onClose();
  };

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-md">
        <motion.div 
          initial={{ opacity: 0, scale: 0.94, y: 15 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.94, y: 15 }}
          transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
          className={`relative w-full max-w-md rounded-3xl border shadow-2xl overflow-hidden transition-colors ${
            isDarkMode 
              ? 'bg-[#0c0e12] border-white/12 text-white' 
              : 'bg-white border-neutral-200 text-neutral-900 shadow-xl'
          }`}
        >
          {/* Header */}
          <div className={`flex items-center justify-between px-6 py-4 border-b ${
            isDarkMode ? 'border-white/10 bg-white/[0.02]' : 'border-neutral-100 bg-neutral-50'
          }`}>
            <div className="flex items-center gap-2">
              <span className={`p-1 rounded-lg ${isDarkMode ? 'bg-white/10 text-white' : 'bg-neutral-200 text-neutral-800'}`}>
                <Plus className="w-4 h-4" />
              </span>
              <h3 className="font-semibold tracking-wide text-base">New NetRunner Project</h3>
            </div>
            <button 
              onClick={onClose}
              className={`p-1.5 rounded-full transition-colors cursor-pointer ${
                isDarkMode ? 'text-neutral-400 hover:text-white hover:bg-white/10' : 'text-neutral-500 hover:text-black hover:bg-neutral-100'
              }`}
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="p-6 space-y-4">
            {/* Project Name */}
            <div>
              <label className={`block text-xs font-medium mb-1.5 ${isDarkMode ? 'text-neutral-300' : 'text-neutral-700'}`}>
                Project Name
              </label>
              <input
                type="text"
                required
                value={projectName}
                onChange={(e) => setProjectName(e.target.value)}
                placeholder="e.g. Matrix-Neural-Engine"
                className={`w-full px-4 py-2.5 rounded-xl border text-sm focus:outline-none transition-colors ${
                  isDarkMode 
                    ? 'bg-white/[0.04] border-white/10 text-white placeholder-neutral-500 focus:border-white/30' 
                    : 'bg-neutral-50 border-neutral-300 text-neutral-900 placeholder-neutral-400 focus:border-neutral-600'
                }`}
              />
            </div>

            {/* Description */}
            <div>
              <label className={`block text-xs font-medium mb-1.5 ${isDarkMode ? 'text-neutral-300' : 'text-neutral-700'}`}>
                Description (Optional)
              </label>
              <input
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Brief project goal..."
                className={`w-full px-4 py-2.5 rounded-xl border text-sm focus:outline-none transition-colors ${
                  isDarkMode 
                    ? 'bg-white/[0.04] border-white/10 text-white placeholder-neutral-500 focus:border-white/30' 
                    : 'bg-neutral-50 border-neutral-300 text-neutral-900 placeholder-neutral-400 focus:border-neutral-600'
                }`}
              />
            </div>

            {/* Template Selector */}
            <div>
              <label className={`block text-xs font-medium mb-2 ${isDarkMode ? 'text-neutral-300' : 'text-neutral-700'}`}>
                Select Architecture Template
              </label>
              <div className="grid grid-cols-2 gap-2">
                {TEMPLATES.map((tmpl) => {
                  const Icon = tmpl.icon;
                  const isSelected = selectedTemplate.id === tmpl.id;
                  return (
                    <div
                      key={tmpl.id}
                      onClick={() => setSelectedTemplate(tmpl)}
                      className={`p-3 rounded-2xl border text-left cursor-pointer transition-all ${
                        isSelected 
                          ? (isDarkMode ? 'bg-white/10 border-white/30' : 'bg-neutral-100 border-neutral-400 shadow-sm') 
                          : (isDarkMode ? 'bg-white/[0.03] border-white/8 hover:bg-white/[0.06]' : 'bg-neutral-50 border-neutral-200 hover:bg-neutral-100')
                      }`}
                    >
                      <Icon className={`w-4 h-4 mb-1.5 ${
                        isSelected 
                          ? (isDarkMode ? 'text-white' : 'text-neutral-900') 
                          : (isDarkMode ? 'text-neutral-400' : 'text-neutral-500')
                      }`} />
                      <div className={`font-medium text-xs ${isDarkMode ? 'text-white' : 'text-neutral-900'}`}>{tmpl.name}</div>
                      <div className="text-[10px] text-neutral-400 line-clamp-1 mt-0.5">{tmpl.desc}</div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Footer Buttons */}
            <div className={`pt-3 flex items-center justify-end gap-3 border-t ${isDarkMode ? 'border-white/10' : 'border-neutral-200'}`}>
              <button
                type="button"
                onClick={onClose}
                className={`px-4 py-2 rounded-full text-xs font-medium transition-colors cursor-pointer ${
                  isDarkMode ? 'text-neutral-400 hover:text-white' : 'text-neutral-600 hover:text-black'
                }`}
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={!projectName.trim()}
                className={`px-5 py-2.5 rounded-full text-xs font-semibold disabled:opacity-40 transition-all flex items-center gap-1.5 shadow-md cursor-pointer ${
                  isDarkMode ? 'bg-white text-black hover:bg-neutral-200' : 'bg-neutral-900 text-white hover:bg-black'
                }`}
              >
                <span>Initialize Project</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}

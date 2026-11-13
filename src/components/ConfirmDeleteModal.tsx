import React from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Trash2 } from 'lucide-react';

interface ConfirmDeleteModalProps {
  isOpen: boolean;
  title: string;
  description: string;
  confirmLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
  isDarkMode?: boolean;
}

export function ConfirmDeleteModal({
  isOpen,
  title,
  description,
  confirmLabel = 'Delete',
  onConfirm,
  onCancel,
  isDarkMode = true,
}: ConfirmDeleteModalProps) {
  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.16 }}
          onClick={onCancel}
          className={`absolute inset-0 z-50 flex items-center justify-center p-5 ${
            isDarkMode ? 'bg-black/28' : 'bg-slate-900/12'
          }`}
        >
          <motion.div
            initial={{ opacity: 0, scale: 0.94, y: 10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.94, y: 10 }}
            transition={{ duration: 0.2, ease: [0.16, 1, 0.3, 1] }}
            onClick={(e) => e.stopPropagation()}
            className={`relative w-full max-w-xs rounded-[26px] border backdrop-blur-2xl backdrop-saturate-150 p-5 overflow-hidden ${
              isDarkMode
                ? 'bg-[#0e121b]/48 border-white/20 text-white shadow-[0_24px_70px_rgba(0,0,0,0.7),inset_0_1px_0_rgba(255,255,255,0.22)]'
                : 'bg-white/58 border-white/85 text-neutral-900 shadow-[0_24px_60px_rgba(15,23,42,0.16),inset_0_1px_0_rgba(255,255,255,0.95)]'
            }`}
          >
            <div
              aria-hidden="true"
              className={`pointer-events-none absolute inset-x-6 top-0 h-px ${
                isDarkMode
                  ? 'bg-gradient-to-r from-transparent via-white/40 to-transparent'
                  : 'bg-gradient-to-r from-transparent via-white to-transparent'
              }`}
            />

            <div className="flex flex-col items-center text-center space-y-3">
              <div className="w-11 h-11 rounded-2xl bg-red-500/15 border border-red-500/30 flex items-center justify-center text-red-400">
                <Trash2 className="w-5 h-5" />
              </div>
              <div className="space-y-1">
                <h4 className="text-sm font-semibold">{title}</h4>
                <p className={`text-xs leading-relaxed ${
                  isDarkMode ? 'text-neutral-300' : 'text-neutral-600'
                }`}>
                  {description}
                </p>
              </div>
              <div className="w-full flex items-center gap-2.5 pt-2">
                <button
                  type="button"
                  onClick={onCancel}
                  className={`flex-1 py-2.5 rounded-full text-xs font-medium border transition-colors cursor-pointer ${
                    isDarkMode
                      ? 'bg-white/[0.06] border-white/15 text-neutral-200 hover:bg-white/12'
                      : 'bg-white/60 border-neutral-300/70 text-neutral-700 hover:bg-white'
                  }`}
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={onConfirm}
                  className="flex-1 py-2.5 rounded-full text-xs font-semibold bg-red-500 hover:bg-red-600 text-white shadow-md transition-colors cursor-pointer active:scale-[0.98]"
                >
                  {confirmLabel}
                </button>
              </div>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

import React from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { CheckCircle2, Info } from 'lucide-react';

interface ToastProps {
  message: string | null;
  onClear: () => void;
}

export function Toast({ message, onClear }: ToastProps) {
  return (
    <AnimatePresence>
      {message && (
        <motion.div
          initial={{ opacity: 0, y: -20, scale: 0.9 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: -20, scale: 0.9 }}
          transition={{ duration: 0.2 }}
          className="fixed top-5 inset-x-0 z-50 flex justify-center px-4 pointer-events-none"
        >
          <div className="px-4 py-2.5 rounded-full bg-[#181a20]/90 backdrop-blur-xl border border-white/20 text-white text-xs font-medium shadow-[0_10px_30px_rgba(0,0,0,0.8)] flex items-center gap-2 pointer-events-auto">
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
            <span>{message}</span>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

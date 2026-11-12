import React, { useState } from 'react';
import { motion } from 'motion/react';
import { Compass, ArrowLeft, Radio, RefreshCw, BellRing, Check } from 'lucide-react';
import { WatermarkStar } from './Icons';

interface BrowseViewProps {
  onBackToHome: () => void;
  onShowToast: (msg: string) => void;
  isDarkMode?: boolean;
}

export function BrowseView({ onBackToHome, onShowToast, isDarkMode = true }: BrowseViewProps) {
  const [isChecking, setIsChecking] = useState(false);
  const [subscribed, setSubscribed] = useState(false);

  const handleCheckStatus = () => {
    setIsChecking(true);
    setTimeout(() => {
      setIsChecking(false);
      onShowToast('Server ping: 22ms. Registry deployment in progress (ETA 12m)');
    }, 900);
  };

  const handleNotifyMe = () => {
    setSubscribed(true);
    onShowToast('Subscribed! We will notify you when the Browse Hub launches.');
  };

  return (
    <div className="relative w-full h-full min-h-[640px] flex flex-col justify-between p-6 overflow-hidden select-none">
      {/* Background Watermark Star in dark mode */}
      {isDarkMode && (
        <div className="absolute -top-12 -right-16 pointer-events-none">
          <WatermarkStar className="w-80 h-80 opacity-[0.05]" />
        </div>
      )}

      {/* Top Header */}
      <motion.div 
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        className="flex items-center justify-between pt-2 z-10"
      >
        <div className="flex items-center gap-3">
          <button 
            onClick={onBackToHome}
            className="p-1.5 -ml-1.5 rounded-full app-text-sub hover:app-text-title transition-colors cursor-pointer"
            title="Return to Home"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div className="h-4 w-[1px] app-divider" />
          <div className="flex items-center gap-2">
            <Compass className="w-4 h-4 app-text-title animate-spin [animation-duration:12s]" />
            <span className="font-semibold tracking-widest text-xs uppercase app-text-title">
              Browse Hub
            </span>
          </div>
        </div>

        <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-amber-500/10 border border-amber-500/20 text-[11px] text-amber-500 font-mono">
          <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-ping" />
          <span>Offline</span>
        </div>
      </motion.div>

      {/* Center Hero Card with Radar Pulse & Required Message */}
      <motion.div 
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.4, delay: 0.1 }}
        className="my-auto py-8 flex flex-col items-center text-center z-10 max-w-sm mx-auto"
      >
        {/* Pulsing Radar Ring & Compass Graphic */}
        <div className="relative w-32 h-32 flex items-center justify-center mb-7">
          <motion.div 
            animate={{ scale: [1, 1.35, 1], opacity: [0.15, 0.4, 0.15] }}
            transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
            className={`absolute inset-0 rounded-full border ${isDarkMode ? 'border-white/20' : 'border-neutral-300'}`}
          />

          <motion.div 
            animate={{ rotate: 360 }}
            transition={{ duration: 20, repeat: Infinity, ease: 'linear' }}
            className={`absolute inset-3 rounded-full border border-dashed ${isDarkMode ? 'border-white/25' : 'border-neutral-400'}`}
          />

          <div className={`relative w-16 h-16 rounded-2xl border flex items-center justify-center shadow-lg ${
            isDarkMode 
              ? 'bg-gradient-to-b from-white/10 to-white/[0.02] border-white/20 text-white' 
              : 'bg-white border-neutral-200 text-neutral-900 shadow-md'
          }`}>
            <Compass className="w-8 h-8" />
          </div>
        </div>

        {/* Primary Message explicitly requested */}
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.35, delay: 0.2 }}
        >
          <div className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full border text-xs mb-3 ${
            isDarkMode ? 'bg-white/[0.04] border-white/10 text-neutral-400' : 'bg-neutral-100 border-neutral-200 text-neutral-600'
          }`}>
            <Radio className="w-3 h-3" />
            <span>Community & Plugin Registry</span>
          </div>

          <h2 className="text-2xl sm:text-3xl font-bold tracking-tight app-text-title mb-2.5">
            Not available at the moment.
          </h2>

          <p className="text-sm app-text-sub leading-relaxed max-w-xs mb-6">
            The NetRunner browse catalog, shared blueprints, and extension store are currently undergoing server maintenance.
          </p>
        </motion.div>

        {/* Actions */}
        <div className="w-full flex flex-col gap-2.5">
          <button
            onClick={onBackToHome}
            className={`w-full py-3 px-6 rounded-full text-xs font-semibold transition-all shadow-md flex items-center justify-center gap-2 cursor-pointer ${
              isDarkMode ? 'bg-white text-black hover:bg-neutral-200' : 'bg-neutral-900 text-white hover:bg-black'
            }`}
          >
            <span>Back to Workspace</span>
          </button>

          <div className="grid grid-cols-2 gap-2 w-full">
            <button
              onClick={handleCheckStatus}
              disabled={isChecking}
              className={`py-2.5 px-4 rounded-full text-xs font-medium border transition-colors flex items-center justify-center gap-1.5 cursor-pointer ${
                isDarkMode 
                  ? 'bg-white/[0.05] hover:bg-white/[0.1] text-neutral-300 border-white/10' 
                  : 'bg-neutral-100 hover:bg-neutral-200 text-neutral-700 border-neutral-300'
              }`}
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isChecking ? 'animate-spin' : ''}`} />
              <span>{isChecking ? 'Checking...' : 'Check Status'}</span>
            </button>

            <button
              onClick={handleNotifyMe}
              className={`py-2.5 px-4 rounded-full text-xs font-medium border transition-colors flex items-center justify-center gap-1.5 cursor-pointer ${
                subscribed 
                  ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-500' 
                  : (isDarkMode ? 'bg-white/[0.05] hover:bg-white/[0.1] text-neutral-300 border-white/10' : 'bg-neutral-100 hover:bg-neutral-200 text-neutral-700 border-neutral-300')
              }`}
            >
              {subscribed ? <Check className="w-3.5 h-3.5" /> : <BellRing className="w-3.5 h-3.5" />}
              <span>{subscribed ? 'Subscribed' : 'Notify Me'}</span>
            </button>
          </div>
        </div>
      </motion.div>

      {/* Bottom system status indicator */}
      <div className="pt-4 border-t app-divider flex items-center justify-between text-[11px] app-text-muted font-mono z-10">
        <span>ERR_CATALOG_UNAVAILABLE</span>
        <span>SYS_VER 1.0.0</span>
      </div>
    </div>
  );
}

// The signature NetRunner 4-pointed logo using the real asset provided by user
export function NetRunnerLogo({ 
  className = "w-6 h-6", 
  alt = "NetRunner Logo",
  isDark = true 
}: { 
  className?: string; 
  alt?: string; 
  isDark?: boolean; 
  strokeWidth?: number 
}) {
  return (
    <div className={`relative inline-flex items-center justify-center shrink-0 ${className}`}>
      <img 
        src="/assets/aistudio/Untitled9_20261108121101_fc9e593c.png" 
        alt={alt}
        className={`w-full h-full object-contain select-none pointer-events-none [filter:invert(1)] [mix-blend-mode:screen] transition-opacity duration-500 ease-[cubic-bezier(0.22,1,0.36,1)] ${
          isDark ? 'opacity-100' : 'opacity-0'
        }`}
      />
      <img 
        src="/assets/aistudio/Untitled9_20261108121101_fc9e593c.png" 
        alt=""
        aria-hidden="true"
        className={`absolute inset-0 w-full h-full object-contain select-none pointer-events-none [mix-blend-mode:multiply] transition-opacity duration-500 ease-[cubic-bezier(0.22,1,0.36,1)] ${
          isDark ? 'opacity-0' : 'opacity-100'
        }`}
      />
    </div>
  );
}

// Background watermark is now part of the user's authentic high-resolution wallpaper asset
export function WatermarkStar({ className = "" }: { className?: string }) {
  return null;
}

// 3D Dark Folder illustration with dual sparkles matching Image 1
export function GlossyFolderGraphic({ className = "w-28 h-28" }: { className?: string }) {
  return (
    <div className={`relative inline-flex items-center justify-center ${className}`}>
      {/* Floating Sparkle 1 */}
      <div className="absolute -top-1 right-2 text-white/70 animate-pulse">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 0L14 8L22 10L14 12L12 20L10 12L2 10L10 8Z" />
        </svg>
      </div>
      
      {/* Floating Sparkle 2 */}
      <div className="absolute top-5 -right-3 text-white/50 animate-pulse delay-300">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 0L14 8L22 10L14 12L12 20L10 12L2 10L10 8Z" />
        </svg>
      </div>

      {/* Glossy Dark 3D Folder */}
      <svg 
        viewBox="0 0 120 100" 
        fill="none" 
        xmlns="http://www.w3.org/2000/svg"
        className="w-full h-full drop-shadow-[0_12px_24px_rgba(0,0,0,0.8)]"
      >
        <defs>
          <linearGradient id="folderBack" x1="10" y1="20" x2="110" y2="85" gradientUnits="userSpaceOnUse">
            <stop stopColor="#2c3038" />
            <stop offset="0.6" stopColor="#1a1c22" />
            <stop offset="1" stopColor="#101115" />
          </linearGradient>

          <linearGradient id="folderFront" x1="15" y1="36" x2="105" y2="92" gradientUnits="userSpaceOnUse">
            <stop stopColor="#3a3f4b" />
            <stop offset="0.3" stopColor="#242730" />
            <stop offset="1" stopColor="#12141a" />
          </linearGradient>

          <linearGradient id="rimHighlight" x1="15" y1="36" x2="105" y2="36" gradientUnits="userSpaceOnUse">
            <stop stopColor="rgba(255,255,255,0.4)" />
            <stop offset="0.7" stopColor="rgba(255,255,255,0.15)" />
            <stop offset="1" stopColor="rgba(255,255,255,0.02)" />
          </linearGradient>
        </defs>

        {/* Folder Back Tab */}
        <path 
          d="M18 24C18 20.6863 20.6863 18 24 18H44C46.8 18 49.3 19.5 50.7 21.8L54.3 27.5C55.2 29 56.8 30 58.6 30H96C99.3137 30 102 32.6863 102 36V78C102 81.3137 99.3137 84 96 84H24C20.6863 84 18 81.3137 18 78V24Z" 
          fill="url(#folderBack)" 
          stroke="rgba(255,255,255,0.08)"
          strokeWidth="1.2"
        />

        {/* Paper / Inner Glow peek */}
        <path 
          d="M26 30H94C96 30 98 31.5 98 33.5V45H22V33.5C22 31.5 24 30 26 30Z" 
          fill="rgba(255,255,255,0.04)" 
        />

        {/* Folder Front Flap with perspective skew & glossy curve */}
        <path 
          d="M24 37C24 34.2 26.2 32 29 32H91C93.8 32 96 34.2 96 37L91 82C90.6 85.2 87.8 87.5 84.6 87.5H23.4C20.2 87.5 17.4 85.2 17 82L24 37Z" 
          fill="url(#folderFront)" 
          stroke="rgba(255,255,255,0.1)"
          strokeWidth="1.2"
        />

        {/* Top Rim Specular Highlight */}
        <path 
          d="M27 34H93" 
          stroke="url(#rimHighlight)" 
          strokeWidth="1.5" 
          strokeLinecap="round"
        />
      </svg>
    </div>
  );
}

export function DiscordLogo({ className = "w-5 h-5" }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="currentColor"
      className={className}
      aria-hidden="true"
    >
      <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028c.462-.63.874-1.295 1.226-1.994.021-.041.001-.09-.041-.106a13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.061 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.028zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.956-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.946 2.418-2.157 2.418z" />
    </svg>
  );
}

export function XLogo({ className = "w-4 h-4" }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="currentColor"
      className={className}
      aria-hidden="true"
    >
      <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
    </svg>
  );
}



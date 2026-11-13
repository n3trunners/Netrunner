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

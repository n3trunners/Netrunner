import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { X, Github, Loader2, AlertCircle } from 'lucide-react';
import { GitHubUser, GitHubStats, GitHubRepo } from '../types';

interface GitHubAuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  onLoginSuccess: (user: GitHubUser, stats: GitHubStats, repos: GitHubRepo[], token?: string) => void;
  onShowToast: (msg: string) => void;
  isDarkMode?: boolean;
}

export function GitHubAuthModal({
  isOpen,
  onClose,
  onLoginSuccess,
  onShowToast,
  isDarkMode = true
}: GitHubAuthModalProps) {
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const popupWatcherRef = useRef<number | null>(null);

  const clearPopupWatcher = () => {
    if (popupWatcherRef.current !== null) {
      window.clearInterval(popupWatcherRef.current);
      popupWatcherRef.current = null;
    }
  };

  const finalizeGitHubProfile = async (
    rawProfile: Record<string, any>,
    accessToken?: string
  ) => {
    clearPopupWatcher();
    const login = rawProfile.login || 'developer';
    const userProfile: GitHubUser = {
      login,
      name: rawProfile.name || login,
      avatar_url: rawProfile.avatar_url || `https://github.com/${login}.png`,
      html_url: rawProfile.html_url || `https://github.com/${login}`,
      bio: rawProfile.bio || 'NetRunner Developer',
      company: rawProfile.company || undefined,
      location: rawProfile.location || undefined,
      blog: rawProfile.blog || undefined,
      twitter_username: rawProfile.twitter_username || undefined,
      public_repos: typeof rawProfile.public_repos === 'number' ? rawProfile.public_repos : 0,
      public_gists: typeof rawProfile.public_gists === 'number' ? rawProfile.public_gists : 0,
      followers: typeof rawProfile.followers === 'number' ? rawProfile.followers : 0,
      following: typeof rawProfile.following === 'number' ? rawProfile.following : 0,
      created_at: rawProfile.created_at || new Date().toISOString()
    };

    const headers: Record<string, string> = {
      Accept: 'application/vnd.github.v3+json',
    };
    if (accessToken) {
      headers.Authorization = `Bearer ${accessToken}`;
    }

    let repos: any[] = [];
    try {
      const reposRes = await fetch(
        `https://api.github.com/users/${encodeURIComponent(login)}/repos?sort=updated&per_page=30`,
        { headers }
      );
      if (reposRes.ok) {
        repos = await reposRes.json();
      }
    } catch (err) {
      console.warn('Could not fetch user repos:', err);
    }

    const totalStars = Array.isArray(repos)
      ? repos.reduce((acc, r) => acc + (r.stargazers_count || 0), 0)
      : 0;
    const totalForks = Array.isArray(repos)
      ? repos.reduce((acc, r) => acc + (r.forks_count || 0), 0)
      : 0;

    const languagesCount: Record<string, number> = {};
    if (Array.isArray(repos)) {
      repos.forEach((r) => {
        if (r.language) {
          languagesCount[r.language] = (languagesCount[r.language] || 0) + 1;
        }
      });
    }

    const topLanguages = Object.entries(languagesCount)
      .sort((a, b) => b[1] - a[1])
      .map(([language, count]) => ({ language, count }));

    const stats: GitHubStats = {
      totalStars,
      totalForks,
      publicRepos: userProfile.public_repos || repos.length,
      followers: userProfile.followers,
      topLanguages
    };

    const formattedRepos: GitHubRepo[] = Array.isArray(repos)
      ? repos.map((r) => ({
          id: r.id,
          name: r.name,
          description: r.description,
          html_url: r.html_url,
          stars: r.stargazers_count,
          forks: r.forks_count,
          language: r.language,
          updated_at: r.updated_at
        }))
      : [];

    onLoginSuccess(userProfile, stats, formattedRepos, accessToken);
    onShowToast(`Signed in as @${login}! Live stats synchronized.`);
    setIsLoading(false);
    onClose();
  };

  useEffect(() => {
    if (!isOpen) {
      clearPopupWatcher();
      setIsLoading(false);
      return;
    }
    const handleMessage = (event: MessageEvent) => {
      const origin = event.origin;
      if (!origin.endsWith('.run.app') && !origin.includes('localhost')) {
        return;
      }
      if (event.data?.type === 'OAUTH_AUTH_SUCCESS' && event.data?.user) {
        finalizeGitHubProfile(event.data.user, event.data.token);
      }
    };
    window.addEventListener('message', handleMessage);
    return () => {
      window.removeEventListener('message', handleMessage);
      clearPopupWatcher();
    };
  }, [isOpen]);

  const handleContinueWithGitHub = async () => {
    setIsLoading(true);
    setErrorMessage(null);

    try {
      // Direct GitHub OAuth popup flow via server — avoids storage-partitioned iframe sessionStorage/signInWithRedirect errors
      const res = await fetch('/api/auth/github/url?platform=web');
      if (!res.ok) {
        throw new Error(`Auth server returned HTTP ${res.status}`);
      }
      const data = await res.json();
      if (!data.configured || !data.url) {
        throw new Error(data.message || 'GitHub OAuth Client ID is not configured yet.');
      }

      const authWindow = window.open(data.url, 'github_oauth_popup', 'width=600,height=720');
      if (!authWindow) {
        throw new Error('Sign-in popup was blocked by your browser. Please allow popups.');
      }

      clearPopupWatcher();
      popupWatcherRef.current = window.setInterval(() => {
        if (authWindow.closed) {
          clearPopupWatcher();
          setIsLoading(false);
        }
      }, 400);
    } catch (err: any) {
      console.error('GitHub authentication error:', err);
      setErrorMessage(err?.message || 'Authentication failed. Please try again.');
      setIsLoading(false);
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.18 }}
          className={`absolute inset-0 z-50 flex items-center justify-center p-4 ${
            isDarkMode ? 'bg-black/15' : 'bg-slate-900/5'
          }`}
        >
          <motion.div
            initial={{ opacity: 0, scale: 0.94, y: 12 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.94, y: 12 }}
            transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
            className={`relative w-full max-w-sm rounded-[28px] p-6 border overflow-hidden transition-colors ${
              isDarkMode
                ? 'bg-[#0b0f18]/32 border-white/25 text-white shadow-[0_28px_80px_rgba(0,0,0,0.68),inset_0_1px_0_rgba(255,255,255,0.28)]'
                : 'bg-white/36 border-white/80 text-neutral-900 shadow-[0_24px_64px_rgba(15,23,42,0.14),inset_0_1px_0_rgba(255,255,255,0.95)]'
            }`}
          >
            {/* Clear glass diagonal sheen & top specular highlight */}
            <div
              aria-hidden="true"
              className={`pointer-events-none absolute inset-0 ${
                isDarkMode
                  ? 'bg-gradient-to-br from-white/[0.11] via-transparent to-white/[0.03]'
                  : 'bg-gradient-to-br from-white/50 via-transparent to-white/15'
              }`}
            />
            <div
              aria-hidden="true"
              className={`pointer-events-none absolute inset-x-6 top-0 h-px ${
                isDarkMode
                  ? 'bg-gradient-to-r from-transparent via-white/55 to-transparent'
                  : 'bg-gradient-to-r from-transparent via-white to-transparent'
              }`}
            />

            <button
              onClick={onClose}
              className={`absolute top-4 right-4 p-1.5 rounded-full transition-colors cursor-pointer ${
                isDarkMode
                  ? 'text-neutral-300 hover:text-white hover:bg-white/12'
                  : 'text-neutral-500 hover:text-black hover:bg-black/8'
              }`}
            >
              <X className="w-5 h-5" />
            </button>

            <div className="text-center pt-2 pb-1 space-y-4">
              <div className={`w-14 h-14 rounded-2xl mx-auto flex items-center justify-center shadow-lg border ${
                isDarkMode
                  ? 'bg-white/8 border-white/18 text-white'
                  : 'bg-neutral-900 border-neutral-800 text-white'
              }`}>
                <Github className="w-7 h-7" />
              </div>

              <div>
                <h3 className="font-bold text-lg tracking-tight">
                  Sign in to NetRunner
                </h3>
                <p className={`text-xs mt-1 leading-relaxed ${isDarkMode ? 'text-neutral-300' : 'text-neutral-600'}`}>
                  Sign in with your GitHub account to access your workspace and sync repositories.
                </p>
              </div>

              {errorMessage && (
                <div className="p-3 rounded-2xl bg-amber-500/12 border border-amber-500/25 text-amber-300 text-xs text-left flex items-start gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                  <span className="leading-relaxed">{errorMessage}</span>
                </div>
              )}

              <div className="pt-2">
                <button
                  onClick={handleContinueWithGitHub}
                  disabled={isLoading}
                  className={`w-full py-3.5 px-6 rounded-full font-semibold text-xs active:scale-[0.98] transition-all flex items-center justify-center gap-2.5 shadow-lg disabled:opacity-50 cursor-pointer ${
                    isDarkMode
                      ? 'bg-white text-black hover:bg-neutral-200 shadow-[0_4px_20px_rgba(255,255,255,0.2)]'
                      : 'bg-neutral-900 text-white hover:bg-black shadow-[0_4px_20px_rgba(0,0,0,0.2)]'
                  }`}
                >
                  {isLoading ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      <span>Connecting with GitHub...</span>
                    </>
                  ) : (
                    <>
                      <Github className="w-4 h-4" />
                      <span>Continue with GitHub</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { X, Github, Loader2, AlertCircle } from 'lucide-react';
import {
  auth,
  githubProvider,
  signInWithPopup,
  getAdditionalUserInfo,
  GithubAuthProvider,
  db
} from '../firebase';
import { doc, setDoc } from 'firebase/firestore';
import firebaseConfig from '../../firebase-applet-config.json';
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

  const finalizeGitHubProfile = async (
    rawProfile: Record<string, any>,
    accessToken?: string,
    firebaseUid?: string
  ) => {
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

    if (firebaseUid) {
      try {
        await setDoc(doc(db, 'users', firebaseUid), {
          ...userProfile,
          uid: firebaseUid,
          updatedAt: new Date().toISOString()
        }, { merge: true });
      } catch (e) {
        console.warn('Firestore user doc sync notice:', e);
      }
    }

    onLoginSuccess(userProfile, stats, formattedRepos, accessToken);
    onShowToast(`Signed in as @${login}! Live stats synchronized.`);
    setIsLoading(false);
    onClose();
  };

  useEffect(() => {
    if (!isOpen) return;
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
    return () => window.removeEventListener('message', handleMessage);
  }, [isOpen]);

  if (!isOpen) return null;

  const handleContinueWithGitHub = async () => {
    setIsLoading(true);
    setErrorMessage(null);

    try {
      const hasFirebaseAuth = Boolean(firebaseConfig?.apiKey);
      if (hasFirebaseAuth) {
        const result = await signInWithPopup(auth, githubProvider);
        const fbUser = result.user;
        const additionalInfo = getAdditionalUserInfo(result);
        const credential = GithubAuthProvider.credentialFromResult(result);
        const accessToken = credential?.accessToken;
        const rawProfile: any = additionalInfo?.profile || {};
        const login =
          additionalInfo?.username ||
          rawProfile.login ||
          (fbUser.displayName ? fbUser.displayName.replace(/\s+/g, '') : 'developer');

        await finalizeGitHubProfile(
          {
            ...rawProfile,
            login,
            name: rawProfile.name || fbUser.displayName || login,
            avatar_url: rawProfile.avatar_url || fbUser.photoURL || `https://github.com/${login}.png`,
          },
          accessToken,
          fbUser.uid
        );
        return;
      }

      // Direct GitHub OAuth popup flow via server
      const res = await fetch('/api/auth/github/url?platform=web');
      if (!res.ok) {
        throw new Error('Could not initialize GitHub OAuth.');
      }
      const data = await res.json();
      if (!data.configured || !data.url) {
        throw new Error(data.message || 'GitHub OAuth Client ID is not configured yet.');
      }

      const authWindow = window.open(data.url, 'github_oauth_popup', 'width=600,height=720');
      if (!authWindow) {
        throw new Error('Sign-in popup was blocked by your browser. Please allow popups.');
      }
    } catch (err: any) {
      console.error('GitHub authentication error:', err);
      if (err.code === 'auth/operation-not-allowed') {
        setErrorMessage(
          'GitHub provider is not yet enabled in Firebase Console. Enable it under Authentication > Sign-in method > GitHub.'
        );
      } else if (err.code === 'auth/popup-blocked') {
        setErrorMessage('Sign-in popup was blocked by browser. Please allow popups for this page.');
      } else {
        setErrorMessage(err.message || 'Authentication failed. Please try again.');
      }
      setIsLoading(false);
    }
  };

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-md">
        <motion.div
          initial={{ opacity: 0, scale: 0.94, y: 15 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.94, y: 15 }}
          transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
          className={`relative w-full max-w-sm rounded-3xl p-6 border shadow-2xl overflow-hidden transition-colors ${
            isDarkMode 
              ? 'bg-[#0d0f14] border-white/12 text-white' 
              : 'bg-white border-neutral-200 text-neutral-900 shadow-xl'
          }`}
        >
          {/* Close button */}
          <button
            onClick={onClose}
            className={`absolute top-4 right-4 p-1.5 rounded-full transition-colors cursor-pointer ${
              isDarkMode 
                ? 'text-neutral-400 hover:text-white hover:bg-white/10' 
                : 'text-neutral-500 hover:text-black hover:bg-neutral-100'
            }`}
          >
            <X className="w-5 h-5" />
          </button>

          {/* Clean Card Content */}
          <div className="text-center pt-2 pb-1 space-y-4">
            <div className={`w-14 h-14 rounded-2xl mx-auto flex items-center justify-center shadow-lg border ${
              isDarkMode 
                ? 'bg-white/5 border-white/15 text-white' 
                : 'bg-neutral-900 border-neutral-800 text-white'
            }`}>
              <Github className="w-7 h-7" />
            </div>

            <div>
              <h3 className="font-bold text-lg tracking-tight">
                Sign in to NetRunner
              </h3>
              <p className={`text-xs mt-1 leading-relaxed ${isDarkMode ? 'text-neutral-400' : 'text-neutral-500'}`}>
                Sign in with your GitHub account to access your workspace and sync repositories.
              </p>
            </div>

            {errorMessage && (
              <div className="p-3 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-400 text-xs text-left flex items-start gap-2">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                <span className="leading-relaxed">{errorMessage}</span>
              </div>
            )}

            {/* Single clean Continue with GitHub button */}
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
      </div>
    </AnimatePresence>
  );
}

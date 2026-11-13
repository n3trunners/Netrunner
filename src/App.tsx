/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence, MotionConfig } from 'motion/react';
import { Smartphone, Monitor } from 'lucide-react';

import { Project, ActiveTab, SettingsModalType, GitHubUser, GitHubStats, GitHubRepo } from './types';
import { DashboardView } from './components/DashboardView';
import { SettingsView } from './components/SettingsView';
import { BrowseView } from './components/BrowseView';
import { ProfileView } from './components/ProfileView';
import { BottomDock } from './components/BottomDock';
import { SettingsModals } from './components/SettingsModals';
import { SettingsSubPageView, IntegrationSubPageType } from './components/SettingsSubPageView';
import { NewProjectModal } from './components/NewProjectModal';
import { GitHubAuthModal } from './components/GitHubAuthModal';
import { AIChatModal } from './components/AIChatModal';
import { Toast } from './components/Toast';

import {
  auth,
  db,
  onAuthStateChanged,
  firebaseSignOut
} from './firebase';
import {
  collection,
  doc,
  setDoc,
  getDoc,
  getDocs,
  deleteDoc
} from 'firebase/firestore';

export default function App() {
  // Navigation & Screen State
  const [activeTab, setActiveTab] = useState<ActiveTab>('home');
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [activeSettingsSubPage, setActiveSettingsSubPage] = useState<IntegrationSubPageType | null>(null);
  const [activeSettingsModal, setActiveSettingsModal] = useState<SettingsModalType>(null);
  const [isNewProjectModalOpen, setIsNewProjectModalOpen] = useState(false);
  const [isGitHubAuthOpen, setIsGitHubAuthOpen] = useState(false);
  const [activeProjectWorkspace, setActiveProjectWorkspace] = useState<Project | null>(null);
  const [isRefreshingStats, setIsRefreshingStats] = useState(false);

  // Appearance Theme State (Persistent in localStorage)
  const [isDarkMode, setIsDarkMode] = useState<boolean>(() => {
    const saved = localStorage.getItem('netrunner_theme');
    return saved !== null ? saved === 'dark' : true;
  });

  const [isDeviceFrame, setIsDeviceFrame] = useState(true);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Real User State
  const [currentUser, setCurrentUser] = useState<GitHubUser | null>(() => {
    const saved = localStorage.getItem('netrunner_github_user');
    return saved ? JSON.parse(saved) : null;
  });

  const [userStats, setUserStats] = useState<GitHubStats | null>(() => {
    const saved = localStorage.getItem('netrunner_github_stats');
    return saved ? JSON.parse(saved) : null;
  });

  const [userRepos, setUserRepos] = useState<GitHubRepo[]>(() => {
    const saved = localStorage.getItem('netrunner_github_repos');
    return saved ? JSON.parse(saved) : [];
  });

  const [githubToken, setGithubToken] = useState<string | null>(() => {
    return localStorage.getItem('netrunner_github_token');
  });

  // Projects State
  const [projects, setProjects] = useState<Project[]>(() => {
    const saved = localStorage.getItem('netrunner_projects');
    return saved ? JSON.parse(saved) : [];
  });

  // Sync theme changes to localStorage
  useEffect(() => {
    localStorage.setItem('netrunner_theme', isDarkMode ? 'dark' : 'light');
  }, [isDarkMode]);

  // Sync projects to localStorage as local cache
  useEffect(() => {
    localStorage.setItem('netrunner_projects', JSON.stringify(projects));
  }, [projects]);

  // Helper to fetch live real-time stats & repos directly from GitHub API
  const fetchLiveGitHubStats = async (login: string, existingUser?: GitHubUser | null) => {
    try {
      const headers: Record<string, string> = { Accept: 'application/vnd.github.v3+json' };
      if (githubToken) {
        headers.Authorization = `Bearer ${githubToken}`;
      }

      const userRes = await fetch(`https://api.github.com/users/${encodeURIComponent(login)}`, { headers });
      let freshUser = existingUser;
      if (userRes.ok) {
        const ghProfile = await userRes.json();
        freshUser = {
          login: ghProfile.login || login,
          name: ghProfile.name || ghProfile.login || login,
          avatar_url: ghProfile.avatar_url || `https://github.com/${login}.png`,
          html_url: ghProfile.html_url || `https://github.com/${login}`,
          bio: ghProfile.bio || 'NetRunner Developer',
          company: ghProfile.company || undefined,
          location: ghProfile.location || undefined,
          blog: ghProfile.blog || undefined,
          twitter_username: ghProfile.twitter_username || undefined,
          public_repos: ghProfile.public_repos ?? (existingUser?.public_repos || 0),
          public_gists: ghProfile.public_gists ?? 0,
          followers: ghProfile.followers ?? (existingUser?.followers || 0),
          following: ghProfile.following ?? (existingUser?.following || 0),
          created_at: ghProfile.created_at || new Date().toISOString()
        };
        setCurrentUser(freshUser);
        localStorage.setItem('netrunner_github_user', JSON.stringify(freshUser));
      }

      const reposRes = await fetch(
        `https://api.github.com/users/${encodeURIComponent(login)}/repos?sort=updated&per_page=30`,
        { headers }
      );
      if (reposRes.ok) {
        const rawRepos = await reposRes.json();
        const formattedRepos: GitHubRepo[] = Array.isArray(rawRepos)
          ? rawRepos.map((r: any) => ({
              id: r.id,
              name: r.name,
              description: r.description,
              html_url: r.html_url,
              stars: r.stargazers_count,
              forks: r.forks_count,
              language: r.language,
              updated_at: r.updated_at,
            }))
          : [];
        setUserRepos(formattedRepos);
        localStorage.setItem('netrunner_github_repos', JSON.stringify(formattedRepos));

        const totalStars = formattedRepos.reduce((acc, r) => acc + (r.stars || 0), 0);
        const totalForks = formattedRepos.reduce((acc, r) => acc + (r.forks || 0), 0);
        const languagesCount: Record<string, number> = {};
        formattedRepos.forEach(r => {
          if (r.language) languagesCount[r.language] = (languagesCount[r.language] || 0) + 1;
        });
        const topLanguages = Object.entries(languagesCount)
          .sort((a, b) => b[1] - a[1])
          .map(([language, count]) => ({ language, count }));

        const updatedStats: GitHubStats = {
          totalStars,
          totalForks,
          publicRepos: freshUser?.public_repos ?? formattedRepos.length,
          followers: freshUser?.followers ?? 0,
          topLanguages,
        };
        setUserStats(updatedStats);
        localStorage.setItem('netrunner_github_stats', JSON.stringify(updatedStats));
      }
    } catch (e) {
      console.warn('Real-time GitHub stats fetch notice:', e);
    }
  };

  // Firebase Auth State Listener & Firestore Sync (only when configured)
  useEffect(() => {
    if (!auth || !db) return;
    const unsubscribe = onAuthStateChanged(auth, async (fbUser) => {
      if (fbUser && db) {
        let userToUse: GitHubUser | null = currentUser;
        try {
          const userDocSnap = await getDoc(doc(db, 'users', fbUser.uid));
          if (userDocSnap.exists()) {
            const data = userDocSnap.data() as any;
            if (data.login) {
              userToUse = {
                login: data.login,
                name: data.name || data.displayName || data.login,
                avatar_url: data.avatar_url || data.photoURL || `https://github.com/${data.login}.png`,
                html_url: data.html_url || `https://github.com/${data.login}`,
                bio: data.bio || 'NetRunner Developer',
                company: data.company,
                location: data.location,
                blog: data.blog,
                twitter_username: data.twitter_username,
                public_repos: data.public_repos || 0,
                public_gists: data.public_gists || 0,
                followers: data.followers || 0,
                following: data.following || 0,
                created_at: data.created_at || new Date().toISOString()
              };
              setCurrentUser(userToUse);
              localStorage.setItem('netrunner_github_user', JSON.stringify(userToUse));
            }
          }
        } catch (err) {
          console.warn('Firestore user fetch notice:', err);
        }

        if (userToUse?.login) {
          fetchLiveGitHubStats(userToUse.login, userToUse);
        }

        try {
          const snapshot = await getDocs(collection(db, 'users', fbUser.uid, 'projects'));
          const cloudProjects: Project[] = [];
          snapshot.forEach((docSnap) => {
            cloudProjects.push(docSnap.data() as Project);
          });
          if (cloudProjects.length > 0) {
            setProjects(cloudProjects);
          }
        } catch (err) {
          console.warn('Firestore load warning:', err);
        }
      }
    });

    return () => unsubscribe();
  }, []);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3200);
  };

  const handleLoginSuccess = (
    user: GitHubUser,
    stats: GitHubStats,
    repos: GitHubRepo[],
    token?: string
  ) => {
    setCurrentUser(user);
    setUserStats(stats);
    setUserRepos(repos);
    localStorage.setItem('netrunner_github_user', JSON.stringify(user));
    localStorage.setItem('netrunner_github_stats', JSON.stringify(stats));
    localStorage.setItem('netrunner_github_repos', JSON.stringify(repos));
    if (token) {
      setGithubToken(token);
      localStorage.setItem('netrunner_github_token', token);
    }
  };

  const handleSignOut = async () => {
    try {
      if (auth) {
        await firebaseSignOut(auth);
      }
    } catch (e) {
      console.warn('Sign out error:', e);
    }
    setCurrentUser(null);
    setUserStats(null);
    setUserRepos([]);
    setGithubToken(null);
    localStorage.removeItem('netrunner_github_user');
    localStorage.removeItem('netrunner_github_stats');
    localStorage.removeItem('netrunner_github_repos');
    localStorage.removeItem('netrunner_github_token');
    showToast('Signed out of NetRunner');
    setActiveSettingsModal(null);
  };

  const handleRefreshStats = async () => {
    if (!currentUser?.login) return;
    setIsRefreshingStats(true);
    try {
      await fetchLiveGitHubStats(currentUser.login, currentUser);
      showToast('Live GitHub statistics synchronized!');
    } catch (e) {
      showToast('Could not refresh stats from GitHub API');
    } finally {
      setIsRefreshingStats(false);
    }
  };

  const handleCreateProject = (newProjData: Omit<Project, 'id' | 'updatedAt' | 'status' | 'filesCount'>) => {
    const newProj: Project = {
      ...newProjData,
      id: `proj-${Date.now()}`,
      updatedAt: 'Just now',
      status: 'active',
      filesCount: Math.floor(Math.random() * 8) + 4
    };

    const nextProjects = [newProj, ...projects];
    setProjects(nextProjects);
    localStorage.setItem('netrunner_projects', JSON.stringify(nextProjects));
    setIsSettingsOpen(false);
    setActiveSettingsSubPage(null);
    setActiveTab('home');

    if (auth?.currentUser && db) {
      setDoc(doc(db, 'users', auth.currentUser.uid, 'projects', newProj.id), {
        ...newProj,
        ownerId: auth.currentUser.uid,
        createdAt: new Date().toISOString()
      }).catch((err) => {
        console.warn('Firestore project write warning:', err);
      });
    }

    showToast(`Project "${newProj.name}" created!`);
  };

  const handleImportGitHubRepo = async (repo: GitHubRepo) => {
    const existing = projects.find(p => p.name === repo.name);
    if (existing) {
      showToast(`Repository "${repo.name}" is already in your workspace.`);
      return;
    }
    const newProj: Project = {
      id: `repo-${repo.id}`,
      name: repo.name,
      description: repo.description || 'Imported from GitHub',
      category: 'Fullstack',
      tags: [repo.language || 'Code', 'GitHub'],
      updatedAt: 'Imported',
      status: 'active',
      filesCount: 12,
      repoUrl: repo.html_url
    };

    setProjects(prev => [newProj, ...prev]);

    if (auth?.currentUser && db) {
      try {
        await setDoc(doc(db, 'users', auth.currentUser.uid, 'projects', newProj.id), {
          ...newProj,
          ownerId: auth.currentUser.uid,
          createdAt: new Date().toISOString()
        });
      } catch (err) {
        console.warn('Firestore import sync warning:', err);
      }
    }

    showToast(`Imported "${repo.name}" into workspace!`);
  };

  const handleDeleteProject = async (id: string) => {
    const removed = projects.find(p => p.id === id);
    setProjects(prev => prev.filter(p => p.id !== id));
    if (removed) {
      showToast(`Deleted ${removed.name}`);
    }
    if (auth?.currentUser && db) {
      try {
        await deleteDoc(doc(db, 'users', auth.currentUser.uid, 'projects', id));
      } catch (err) {
        console.warn('Firestore delete warning:', err);
      }
    }
  };

  const handleResetProjects = () => {
    const toDelete = [...projects];
    setProjects([]);
    localStorage.setItem('netrunner_projects', JSON.stringify([]));
    showToast('Cleared workspace projects to empty state');
    if (auth?.currentUser && db) {
      const uid = auth.currentUser.uid;
      toDelete.forEach((p) => {
        deleteDoc(doc(db!, 'users', uid, 'projects', p.id)).catch(() => {});
      });
    }
  };

  return (
    <MotionConfig reducedMotion="user">
      <div className={`min-h-screen w-full transition-colors duration-500 ease-[cubic-bezier(0.22,1,0.36,1)] ${
        isDarkMode ? 'bg-[#040507] text-white' : 'bg-[#eaedf2] text-neutral-900'
      } flex flex-col items-center justify-center p-0 sm:p-4 md:p-6 overflow-x-hidden selection:bg-neutral-500/20`}>

        {/* Desktop Controls (Frame toggle) */}
        <aside aria-label="Desktop Controls" className="hidden sm:flex items-center justify-between w-full max-w-md mb-3 px-2 text-xs">
          <div className="flex items-center gap-2">
            <span className={`font-mono font-medium ${isDarkMode ? 'text-neutral-300' : 'text-neutral-700'}`}>
              NetRunner Workspace
            </span>
          </div>

          <div className={`flex items-center gap-1 p-1 rounded-full border backdrop-blur-md ${
            isDarkMode ? 'bg-white/5 border-white/10 text-neutral-400' : 'bg-white border-neutral-200 text-neutral-600 shadow-sm'
          }`}>
            <button
              onClick={() => setIsDeviceFrame(true)}
              className={`px-3 py-1 rounded-full flex items-center gap-1.5 transition-all cursor-pointer ${
                isDeviceFrame
                  ? (isDarkMode ? 'bg-white/15 text-white shadow-sm' : 'bg-neutral-900 text-white shadow-sm')
                  : 'hover:text-black'
              }`}
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span>Mobile Frame</span>
            </button>
            <button
              onClick={() => setIsDeviceFrame(false)}
              className={`px-3 py-1 rounded-full flex items-center gap-1.5 transition-all cursor-pointer ${
                !isDeviceFrame
                  ? (isDarkMode ? 'bg-white/15 text-white shadow-sm' : 'bg-neutral-900 text-white shadow-sm')
                  : 'hover:text-black'
              }`}
            >
              <Monitor className="w-3.5 h-3.5" />
              <span>Fluid Canvas</span>
            </button>
          </div>
        </aside>

        {/* Main Container */}
        <main
          className={`relative w-full ${
            isDeviceFrame
              ? 'sm:max-w-[412px] sm:h-[870px] sm:rounded-[36px] sm:border sm:shadow-2xl'
              : 'max-w-xl h-screen sm:h-[90vh] sm:rounded-3xl sm:border'
          } ${
            isDarkMode
              ? 'theme-dark border-white/10 sm:shadow-[0_25px_70px_rgba(0,0,0,0.9)]'
              : 'theme-light border-neutral-200 sm:shadow-[0_20px_50px_rgba(0,0,0,0.08)]'
          } h-screen flex flex-col overflow-hidden transition-[border-color,box-shadow,max-width,height,border-radius] duration-500 ease-[cubic-bezier(0.22,1,0.36,1)]`}
        >
          {/* Screen Content Viewport */}
          <div className="relative flex-1 w-full h-full overflow-hidden">
            <AnimatePresence mode="popLayout" initial={false}>
              {activeSettingsSubPage ? (
                <motion.div
                  key={`settings-sub-${activeSettingsSubPage}`}
                  initial={{ opacity: 0, x: 24 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: 24 }}
                  transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
                  className="w-full h-full"
                >
                  <SettingsSubPageView
                    pageType={activeSettingsSubPage}
                    onBack={() => setActiveSettingsSubPage(null)}
                    onShowToast={showToast}
                    isDarkMode={isDarkMode}
                  />
                </motion.div>
              ) : isSettingsOpen ? (
                <motion.div
                  key="settings"
                  initial={{ opacity: 0, x: 24 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: 24 }}
                  transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
                  className="w-full h-full"
                >
                  <SettingsView
                    onBack={() => setIsSettingsOpen(false)}
                    onOpenModal={(modal) => {
                      if (
                        modal === 'connectors' ||
                        modal === 'models' ||
                        modal === 'agents' ||
                        modal === 'skills'
                      ) {
                        setActiveSettingsSubPage(modal);
                      } else {
                        setActiveSettingsModal(modal);
                      }
                    }}
                    isDarkMode={isDarkMode}
                    onToggleTheme={() => {
                      const next = !isDarkMode;
                      setIsDarkMode(next);
                      showToast(`Switched to ${next ? 'Dark' : 'Light'} appearance`);
                    }}
                    currentUser={currentUser}
                    onOpenGitHubAuth={() => setIsGitHubAuthOpen(true)}
                  />
                </motion.div>
              ) : activeTab === 'browse' ? (
                <motion.div
                  key="browse"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.18, ease: [0.16, 1, 0.3, 1] }}
                  className="w-full h-full"
                >
                  <BrowseView
                    onBackToHome={() => setActiveTab('home')}
                    onShowToast={showToast}
                    isDarkMode={isDarkMode}
                  />
                </motion.div>
              ) : activeTab === 'profile' ? (
                <motion.div
                  key="profile"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.18, ease: [0.16, 1, 0.3, 1] }}
                  className="w-full h-full"
                >
                  <ProfileView
                    onBackToHome={() => setActiveTab('home')}
                    onOpenSettings={() => setIsSettingsOpen(true)}
                    onShowToast={showToast}
                    currentUser={currentUser}
                    userStats={userStats}
                    userRepos={userRepos}
                    onOpenGitHubAuth={() => setIsGitHubAuthOpen(true)}
                    onSignOut={handleSignOut}
                    onRefreshStats={handleRefreshStats}
                    isRefreshing={isRefreshingStats}
                    isDarkMode={isDarkMode}
                  />
                </motion.div>
              ) : (
                <motion.div
                  key="dashboard"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.18, ease: [0.16, 1, 0.3, 1] }}
                  className="w-full h-full"
                >
                  <DashboardView
                    projects={projects}
                    onOpenNewProjectModal={() => setIsNewProjectModalOpen(true)}
                    onOpenSettings={() => setIsSettingsOpen(true)}
                    onOpenProjectWorkspace={(proj) => setActiveProjectWorkspace(proj)}
                    onDeleteProject={handleDeleteProject}
                    onResetProjects={handleResetProjects}
                    onShowToast={showToast}
                    currentUser={currentUser}
                    userRepos={userRepos}
                    onOpenGitHubAuth={() => setIsGitHubAuthOpen(true)}
                    onImportGitHubRepo={handleImportGitHubRepo}
                    isDarkMode={isDarkMode}
                  />
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          {/* Bottom Floating Navigation Dock */}
          {!isSettingsOpen && !activeSettingsSubPage && (
            <BottomDock
              activeTab={activeTab}
              onChangeTab={(tab) => setActiveTab(tab)}
              isDarkMode={isDarkMode}
            />
          )}

          {/* Frosted Glassmorphism Overlays Inside Main Viewport (shows NetRunner wallpaper behind blur) */}
          <GitHubAuthModal
            isOpen={isGitHubAuthOpen}
            onClose={() => setIsGitHubAuthOpen(false)}
            onLoginSuccess={handleLoginSuccess}
            onShowToast={showToast}
            isDarkMode={isDarkMode}
          />

          <NewProjectModal
            isOpen={isNewProjectModalOpen}
            onClose={() => setIsNewProjectModalOpen(false)}
            onCreateProject={handleCreateProject}
            isDarkMode={isDarkMode}
          />

          <SettingsModals
            activeModal={activeSettingsModal}
            onClose={() => setActiveSettingsModal(null)}
            onShowToast={showToast}
            currentUser={currentUser}
            onOpenGitHubAuth={() => {
              setActiveSettingsModal(null);
              setIsGitHubAuthOpen(true);
            }}
            onSignOut={handleSignOut}
            isDarkMode={isDarkMode}
          />
        </main>

        {/* Project-Scoped Google Gemini AI Workspace */}
        <AIChatModal
          isOpen={Boolean(activeProjectWorkspace)}
          project={activeProjectWorkspace}
          onClose={() => setActiveProjectWorkspace(null)}
          onOpenSettings={(modalType) => {
            setActiveProjectWorkspace(null);
            setIsSettingsOpen(true);
            setActiveSettingsSubPage(modalType);
          }}
          onShowToast={showToast}
          isDarkMode={isDarkMode}
        />

        {/* Floating System Toast */}
        <Toast message={toastMessage} onClear={() => setToastMessage(null)} />
      </div>
    </MotionConfig>
  );
}

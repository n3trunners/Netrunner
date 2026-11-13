export interface GitHubUser {
  login: string;
  name: string;
  avatar_url: string;
  html_url: string;
  bio?: string;
  company?: string;
  location?: string;
  blog?: string;
  twitter_username?: string;
  public_repos: number;
  public_gists: number;
  followers: number;
  following: number;
  created_at: string;
}

export interface GitHubRepo {
  id: number;
  name: string;
  description: string | null;
  html_url: string;
  stars: number;
  forks: number;
  language: string | null;
  updated_at: string;
}

export interface GitHubStats {
  totalStars: number;
  totalForks: number;
  publicRepos: number;
  followers: number;
  topLanguages: { language: string; count: number }[];
}

export interface Project {
  id: string;
  name: string;
  description: string;
  category: 'Hybrid' | 'AI Agent' | 'Fullstack' | 'Mobile' | 'Frontend' | 'API';
  architecture?: 'hybrid-kotlin-rust' | 'web' | 'fullstack';
  updatedAt: string;
  tags: string[];
  status: 'active' | 'ready' | 'idle';
  filesCount: number;
  repoUrl?: string;
}

export type ActiveTab = 'home' | 'browse' | 'profile';

export type SettingsModalType = 
  | null
  | 'connectors'
  | 'models'
  | 'agents'
  | 'skills'
  | 'language'
  | 'notifications'
  | 'editor'
  | 'data-storage'
  | 'security'
  | 'about'
  | 'profile-detail';

export interface UserCustomIntegration {
  id: string;
  name: string;
  type: string;
  endpointOrKey?: string;
  status: 'active' | 'configured';
  createdAt: string;
}

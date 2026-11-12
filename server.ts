import express, { Request, Response, RequestHandler } from 'express';
import { createServer as createViteServer } from 'vite';
import dotenv from 'dotenv';
import path from 'path';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;
const APP_URL = process.env.APP_URL || 'https://ais-dev-27eedfzzz5vdywch54hxxg-668765759056.asia-northeast1.run.app';

app.use(express.json());

// 1. Get GitHub OAuth Authorization URL
app.get('/api/auth/github/url', (req, res) => {
  const clientId = process.env.GITHUB_CLIENT_ID;
  const redirectUri = `${APP_URL}/auth/callback`;

  if (!clientId) {
    return res.json({
      configured: false,
      message: 'GITHUB_CLIENT_ID not configured yet in environment.',
      redirectUri,
    });
  }

  const params = new URLSearchParams({
    client_id: clientId,
    redirect_uri: redirectUri,
    scope: 'read:user,repo',
    allow_signup: 'true',
  });

  const authUrl = `https://github.com/login/oauth/authorize?${params.toString()}`;
  res.json({
    configured: true,
    url: authUrl,
    redirectUri,
  });
});

// 2. OAuth Callback Route - Handles GitHub OAuth Code Exchange
const handleOAuthCallback: RequestHandler = async (req, res) => {
  const { code, error, error_description } = req.query;

  if (error || !code) {
    return res.send(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>Authentication Error</title>
          <style>body{background:#0a0c10;color:#fff;font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;}</style>
        </head>
        <body>
          <div style="text-align:center;">
            <h3>GitHub Authentication Failed</h3>
            <p style="color:#888;">${error_description || error || 'Missing authorization code'}</p>
            <button onclick="window.close()" style="background:#fff;color:#000;border:none;padding:8px 16px;border-radius:20px;cursor:pointer;">Close Window</button>
          </div>
        </body>
      </html>
    `);
  }

  try {
    const clientId = process.env.GITHUB_CLIENT_ID;
    const clientSecret = process.env.GITHUB_CLIENT_SECRET;

    if (!clientId || !clientSecret) {
      throw new Error('Missing GitHub OAuth Client credentials in environment.');
    }

    // Exchange code for access token
    const tokenResponse = await fetch('https://github.com/login/oauth/access_token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify({
        client_id: clientId,
        client_secret: clientSecret,
        code,
        redirect_uri: `${APP_URL}/auth/callback`,
      }),
    });

    const tokenData = await tokenResponse.json();

    if (tokenData.error) {
      throw new Error(tokenData.error_description || tokenData.error);
    }

    const accessToken = tokenData.access_token;

    // Fetch user details from GitHub
    const userResponse = await fetch('https://api.github.com/user', {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'User-Agent': 'NetRunner-App',
      },
    });

    const userData = await userResponse.json();

    // Send success postMessage to opener window and close popup
    res.send(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>Authenticating with NetRunner...</title>
          <style>
            body {
              background: #07080a;
              color: #f3f4f6;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
              display: flex;
              align-items: center;
              justify-content: center;
              height: 100vh;
              margin: 0;
            }
            .card {
              text-align: center;
              padding: 24px;
              background: rgba(255,255,255,0.05);
              border: 1px solid rgba(255,255,255,0.1);
              border-radius: 20px;
            }
          </style>
        </head>
        <body>
          <div class="card">
            <h3 style="margin-bottom: 8px;">GitHub Authentication Successful!</h3>
            <p style="color: #9ca3af; font-size: 14px;">Syncing with NetRunner workspace...</p>
          </div>
          <script>
            const payload = {
              type: 'OAUTH_AUTH_SUCCESS',
              token: ${JSON.stringify(accessToken)},
              user: ${JSON.stringify(userData)}
            };
            if (window.opener) {
              window.opener.postMessage(payload, '*');
              setTimeout(() => window.close(), 600);
            } else {
              window.location.href = '/';
            }
          </script>
        </body>
      </html>
    `);
  } catch (err: any) {
    console.error('GitHub token exchange error:', err);
    res.status(500).send(`
      <!DOCTYPE html>
      <html>
        <head><title>OAuth Error</title><style>body{background:#0a0c10;color:#fff;font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;}</style></head>
        <body>
          <div style="text-align:center;max-width:400px;padding:20px;">
            <h3>Authentication Error</h3>
            <p style="color:#ef4444;font-size:14px;">${err.message || 'Unknown authentication failure'}</p>
            <button onclick="window.close()" style="margin-top:12px;background:#fff;color:#000;border:none;padding:8px 16px;border-radius:20px;cursor:pointer;">Close Window</button>
          </div>
        </body>
      </html>
    `);
  }
};

app.get(['/auth/callback', '/auth/callback/'], handleOAuthCallback);

// 3. Real GitHub Stats API Proxy (fetches real user profile + repositories)
app.get('/api/github/stats/:username', async (req, res) => {
  const { username } = req.params;
  const token = req.headers.authorization?.replace('Bearer ', '') || process.env.GITHUB_TOKEN;

  const headers: Record<string, string> = {
    'User-Agent': 'NetRunner-App',
    Accept: 'application/vnd.github.v3+json',
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  try {
    // 1. Fetch user profile
    const userRes = await fetch(`https://api.github.com/users/${encodeURIComponent(username)}`, { headers });
    if (!userRes.ok) {
      if (userRes.status === 404) {
        return res.status(404).json({ error: 'GitHub user not found' });
      }
      const errText = await userRes.text();
      return res.status(userRes.status).json({ error: `GitHub API error: ${errText}` });
    }
    const profile = await userRes.json();

    // 2. Fetch user's public repositories
    const reposRes = await fetch(
      `https://api.github.com/users/${encodeURIComponent(username)}/repos?sort=updated&per_page=12&type=owner`,
      { headers }
    );
    const repos = reposRes.ok ? await reposRes.json() : [];

    // Calculate aggregated real metrics
    const totalStars = Array.isArray(repos)
      ? repos.reduce((acc: number, r: any) => acc + (r.stargazers_count || 0), 0)
      : 0;

    const totalForks = Array.isArray(repos)
      ? repos.reduce((acc: number, r: any) => acc + (r.forks_count || 0), 0)
      : 0;

    const languagesCount: Record<string, number> = {};
    if (Array.isArray(repos)) {
      repos.forEach((r: any) => {
        if (r.language) {
          languagesCount[r.language] = (languagesCount[r.language] || 0) + 1;
        }
      });
    }

    const topLanguages = Object.entries(languagesCount)
      .sort((a, b) => b[1] - a[1])
      .map(([lang, count]) => ({ language: lang, count }));

    res.json({
      profile: {
        login: profile.login,
        name: profile.name || profile.login,
        avatar_url: profile.avatar_url,
        html_url: profile.html_url,
        bio: profile.bio || 'NetRunner Developer',
        company: profile.company,
        location: profile.location,
        blog: profile.blog,
        twitter_username: profile.twitter_username,
        public_repos: profile.public_repos,
        public_gists: profile.public_gists,
        followers: profile.followers,
        following: profile.following,
        created_at: profile.created_at,
      },
      stats: {
        totalStars,
        totalForks,
        publicRepos: profile.public_repos,
        followers: profile.followers,
        topLanguages,
      },
      repositories: Array.isArray(repos)
        ? repos.map((r: any) => ({
            id: r.id,
            name: r.name,
            description: r.description,
            html_url: r.html_url,
            stars: r.stargazers_count,
            forks: r.forks_count,
            language: r.language,
            updated_at: r.updated_at,
            default_branch: r.default_branch,
            visibility: r.visibility,
          }))
        : [],
    });
  } catch (err: any) {
    console.error('Error fetching GitHub stats:', err);
    res.status(500).json({ error: err.message || 'Failed to fetch GitHub stats' });
  }
});

// Setup Vite middleware for local development
async function startServer() {
  if (process.env.NODE_ENV !== 'production') {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.resolve(__dirname, 'dist');
    app.use(express.static(distPath));
    app.get('*', (req, res) => {
      res.sendFile(path.resolve(distPath, 'index.html'));
    });
  }

  app.listen(PORT, () => {
    console.log(`NetRunner Server listening on port ${PORT}`);
  });
}

startServer();

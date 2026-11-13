import express, { Request, Response, RequestHandler } from 'express';
import { createServer as createViteServer } from 'vite';
import { GoogleGenAI, Content } from '@google/genai';
import dotenv from 'dotenv';
import path from 'path';
import fs from 'fs/promises';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;
const APP_URL = process.env.APP_URL || 'https://ais-dev-27eedfzzz5vdywch54hxxg-668765759056.asia-northeast1.run.app';

const DEFAULT_GEMINI_MODEL = 'gemini-4-hydrogen';
const DEPRECATED_MODELS = new Set([
  'gemini-1.5-flash',
  'gemini-1.5-pro',
  'gemini-pro',
  'gemini-2.0-flash',
  'gemini-2.0-pro',
  'gemini-2.0-flash-thinking',
  'gemini-2.5-flash',
  'gemini-2.5-pro',
]);

function resolveGeminiModel(requestedModel?: string): string {
  const trimmed = typeof requestedModel === 'string' ? requestedModel.trim() : '';
  if (!trimmed || DEPRECATED_MODELS.has(trimmed)) {
    return DEFAULT_GEMINI_MODEL;
  }
  return trimmed;
}

function getGeminiClientWithUserToken(userApiKey: string): GoogleGenAI {
  const trimmed = userApiKey.trim();
  if (!trimmed) {
    const err = new Error(
      'Your own AI token is required. NetRunner does not provide free tokens — add your personal key in Settings > Models.'
    ) as Error & { status?: number; code?: string };
    err.status = 401;
    err.code = 'TOKEN_REQUIRED';
    throw err;
  }
  return new GoogleGenAI({
    apiKey: trimmed,
    httpOptions: {
      headers: {
        'User-Agent': 'aistudio-build',
      },
    },
  });
}

function extractUserToken(req: Request): string {
  const headerKey = req.header('x-ai-api-key') || req.header('x-gemini-api-key') || '';
  if (headerKey.trim()) return headerKey.trim();
  const auth = req.header('authorization') || '';
  if (auth.toLowerCase().startsWith('bearer ')) {
    return auth.slice(7).trim();
  }
  return '';
}

app.use(express.json({ limit: '2mb' }));

// 1. Get GitHub OAuth Authorization URL
app.get('/api/auth/github/url', (req, res) => {
  const clientId = process.env.GITHUB_CLIENT_ID;
  const redirectUri = `${APP_URL}/auth/callback`;
  const platform = typeof req.query.platform === 'string' ? req.query.platform : 'web';

  if (!clientId) {
    return res.json({
      configured: false,
      message: 'GITHUB_CLIENT_ID not configured yet in environment.',
      redirectUri,
    });
  }

  const nonce = Math.random().toString(36).slice(2) + Date.now().toString(36);
  const state = platform === 'android' ? `android:${nonce}` : `web:${nonce}`;

  const params = new URLSearchParams({
    client_id: clientId,
    redirect_uri: redirectUri,
    scope: 'read:user,repo',
    allow_signup: 'true',
    state,
  });

  const authUrl = `https://github.com/login/oauth/authorize?${params.toString()}`;
  res.json({
    configured: true,
    url: authUrl,
    redirectUri,
  });
});

// 2. OAuth Callback Route - Handles GitHub OAuth Code Exchange (Web Popup + Android Deep Link)
const handleOAuthCallback: RequestHandler = async (req, res) => {
  const { code, error, error_description, state } = req.query;
  const isAndroid = typeof state === 'string' && state.startsWith('android:');

  if (error || !code) {
    if (isAndroid) {
      const errMsg = encodeURIComponent(String(error_description || error || 'Missing authorization code'));
      return res.redirect(`netrunner://auth?error=${errMsg}`);
    }
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

    if (isAndroid) {
      const deepLink = `netrunner://auth?token=${encodeURIComponent(accessToken)}`;
      return res.send(`
        <!DOCTYPE html>
        <html>
          <head>
            <title>Returning to NetRunner...</title>
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <style>
              body { background: #07080a; color: #f3f4f6; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; padding: 20px; }
              .card { text-align: center; padding: 28px 24px; background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.12); border-radius: 24px; max-width: 340px; width: 100%; }
              .btn { display: inline-block; margin-top: 16px; background: #fff; color: #000; text-decoration: none; font-weight: 600; font-size: 13px; padding: 12px 24px; border-radius: 999px; }
            </style>
          </head>
          <body>
            <div class="card">
              <h3 style="margin: 0 0 8px;">GitHub Connected</h3>
              <p style="color: #9ca3af; font-size: 13px; margin: 0;">Returning to NetRunner Android...</p>
              <a class="btn" href="${deepLink}">Open NetRunner</a>
            </div>
            <script>
              window.location.replace(${JSON.stringify(deepLink)});
            </script>
          </body>
        </html>
      `);
    }

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
    if (isAndroid) {
      return res.redirect(`netrunner://auth?error=${encodeURIComponent(err.message || 'Authentication failed')}`);
    }
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

app.post('/api/auth/github/exchange', async (req: Request, res: Response) => {
  const { code, redirectUri } = (req.body || {}) as { code?: string; redirectUri?: string };
  if (!code) {
    return res.status(400).json({ error: 'Missing authorization code.' });
  }
  const clientId = process.env.GITHUB_CLIENT_ID;
  const clientSecret = process.env.GITHUB_CLIENT_SECRET;
  if (!clientId || !clientSecret) {
    return res.status(500).json({ error: 'GitHub OAuth credentials are not configured on the server.' });
  }

  try {
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
        redirect_uri: redirectUri || `${APP_URL}/auth/callback`,
      }),
    });
    const tokenData = await tokenResponse.json();
    if (tokenData.error || !tokenData.access_token) {
      return res.status(400).json({ error: tokenData.error_description || tokenData.error || 'Token exchange failed.' });
    }
    return res.json({ access_token: tokenData.access_token });
  } catch (err: any) {
    return res.status(500).json({ error: err?.message || 'Failed to exchange OAuth code.' });
  }
});

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

// 4. Verify User-Supplied BYOK AI Token
app.post('/api/ai/verify', async (req: Request, res: Response) => {
  const userToken = extractUserToken(req);
  const { model: requestedModel, provider = 'gemini', baseUrl } = req.body as {
    model?: string;
    provider?: 'gemini' | 'openai-compatible';
    baseUrl?: string;
  };

  if (!userToken) {
    return res.status(401).json({
      ok: false,
      code: 'TOKEN_REQUIRED',
      error: 'No API token provided. NetRunner requires your own token (free tokens are not provided).',
    });
  }

  try {
    if (provider === 'openai-compatible') {
      const endpoint = (baseUrl || 'https://api.openai.com/v1').replace(/\/+$/, '');
      const checkRes = await fetch(`${endpoint}/models`, {
        headers: {
          Authorization: `Bearer ${userToken}`,
        },
      });
      if (!checkRes.ok) {
        const text = await checkRes.text().catch(() => '');
        return res.status(checkRes.status).json({
          ok: false,
          error: `Provider rejected token (${checkRes.status}): ${text.slice(0, 180)}`,
        });
      }
      return res.json({ ok: true, provider, model: requestedModel || 'custom' });
    }

    const ai = getGeminiClientWithUserToken(userToken);
    const resolvedModel = resolveGeminiModel(requestedModel);
    await ai.models.generateContent({
      model: resolvedModel,
      contents: 'Respond with OK.',
    });
    return res.json({ ok: true, provider: 'gemini', model: resolvedModel });
  } catch (err: any) {
    const status = typeof err?.status === 'number' ? err.status : 400;
    return res.status(status >= 400 ? status : 400).json({
      ok: false,
      error: err?.message || 'Token verification failed.',
    });
  }
});

// 5. Server-Side BYOK AI Streaming Proxy (User Must Supply Their Own Token)
app.post('/api/gemini/chat/stream', async (req: Request, res: Response) => {
  const userToken = extractUserToken(req);
  const providerHeader = req.header('x-ai-provider');
  const baseUrlHeader = req.header('x-ai-base-url');

  const {
    model: requestedModel,
    messages,
    provider: bodyProvider,
    baseUrl: bodyBaseUrl,
  } = req.body as {
    model?: string;
    messages?: Array<{ role: 'user' | 'assistant' | 'system'; content: string }>;
    provider?: 'gemini' | 'openai-compatible';
    baseUrl?: string;
  };

  const provider = (providerHeader || bodyProvider || 'gemini') as 'gemini' | 'openai-compatible';
  const baseUrl = baseUrlHeader || bodyBaseUrl;

  if (!userToken) {
    return res.status(401).json({
      code: 'TOKEN_REQUIRED',
      error: 'Your own AI token is required. NetRunner does not provide free tokens — add your personal key in Settings > Models.',
    });
  }

  if (!Array.isArray(messages) || messages.length === 0) {
    return res.status(400).json({ error: 'Messages array is required.' });
  }

  let aborted = false;
  req.on('close', () => {
    aborted = true;
  });

  try {
    if (provider === 'openai-compatible') {
      const endpoint = (baseUrl || 'https://api.openai.com/v1').replace(/\/+$/, '');
      const modelId = (requestedModel || 'gpt-4o-mini').trim();

      const upstream = await fetch(`${endpoint}/chat/completions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${userToken}`,
        },
        body: JSON.stringify({
          model: modelId,
          messages,
          stream: true,
        }),
      });

      if (!upstream.ok || !upstream.body) {
        const errText = await upstream.text().catch(() => 'Upstream provider error');
        return res.status(upstream.status || 502).json({
          error: `Provider error (${upstream.status}): ${errText.slice(0, 240)}`,
        });
      }

      res.setHeader('Content-Type', 'text/event-stream; charset=utf-8');
      res.setHeader('Cache-Control', 'no-cache, no-transform');
      res.setHeader('Connection', 'keep-alive');
      res.flushHeaders?.();

      const reader = upstream.body.getReader();
      const decoder = new TextDecoder();
      let buf = '';

      while (!aborted) {
        const { done, value } = await reader.read();
        if (done) break;
        buf += decoder.decode(value, { stream: true });
        const lines = buf.split('\n');
        buf = lines.pop() || '';
        for (const line of lines) {
          const trimmed = line.trim();
          if (!trimmed.startsWith('data:')) continue;
          const dataStr = trimmed.slice(5).trim();
          if (dataStr === '[DONE]') {
            if (!aborted) res.write('data: [DONE]\n\n');
            res.end();
            return;
          }
          try {
            const parsed = JSON.parse(dataStr);
            const delta = parsed?.choices?.[0]?.delta?.content;
            if (delta) {
              res.write(`data: ${JSON.stringify({ delta })}\n\n`);
            }
          } catch {
            // ignore partial chunk
          }
        }
      }
      if (!aborted) res.write('data: [DONE]\n\n');
      res.end();
      return;
    }

    const ai = getGeminiClientWithUserToken(userToken);
    const resolvedModel = resolveGeminiModel(requestedModel);

    const systemParts: string[] = [];
    const conversationContents: Content[] = [];

    for (const msg of messages) {
      if (!msg || typeof msg.content !== 'string') continue;
      const text = msg.content.trim();
      if (!text) continue;

      if (msg.role === 'system') {
        systemParts.push(text);
        continue;
      }

      const geminiRole: 'user' | 'model' = msg.role === 'assistant' ? 'model' : 'user';

      // Gemini multiturn requests must begin with a user turn
      if (conversationContents.length === 0 && geminiRole !== 'user') {
        continue;
      }

      const lastContent = conversationContents[conversationContents.length - 1];
      if (lastContent && lastContent.role === geminiRole && lastContent.parts) {
        lastContent.parts.push({ text });
      } else {
        conversationContents.push({
          role: geminiRole,
          parts: [{ text }],
        });
      }
    }

    if (conversationContents.length === 0) {
      return res.status(400).json({ error: 'At least one user message is required.' });
    }

    const systemInstruction = systemParts.length > 0 ? systemParts.join('\n\n') : undefined;

    const stream = await ai.models.generateContentStream({
      model: resolvedModel,
      contents: conversationContents,
      ...(systemInstruction ? { config: { systemInstruction } } : {}),
    });

    res.setHeader('Content-Type', 'text/event-stream; charset=utf-8');
    res.setHeader('Cache-Control', 'no-cache, no-transform');
    res.setHeader('Connection', 'keep-alive');
    res.flushHeaders?.();

    for await (const chunk of stream) {
      if (aborted) break;
      const text = chunk.text;
      if (text) {
        res.write(`data: ${JSON.stringify({ delta: text })}\n\n`);
      }
    }

    if (!aborted) {
      res.write('data: [DONE]\n\n');
    }
    res.end();
  } catch (err: any) {
    console.error('Server-side BYOK AI streaming error:', err);
    const status = typeof err?.status === 'number'
      ? err.status
      : typeof err?.statusCode === 'number'
        ? err.statusCode
        : 500;

    let message = err?.message || 'Failed to generate response.';
    if (status === 400 || status === 401 || status === 403) {
      message = err?.code === 'TOKEN_REQUIRED'
        ? err.message
        : 'Your API token was rejected or the model is invalid. Check your personal key in Settings > Models.';
    } else if (status === 404) {
      message = 'Requested model was not found or is unsupported for your token.';
    } else if (status === 429) {
      message = 'Your token quota or rate limit was reached. Check your provider billing/limits.';
    }

    if (!res.headersSent) {
      return res.status(status).json({ error: message });
    }

    if (!aborted) {
      res.write(`data: ${JSON.stringify({ error: message })}\n\n`);
    }
    res.end();
  }
});

// 6. GitHub Actions CI/CD Workflow & Runs API
app.get('/api/ci/workflow', async (_req: Request, res: Response) => {
  try {
    const workflowPath = path.resolve(process.cwd(), '.github', 'workflows', 'build.yml');
    const content = await fs.readFile(workflowPath, 'utf8');
    res.json({
      path: '.github/workflows/build.yml',
      triggers: ['push (all branches)', 'pull_request (main, master)', 'workflow_dispatch'],
      jobs: ['build-web', 'check-rust-core', 'build-android-apk'],
      content,
    });
  } catch (err: any) {
    res.status(404).json({ error: err?.message || 'Workflow file not found' });
  }
});

app.get('/api/github/actions/:owner/:repo', async (req: Request, res: Response) => {
  const { owner, repo } = req.params;
  const token = req.headers.authorization?.replace('Bearer ', '') || process.env.GITHUB_TOKEN;

  const headers: Record<string, string> = {
    'User-Agent': 'NetRunner-App',
    Accept: 'application/vnd.github.v3+json',
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  try {
    const runsRes = await fetch(
      `https://api.github.com/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/actions/runs?per_page=6`,
      { headers }
    );
    if (!runsRes.ok) {
      const errText = await runsRes.text().catch(() => '');
      return res.status(runsRes.status).json({
        error: `GitHub Actions API error (${runsRes.status}): ${errText.slice(0, 180)}`,
      });
    }
    const data = await runsRes.json();
    const runs = Array.isArray(data.workflow_runs)
      ? data.workflow_runs.map((r: any) => ({
          id: r.id,
          name: r.name,
          status: r.status,
          conclusion: r.conclusion,
          head_branch: r.head_branch,
          head_sha: r.head_sha?.slice(0, 7),
          event: r.event,
          html_url: r.html_url,
          created_at: r.created_at,
          updated_at: r.updated_at,
        }))
      : [];
    res.json({ runs });
  } catch (err: any) {
    res.status(500).json({ error: err?.message || 'Failed to fetch GitHub Actions runs' });
  }
});

app.post('/api/github/actions/:owner/:repo/dispatch', async (req: Request, res: Response) => {
  const { owner, repo } = req.params;
  const { ref = 'main', workflow = 'build.yml' } = (req.body || {}) as {
    ref?: string;
    workflow?: string;
  };
  const token = req.headers.authorization?.replace('Bearer ', '');
  if (!token) {
    return res.status(401).json({
      error: 'GitHub OAuth token with repo scope is required to trigger workflow dispatch.',
    });
  }

  try {
    const dispatchRes = await fetch(
      `https://api.github.com/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/actions/workflows/${encodeURIComponent(workflow)}/dispatches`,
      {
        method: 'POST',
        headers: {
          'User-Agent': 'NetRunner-App',
          Accept: 'application/vnd.github.v3+json',
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ ref }),
      }
    );

    if (!dispatchRes.ok) {
      const errText = await dispatchRes.text().catch(() => '');
      return res.status(dispatchRes.status).json({
        error: `Failed to dispatch workflow (${dispatchRes.status}): ${errText.slice(0, 200)}`,
      });
    }

    res.json({ ok: true, message: `Triggered ${workflow} on ${owner}/${repo}@${ref}` });
  } catch (err: any) {
    res.status(500).json({ error: err?.message || 'Failed to dispatch workflow' });
  }
});

// 5. Hybrid Architecture Native Source Explorer API (Kotlin + Compose & Rust Core Daemon)
const HYBRID_ROOT = path.resolve(process.cwd(), 'native');

interface HybridFileNode {
  path: string;
  name: string;
  layer: 'rust-core' | 'android-compose';
  language: string;
  bytes: number;
  lines: number;
}

function detectLanguage(filePath: string): string {
  if (filePath.endsWith('.rs')) return 'rust';
  if (filePath.endsWith('.kt') || filePath.endsWith('.kts')) return 'kotlin';
  if (filePath.endsWith('.toml')) return 'toml';
  if (filePath.endsWith('.xml')) return 'xml';
  return 'text';
}

async function collectHybridFiles(dir: string, base: string): Promise<HybridFileNode[]> {
  const entries = await fs.readdir(dir, { withFileTypes: true });
  const results: HybridFileNode[] = [];

  for (const entry of entries) {
    if (
      entry.name === 'target' ||
      entry.name === 'build' ||
      entry.name === '.gradle' ||
      entry.name.startsWith('.')
    ) {
      continue;
    }
    const fullPath = path.join(dir, entry.name);
    const relPath = path.relative(base, fullPath).replace(/\\/g, '/');

    if (entry.isDirectory()) {
      const nested = await collectHybridFiles(fullPath, base);
      results.push(...nested);
    } else if (entry.isFile()) {
      const content = await fs.readFile(fullPath, 'utf8');
      const layer: HybridFileNode['layer'] = relPath.startsWith('rust-core')
        ? 'rust-core'
        : 'android-compose';
      results.push({
        path: relPath,
        name: entry.name,
        layer,
        language: detectLanguage(relPath),
        bytes: Buffer.byteLength(content, 'utf8'),
        lines: content.split('\n').length,
      });
    }
  }

  return results.sort((a, b) => a.path.localeCompare(b.path));
}

app.get('/api/hybrid/tree', async (_req: Request, res: Response) => {
  try {
    const files = await collectHybridFiles(HYBRID_ROOT, HYBRID_ROOT);
    res.json({
      architecture: 'Hybrid Architecture — Kotlin + Jetpack Compose UI & Rust Core Engine/Daemon',
      files,
    });
  } catch (err: any) {
    res.status(500).json({ error: err?.message || 'Failed to read native hybrid tree' });
  }
});

app.get('/api/hybrid/file', async (req: Request, res: Response) => {
  const requested = typeof req.query.path === 'string' ? req.query.path : '';
  if (!requested) {
    return res.status(400).json({ error: 'Query parameter "path" is required.' });
  }

  const resolved = path.resolve(HYBRID_ROOT, requested);
  if (!resolved.startsWith(HYBRID_ROOT + path.sep)) {
    return res.status(403).json({ error: 'Invalid path outside native hybrid directory.' });
  }

  try {
    const content = await fs.readFile(resolved, 'utf8');
    res.json({
      path: requested,
      language: detectLanguage(requested),
      content,
    });
  } catch (err: any) {
    res.status(404).json({ error: err?.message || 'File not found' });
  }
});

app.get('/api/hybrid/bundle', async (_req: Request, res: Response) => {
  try {
    const nodes = await collectHybridFiles(HYBRID_ROOT, HYBRID_ROOT);
    const files = await Promise.all(
      nodes.map(async (node) => {
        const content = await fs.readFile(path.join(HYBRID_ROOT, node.path), 'utf8');
        return { ...node, content };
      })
    );
    res.setHeader('Content-Disposition', 'attachment; filename="netrunner-hybrid-kotlin-rust.json"');
    res.json({
      name: 'NetRunner Hybrid Architecture',
      uiLayer: 'Kotlin + Jetpack Compose (Android ARM64)',
      coreLayer: 'Rust Core Engine/Daemon (Tokio + PRoot PTY + JNI/UDS)',
      exportedAt: new Date().toISOString(),
      files,
    });
  } catch (err: any) {
    res.status(500).json({ error: err?.message || 'Failed to export bundle' });
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

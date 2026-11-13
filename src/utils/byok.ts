export type AIProviderType = 'gemini' | 'openai-compatible';

export interface ByokConfig {
  apiKey: string;
  provider: AIProviderType;
  baseUrl: string;
  model: string;
  hasToken: boolean;
}

export interface SavedApiKeyEntry {
  id: string;
  provider: 'gemini-api';
  providerLabel: string;
  apiKey: string;
  createdAt: string;
}

export const DEFAULT_GEMINI_MODEL = 'gemini-4-hydrogen';

export const DEPRECATED_GEMINI_MODELS = new Set([
  'gemini-1.5-flash',
  'gemini-1.5-pro',
  'gemini-pro',
  'gemini-2.0-flash',
  'gemini-2.0-pro',
  'gemini-2.0-flash-thinking',
  'gemini-2.5-flash',
  'gemini-2.5-pro',
]);

const STORAGE_KEYS = {
  apiKey: 'netrunner_user_api_key',
  provider: 'netrunner_ai_provider',
  baseUrl: 'netrunner_ai_base_url',
  model: 'gemini_model',
  savedKeys: 'netrunner_saved_api_keys',
};

export const BYOK_UPDATED_EVENT = 'netrunner-byok-updated';

export function resolveModelId(requested: string, provider: AIProviderType): string {
  const trimmed = requested.trim();
  if (provider === 'gemini') {
    if (!trimmed || DEPRECATED_GEMINI_MODELS.has(trimmed)) {
      return DEFAULT_GEMINI_MODEL;
    }
    return trimmed;
  }
  return trimmed || 'gpt-4o-mini';
}

export function getByokConfig(): ByokConfig {
  const apiKey = (localStorage.getItem(STORAGE_KEYS.apiKey) || '').trim();
  const rawProvider = localStorage.getItem(STORAGE_KEYS.provider);
  const provider: AIProviderType =
    rawProvider === 'openai-compatible' ? 'openai-compatible' : 'gemini';
  const baseUrl = (localStorage.getItem(STORAGE_KEYS.baseUrl) || '').trim();
  const storedModel = localStorage.getItem(STORAGE_KEYS.model) || '';
  const model = resolveModelId(storedModel, provider);

  return {
    apiKey,
    provider,
    baseUrl,
    model,
    hasToken: apiKey.length > 0,
  };
}

export function saveByokConfig(next: Partial<Omit<ByokConfig, 'hasToken'>>): ByokConfig {
  const current = getByokConfig();
  const provider = next.provider ?? current.provider;
  const apiKey = next.apiKey !== undefined ? next.apiKey.trim() : current.apiKey;
  const baseUrl = next.baseUrl !== undefined ? next.baseUrl.trim() : current.baseUrl;
  const model = resolveModelId(next.model ?? current.model, provider);

  if (apiKey) {
    localStorage.setItem(STORAGE_KEYS.apiKey, apiKey);
  } else {
    localStorage.removeItem(STORAGE_KEYS.apiKey);
  }
  localStorage.setItem(STORAGE_KEYS.provider, provider);
  if (baseUrl) {
    localStorage.setItem(STORAGE_KEYS.baseUrl, baseUrl);
  } else {
    localStorage.removeItem(STORAGE_KEYS.baseUrl);
  }
  localStorage.setItem(STORAGE_KEYS.model, model);

  window.dispatchEvent(new Event(BYOK_UPDATED_EVENT));
  return getByokConfig();
}

export function clearByokToken(): ByokConfig {
  localStorage.removeItem(STORAGE_KEYS.apiKey);
  window.dispatchEvent(new Event(BYOK_UPDATED_EVENT));
  return getByokConfig();
}

export function getSavedApiKeys(): SavedApiKeyEntry[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.savedKeys);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        return parsed.filter((item) => item && typeof item.apiKey === 'string' && item.apiKey.trim().length > 0);
      }
    }
  } catch {
    // ignore malformed storage
  }
  const legacyKey = (localStorage.getItem(STORAGE_KEYS.apiKey) || '').trim();
  if (legacyKey) {
    const migrated: SavedApiKeyEntry[] = [
      {
        id: `key-${Date.now()}`,
        provider: 'gemini-api',
        providerLabel: 'Gemini API',
        apiKey: legacyKey,
        createdAt: new Date().toLocaleDateString(),
      },
    ];
    localStorage.setItem(STORAGE_KEYS.savedKeys, JSON.stringify(migrated));
    return migrated;
  }
  return [];
}

function syncActiveKeyFromList(entries: SavedApiKeyEntry[]) {
  localStorage.setItem(STORAGE_KEYS.savedKeys, JSON.stringify(entries));
  const primary = entries[0];
  if (primary?.apiKey) {
    saveByokConfig({
      apiKey: primary.apiKey,
      provider: 'gemini',
      model: DEFAULT_GEMINI_MODEL,
    });
  } else {
    clearByokToken();
  }
}

export function saveApiKeyEntry(apiKey: string, existingId?: string): SavedApiKeyEntry[] {
  const trimmed = apiKey.trim();
  if (!trimmed) return getSavedApiKeys();
  const current = getSavedApiKeys();
  let updated: SavedApiKeyEntry[];
  if (existingId) {
    updated = current.map((entry) =>
      entry.id === existingId
        ? { ...entry, apiKey: trimmed, provider: 'gemini-api', providerLabel: 'Gemini API' }
        : entry
    );
  } else {
    const entry: SavedApiKeyEntry = {
      id: `key-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
      provider: 'gemini-api',
      providerLabel: 'Gemini API',
      apiKey: trimmed,
      createdAt: new Date().toLocaleDateString(),
    };
    updated = [...current, entry];
  }
  syncActiveKeyFromList(updated);
  return updated;
}

export function deleteApiKeyEntry(id: string): SavedApiKeyEntry[] {
  const current = getSavedApiKeys();
  const updated = current.filter((entry) => entry.id !== id);
  syncActiveKeyFromList(updated);
  return updated;
}

export function maskToken(token: string): string {
  const trimmed = token.trim();
  if (!trimmed) return '';
  if (trimmed.length <= 10) return '••••••••';
  return `${trimmed.slice(0, 5)}••••••${trimmed.slice(-4)}`;
}

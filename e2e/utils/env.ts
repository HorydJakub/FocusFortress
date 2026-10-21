/**
 * Central place for environment configuration. Every value has a default that matches
 * the stock `docker compose up` stack, so the suite runs with zero configuration.
 */
const trimSlash = (url: string) => url.replace(/\/+$/, '');

export const env = {
  baseUrl: trimSlash(process.env.BASE_URL ?? 'http://localhost:3000'),
  apiUrl: trimSlash(process.env.API_URL ?? 'http://localhost:8080/api'),
  mailhogUrl: trimSlash(process.env.MAILHOG_URL ?? 'http://localhost:8025'),
  /** Only set this to a real key if the backend was started with the same key. */
  youtubeApiKey: process.env.YOUTUBE_API_KEY ?? '',
} as const;

/**
 * Some frontend code calls the backend on a hardcoded `http://localhost:8080` instead of
 * `REACT_APP_API_URL`. When the stack runs on non-default ports we transparently re-route
 * those requests to API_URL so the tests exercise the app, not the environment.
 */
export const HARDCODED_API_ORIGIN = 'http://localhost:8080';

export const hasRealYoutubeKey = (): boolean =>
  env.youtubeApiKey.length > 0 && !/^YOUR_|dummy|changeme/i.test(env.youtubeApiKey);

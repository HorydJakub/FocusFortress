import { FocusFortressApi } from '../utils/api-client';
import { env } from '../utils/env';
import type { MailHog } from '../utils/mailhog';
import { buildUser, type NewUser } from '../utils/test-data';
import { test as base } from './base';

export interface TestUser extends NewUser {
  token: string;
}

/**
 * Registers a brand-new user through the API, confirms the address using the token from
 * the real verification email in MailHog, and logs in. `api` stays authenticated as that user.
 */
export async function createVerifiedUser(
  api: FocusFortressApi,
  mailhog: MailHog,
  overrides: Partial<NewUser> = {},
): Promise<TestUser> {
  const interests = (await api.interestOptions()).slice(0, 3).map((i) => i.name);
  const user = buildUser({ selectedInterests: interests, ...overrides });

  await api.register(user);
  await api.verify(await mailhog.waitForVerificationToken(user.email));
  const token = await api.login(user.email, user.password);

  return { ...user, token };
}

/**
 * Logged-in fixtures. The session is injected exactly the way the frontend persists a
 * "Remember me" login (localStorage: token, user, rememberMe), so tests skip the login UI.
 */
export const test = base.extend<{
  user: TestUser;
  /** A second, unrelated account (API only) for data-isolation checks. */
  otherUser: { user: TestUser; api: FocusFortressApi };
}>({
  user: async ({ api, mailhog }, use) => {
    await use(await createVerifiedUser(api, mailhog));
  },

  otherUser: async ({ playwright, mailhog }, use) => {
    const request = await playwright.request.newContext({ baseURL: `${env.apiUrl}/` });
    const api = new FocusFortressApi(request);
    await use({ user: await createVerifiedUser(api, mailhog), api });
    await request.dispose();
  },

  storageState: async ({ user }, use) => {
    await use({
      cookies: [],
      origins: [
        {
          origin: env.baseUrl,
          localStorage: [
            { name: 'token', value: user.token },
            { name: 'user', value: user.email },
            { name: 'rememberMe', value: 'true' },
          ],
        },
      ],
    });
  },
});

export { expect } from '@playwright/test';

import { test as base } from '@playwright/test';
import { AuthPage } from '../pages/AuthPage';
import { CountersPage } from '../pages/CountersPage';
import { DashboardPage } from '../pages/DashboardPage';
import { HabitsPage } from '../pages/HabitsPage';
import { NavBar } from '../pages/components/NavBar';
import { SettingsModal } from '../pages/components/SettingsModal';
import { FocusFortressApi } from '../utils/api-client';
import { env, HARDCODED_API_ORIGIN } from '../utils/env';
import { MailHog } from '../utils/mailhog';

export interface BaseFixtures {
  /** REST client for test setup. Becomes authenticated once `login()` is called. */
  api: FocusFortressApi;
  mailhog: MailHog;
  authPage: AuthPage;
  navBar: NavBar;
  dashboardPage: DashboardPage;
  habitsPage: HabitsPage;
  countersPage: CountersPage;
  settings: SettingsModal;
}

/** Anonymous-visitor fixtures: page objects plus API/MailHog clients, no session. */
export const test = base.extend<BaseFixtures>({
  context: async ({ context }, use) => {
    const apiOrigin = new URL(env.apiUrl).origin;
    if (apiOrigin !== HARDCODED_API_ORIGIN) {
      await context.route(`${HARDCODED_API_ORIGIN}/api/**`, (route) =>
        route.continue({ url: route.request().url().replace(HARDCODED_API_ORIGIN, apiOrigin) }),
      );
    }
    await use(context);
  },

  api: async ({ playwright }, use) => {
    const request = await playwright.request.newContext({ baseURL: `${env.apiUrl}/` });
    await use(new FocusFortressApi(request));
    await request.dispose();
  },

  mailhog: async ({ request }, use) => {
    await use(new MailHog(request, env.mailhogUrl));
  },

  authPage: async ({ page }, use) => use(new AuthPage(page)),
  navBar: async ({ page }, use) => use(new NavBar(page)),
  dashboardPage: async ({ page }, use) => use(new DashboardPage(page)),
  habitsPage: async ({ page }, use) => use(new HabitsPage(page)),
  countersPage: async ({ page }, use) => use(new CountersPage(page)),
  settings: async ({ page }, use) => use(new SettingsModal(page)),
});

export { expect } from '@playwright/test';

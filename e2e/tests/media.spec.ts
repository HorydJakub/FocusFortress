import { expect, test } from '../fixtures/auth';
import { hasRealYoutubeKey } from '../utils/env';

test.describe('My Media', () => {
  test('page opens with an empty library for a new user', async ({ page, navBar, dashboardPage }) => {
    await dashboardPage.goto();
    await navBar.goTo('My Media');

    await expect(page).toHaveURL(/\/media$/);
    await expect(page.getByRole('heading', { level: 1, name: 'My Media' })).toBeVisible();
    await expect(page.getByRole('heading', { name: "It's a bit empty here..." })).toBeVisible();
  });

  test('shows YouTube recommendations for the user interests', { tag: '@external' }, async ({ page }) => {
    test.skip(!hasRealYoutubeKey(), 'needs a real YouTube Data API key (YOUTUBE_API_KEY) configured in the backend');

    await page.goto('/media');
    await expect(page.getByRole('img').first()).toBeVisible({ timeout: 20_000 });
    await expect(page.getByRole('button', { name: /Watch Later/ }).first()).toBeVisible();
  });
});

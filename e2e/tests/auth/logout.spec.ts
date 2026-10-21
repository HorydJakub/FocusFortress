import { expect, test } from '../../fixtures/auth';

test('logout clears the session and protects the app again', async ({ page, dashboardPage, navBar, authPage }) => {
  await dashboardPage.goto();

  await navBar.logout();

  await expect(page).toHaveURL(/\/login$/);
  await expect(authPage.signInHeading).toBeVisible();
  expect(await page.evaluate(() => localStorage.getItem('token'))).toBeNull();

  await page.goto('/dashboard');
  await expect(page).toHaveURL(/\/login$/);
});

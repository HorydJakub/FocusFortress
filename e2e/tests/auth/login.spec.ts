import { createVerifiedUser } from '../../fixtures/auth';
import { expect, test } from '../../fixtures/base';
import { buildUser, uniqueEmail, VALID_PASSWORD } from '../../utils/test-data';

test.describe('Login', () => {
  test('verified user signs in and lands on the dashboard', async ({ api, mailhog, authPage, dashboardPage, page }) => {
    const user = await createVerifiedUser(api, mailhog);

    await authPage.goto();
    await authPage.login(user.email, user.password);

    await dashboardPage.expectLoaded();
    // Without "Remember me" the session lives only for the browser tab.
    expect(await page.evaluate(() => sessionStorage.getItem('token'))).toBeTruthy();
    expect(await page.evaluate(() => localStorage.getItem('token'))).toBeNull();
  });

  test('"Remember me" persists the session in localStorage', async ({ api, mailhog, authPage, dashboardPage, page }) => {
    const user = await createVerifiedUser(api, mailhog);

    await authPage.goto();
    await authPage.login(user.email, user.password, { rememberMe: true });

    await dashboardPage.expectLoaded();
    expect(await page.evaluate(() => localStorage.getItem('token'))).toBeTruthy();
    expect(await page.evaluate(() => localStorage.getItem('rememberMe'))).toBe('true');
  });

  test('validates email and password on the client', async ({ authPage, page }) => {
    const loginRequests: string[] = [];
    page.on('request', (r) => r.url().includes('/auth/login') && loginRequests.push(r.url()));
    await authPage.goto();

    await authPage.submitButton.click();
    await expect(authPage.fieldError('Valid email is required')).toBeVisible();
    await expect(authPage.fieldError('Password must be at least 8 characters')).toBeVisible();

    await authPage.login(uniqueEmail(), 'short');
    await expect(authPage.fieldError('Valid email is required')).toBeHidden();
    await expect(authPage.fieldError('Password must be at least 8 characters')).toBeVisible();
    expect(loginRequests).toHaveLength(0);
  });

  test('unverified user is told to verify the email first', async ({ api, authPage }) => {
    const interests = (await api.interestOptions()).slice(0, 3).map((i) => i.name);
    const user = buildUser({ selectedInterests: interests });
    await api.register(user);

    await authPage.goto();
    await authPage.login(user.email, user.password);

    await expect(authPage.message(/verify your email/i)).toBeVisible();
    await expect(authPage.signInHeading).toBeVisible();
  });

  test('wrong password shows an error message', async ({ api, mailhog, authPage, page }) => {

    const user = await createVerifiedUser(api, mailhog);
    await authPage.goto();
    await authPage.login(user.email, `${VALID_PASSWORD}x`);

    await expect(authPage.message('Invalid email or password')).toBeVisible({ timeout: 5_000 });
    await expect(page).toHaveURL(/\/login$/);
  });

  test('protected pages redirect anonymous visitors to the login page', async ({ page, authPage }) => {
    for (const path of ['/dashboard', '/habits', '/counters']) {
      await page.goto(path);
      await expect(page).toHaveURL(/\/login$/);
      await expect(authPage.signInHeading).toBeVisible();
    }
  });
});

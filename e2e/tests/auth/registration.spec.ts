import { expect, test } from '../../fixtures/base';
import { buildUser } from '../../utils/test-data';

test.describe('Registration', () => {
  test('new user registers with interests, verifies email and signs in for the first time', async ({
    page,
    api,
    authPage,
    mailhog,
    dashboardPage,
  }) => {
    const interests = (await api.interestOptions()).slice(0, 3).map((i) => i.name);
    const user = buildUser({ selectedInterests: interests, dateOfBirth: '1995-04-12', gender: 'Female' });

    await test.step('fill in the sign-up form and pick interests', async () => {
      await authPage.gotoRegistration();
      await expect(authPage.interest(interests[0])).toBeVisible();
      await authPage.fillRegistration(user);
      await expect(authPage.interestsCounter).toHaveText('3 / 7');
      await authPage.submitButton.click();
      await expect(authPage.message('Registration successful!')).toBeVisible();
    });

    await test.step('open the verification link from the email', async () => {
      const email = await mailhog.waitForEmailTo(user.email);
      expect(email.subject).toBe('Verify your FocusFortress account');
      const link = email.body.match(/https?:\/\/[^"'\s<>]+\/api\/auth\/verify\?token=[\w-]+/)?.[0];
      expect(link, 'verification link in email body').toBeTruthy();

      await page.goto(link!);
      await expect(page.locator('body')).toHaveText('Account verified successfully!');
    });

    await test.step('sign in with the new credentials', async () => {
      await authPage.goto();
      await authPage.login(user.email, user.password);
      await dashboardPage.expectLoaded();
      await expect(dashboardPage.welcome).toHaveText(`Welcome back, ${user.email.split('@')[0]}!`);
      await expect(dashboardPage.emptyStateHint).toBeVisible();
    });
  });

  test('date of birth marked "(Optional)" can be left empty', async ({ api, authPage }) => {
    test.info().annotations.push({
      type: 'issue',
      description:
        'BUG: the form labels Date of Birth "(Optional)" and sends null, but UserRegistrationDTO has ' +
        '@NotNull on dateOfBirth, so the backend answers 400 and the UI only shows "Registration failed".',
    });
    test.fail();

    const interests = (await api.interestOptions()).slice(0, 3).map((i) => i.name);
    await authPage.gotoRegistration();
    await authPage.register(buildUser({ dateOfBirth: undefined, selectedInterests: interests }));

    await expect(authPage.message('Registration successful!')).toBeVisible({ timeout: 5_000 });
  });

  test('requires at least three interests', async ({ api, authPage }) => {
    const [first, second] = (await api.interestOptions()).map((i) => i.name);
    await authPage.gotoRegistration();
    await authPage.register(buildUser({ selectedInterests: [first, second] }));

    await expect(authPage.fieldError('⚠️ Please select at least 3 interests')).toBeVisible();
    await expect(authPage.createAccountHeading).toBeVisible();
  });

  test('blocks selecting more than seven interests', async ({ api, authPage }) => {
    const options = (await api.interestOptions()).map((i) => i.name);
    test.skip(options.length < 8, 'needs at least 8 predefined interests');

    await authPage.gotoRegistration();
    for (const name of options.slice(0, 8)) await authPage.interest(name).click();

    await expect(authPage.interestsCounter).toHaveText('7 / 7');
    await expect(authPage.fieldError('⚠️ Maximum 7 interests allowed')).toBeVisible();
  });

  test('rejects mismatching password confirmation', async ({ api, authPage }) => {
    const interests = (await api.interestOptions()).slice(0, 3).map((i) => i.name);
    await authPage.gotoRegistration();
    await authPage.fillRegistration(buildUser({ selectedInterests: interests }));
    await authPage.confirmPasswordInput.fill('Different123');
    await authPage.submitButton.click();

    await expect(authPage.fieldError('Passwords do not match')).toBeVisible();
  });

  test('shows the server error when the email is already registered', async ({ api, authPage }) => {
    const interests = (await api.interestOptions()).slice(0, 3).map((i) => i.name);
    const existing = buildUser({ selectedInterests: interests });
    await api.register(existing);

    await authPage.gotoRegistration();
    await authPage.register(buildUser({ email: existing.email, selectedInterests: interests }));

    await expect(authPage.message('Email already exists!')).toBeVisible();
  });
});

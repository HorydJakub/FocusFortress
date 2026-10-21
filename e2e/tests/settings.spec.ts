import { expect, test } from '../fixtures/auth';

test.describe('Settings', () => {
  test.beforeEach(async ({ dashboardPage, navBar, settings }) => {
    await dashboardPage.goto();
    await navBar.openSettings();
    await settings.expectOpen();
  });

  test('profile tab shows the current profile with a read-only email', async ({ settings, user }) => {
    await expect(settings.nameInput).toHaveValue(user.name);
    await expect(settings.emailInput).toHaveValue(user.email);
    await expect(settings.emailInput).not.toBeEditable();
  });

  test('user updates name, date of birth and gender', async ({ page, api, settings, navBar }) => {
    await settings.nameInput.fill('Renamed Tester');
    await settings.dateOfBirthInput.fill('1990-01-31');
    await settings.genderSelect.selectOption('Prefer not to specify');
    await settings.saveProfileButton.click();

    await expect(settings.message('Profile updated successfully!')).toBeVisible();
    await expect(navBar.userMenu).toContainText('Renamed Tester');

    await page.reload();
    await navBar.openSettings();
    await expect(settings.nameInput).toHaveValue('Renamed Tester');
    await expect(settings.dateOfBirthInput).toHaveValue('1990-01-31');
    await expect(settings.genderSelect).toHaveValue('Prefer not to specify');
    expect(await api.profile()).toMatchObject({
      name: 'Renamed Tester',
      dateOfBirth: '1990-01-31',
      gender: 'Prefer not to specify',
    });
  });

  test('rejects a too-short name', async ({ settings }) => {
    await settings.nameInput.fill('A');
    await settings.saveProfileButton.click();

    await expect(settings.root.getByText('Name must be at least 2 characters')).toBeVisible();
  });

  test('user changes interests', async ({ api, settings, navBar, user }) => {
    const [, , removed] = user.selectedInterests;
    const added = (await api.interestOptions()).map((i) => i.name).find((n) => !user.selectedInterests.includes(n))!;

    await settings.openTab('Interests');
    await expect(settings.interestsTotal).toHaveText('Total Interests: 3 / 7');
    await settings.interest(removed).click();
    await settings.interest(added).click();
    await settings.saveInterestsButton.click();
    await expect(settings.message('Interests updated successfully!')).toBeVisible();

    await settings.close();
    await navBar.openSettings();
    await settings.openTab('Interests');
    await expect(settings.interest(added)).toContainText('✓');
    await expect(settings.interest(removed)).not.toContainText('✓');
  });
});

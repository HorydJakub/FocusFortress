import { expect, test } from '../fixtures/auth';
import { uniqueName } from '../utils/test-data';

test.describe('Dashboard', () => {
  test('greets a new user with empty stats', async ({ dashboardPage, navBar, user }) => {
    await dashboardPage.goto();

    await expect(dashboardPage.welcome).toHaveText(`Welcome back, ${user.email.split('@')[0]}!`);
    await expect(navBar.userMenu).toContainText(user.name);
    await expect(dashboardPage.statValue('Active Habits')).toHaveText('0');
    await expect(dashboardPage.statValue('Counters')).toHaveText('0');
    await expect(dashboardPage.statValue('Completed Habits')).toHaveText('0');
    await expect(dashboardPage.statValue('Best Streak')).toHaveText('0 days');
    await expect(dashboardPage.emptyStateHint).toBeVisible();
  });

  test('reflects habits and counters created by the user', async ({ api, dashboardPage }) => {
    const category = await api.createCategory(uniqueName('Cat'));
    const subcategory = await api.createSubcategory(category.id, uniqueName('Sub'));
    const habit = await api.createHabit({
      name: uniqueName('Read'),
      categoryId: category.id,
      subcategoryId: subcategory.id,
      durationDays: 10,
    });
    await api.markHabitDone(habit.id);
    await api.createCounter({ name: uniqueName('Sugar') });

    await dashboardPage.goto();

    await expect(dashboardPage.statValue('Active Habits')).toHaveText('1');
    await expect(dashboardPage.statValue('Counters')).toHaveText('1');
    await expect(dashboardPage.statValue('Best Streak')).toHaveText('1 day');
    await expect(dashboardPage.emptyStateHint).toBeHidden();
  });

  test('navigation bar reaches every section', async ({ page, dashboardPage, navBar, habitsPage, countersPage }) => {
    await dashboardPage.goto();

    await navBar.goTo('Habits');
    await expect(page).toHaveURL(/\/habits$/);
    await expect(habitsPage.heading).toHaveText('My Habits');

    await navBar.goTo('Counters');
    await expect(page).toHaveURL(/\/counters$/);
    await expect(countersPage.heading).toBeVisible();

    await navBar.goTo('Dashboard');
    await dashboardPage.expectLoaded();
  });
});

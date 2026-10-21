import { expect, test } from '../fixtures/auth';
import { uniqueName } from '../utils/test-data';

test.describe('Data isolation between accounts', () => {
  test("another user's habits and counters are not visible", async ({ otherUser, habitsPage, countersPage }) => {
    const category = await otherUser.api.createCategory(uniqueName('Theirs'));
    const subcategory = await otherUser.api.createSubcategory(category.id, uniqueName('Theirs'));
    const habitName = uniqueName('Their habit');
    const counterName = uniqueName('Their counter');
    await otherUser.api.createHabit({
      name: habitName,
      categoryId: category.id,
      subcategoryId: subcategory.id,
      durationDays: 5,
    });
    await otherUser.api.createCounter({ name: counterName });

    await habitsPage.goto();
    await expect(habitsPage.page.getByRole('heading', { name: "It's a bit empty here!" })).toBeVisible();
    await expect(habitsPage.page.getByText(habitName)).toHaveCount(0);

    await countersPage.goto();
    await expect(countersPage.emptyState).toBeVisible();
    await expect(countersPage.page.getByText(counterName)).toHaveCount(0);
  });

  test("another user's custom categories are not visible", async ({ page, otherUser, habitsPage }) => {

    const theirCategory = uniqueName('Their category');
    await otherUser.api.createCategory(theirCategory);

    await habitsPage.goto();
    await habitsPage.manageCategoriesButton.click();
    await expect(habitsPage.heading).toHaveText('Manage Categories');
    // Wait for the grid to render (the user's own interest-based categories) before asserting absence.
    await expect(page.getByRole('heading', { level: 3 }).first()).toBeVisible();

    await expect(page.getByRole('heading', { level: 3, name: theirCategory })).toHaveCount(0, { timeout: 5_000 });
  });
});

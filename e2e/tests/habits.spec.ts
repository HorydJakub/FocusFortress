import { expect, test } from '../fixtures/auth';
import type { Category, Subcategory } from '../utils/api-client';
import { uniqueName } from '../utils/test-data';

test.describe('Habits', () => {
  let category: Category;
  let subcategory: Subcategory;

  // Categories are shared app-wide, so each test gets its own uniquely named pair.
  test.beforeEach(async ({ api }) => {
    category = await api.createCategory(uniqueName('Health'));
    subcategory = await api.createSubcategory(category.id, uniqueName('Gym'));
  });

  test('user creates a habit and sees it in progress', async ({ habitsPage }) => {
    const name = uniqueName('Morning run');

    await habitsPage.goto();
    await habitsPage.createHabit({
      name,
      description: 'Run 5 km before breakfast',
      categoryId: category.id,
      subcategoryId: subcategory.id,
      durationDays: 30,
    });

    const row = habitsPage.row(name);
    await expect(row).toBeVisible();
    await expect(row).toContainText(category.name);
    await expect(row).toContainText(subcategory.name);
    await expect(habitsPage.streak(name)).toHaveText('0 days');
    await expect(habitsPage.progress(name)).toHaveText('0%');
  });

  test('user edits a habit', async ({ api, habitsPage }) => {
    const original = uniqueName('Stretch');
    const renamed = uniqueName('Yoga');
    await api.createHabit({ name: original, categoryId: category.id, subcategoryId: subcategory.id, durationDays: 14 });

    await habitsPage.goto();
    const modal = await habitsPage.openEdit(original);
    await expect(modal.nameInput).toHaveValue(original);
    await modal.fill({ name: renamed, durationDays: 21 });
    await modal.saveButton.click();
    await expect(modal.root).toBeHidden();

    await expect(habitsPage.row(renamed)).toContainText('0/21 days completed');
    await expect(habitsPage.page.getByText(original, { exact: true })).toHaveCount(0);
  });

  test('marking a habit done updates streak and progress', async ({ api, habitsPage }) => {
    const name = uniqueName('Meditate');
    await api.createHabit({ name, categoryId: category.id, subcategoryId: subcategory.id, durationDays: 4 });

    await habitsPage.goto();
    await habitsPage.markDone(name);

    await expect(habitsPage.markDoneButton(name)).toHaveText('Done!');
    await expect(habitsPage.markDoneButton(name)).toBeDisabled();
    await expect(habitsPage.streak(name)).toHaveText('1 day');
    await expect(habitsPage.progress(name)).toHaveText('25%');
    await expect(habitsPage.row(name)).toContainText('1/4 days completed');
  });

  test('completing the final day celebrates and moves the habit to Completed', async ({
    api,
    habitsPage,
    dashboardPage,
  }) => {
    const name = uniqueName('Cold shower');
    await api.createHabit({ name, categoryId: category.id, subcategoryId: subcategory.id, durationDays: 1 });

    await habitsPage.goto();
    await expect(habitsPage.inProgressSection).toContainText('1');
    await habitsPage.markDone(name);

    await expect(habitsPage.celebration).toBeVisible();
    await expect(habitsPage.page.getByText(`You've completed "${name}"!`)).toBeVisible();
    await expect(habitsPage.completedSection).toBeVisible();
    await expect(habitsPage.inProgressSection).toBeHidden();
    await expect(habitsPage.progress(name)).toHaveText('100%');

    await dashboardPage.goto();
    await expect(dashboardPage.statValue('Completed Habits')).toHaveText('1');
    await expect(dashboardPage.statValue('Active Habits')).toHaveText('0');
  });

  test('user builds a category tree in the UI and adds a habit to it', async ({ page, habitsPage }) => {
    const categoryName = uniqueName('Mind');
    const subcategoryName = uniqueName('Reading');
    const habitName = uniqueName('Read 20 pages');

    await habitsPage.goto();
    await habitsPage.manageCategoriesButton.click();
    await expect(habitsPage.heading).toHaveText('Manage Categories');

    await page.getByRole('button', { name: 'Add Category' }).click();
    await page.getByPlaceholder('e.g. Health, Productivity').fill(categoryName);
    await page.getByRole('button', { name: 'Create Category' }).click();
    await page.getByRole('heading', { level: 3, name: categoryName }).click();
    await expect(habitsPage.heading).toHaveText(categoryName);

    await page.getByRole('button', { name: 'Add Subcategory' }).first().click();
    await page.getByPlaceholder('e.g. Gym, Meditation').fill(subcategoryName);
    await page.getByRole('button', { name: 'Create Subcategory' }).last().click();
    await page.getByRole('heading', { level: 3, name: subcategoryName }).click();
    await expect(habitsPage.heading).toHaveText(subcategoryName);

    // Adding from inside a subcategory pre-selects category and subcategory.
    await habitsPage.addHabitButton.click();
    await habitsPage.modal.fill({ name: habitName, durationDays: 7 });
    await habitsPage.modal.createButton.click();
    await expect(page.getByRole('heading', { level: 3, name: habitName })).toBeVisible();

    await page.getByText('My Habits', { exact: true }).click();
    await expect(habitsPage.row(habitName)).toContainText(categoryName);
  });
});

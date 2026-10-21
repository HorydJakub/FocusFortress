import { expect, test } from '../fixtures/auth';
import { uniqueName } from '../utils/test-data';

test.describe('Counters', () => {
  test('new user sees the empty state', async ({ countersPage }) => {
    await countersPage.goto();
    await expect(countersPage.emptyState).toBeVisible();
  });

  test('user creates a custom counter and the timer starts ticking', async ({ countersPage }) => {
    const name = uniqueName('Sugar');

    await countersPage.goto();
    await countersPage.createCustomCounter({ name, description: 'No sweets after 6pm' });

    const card = countersPage.card(name);
    await expect(card).toBeVisible();
    await expect(card).toContainText(`without: ${name}`);
    // Don't assert absolute values: server and browser clocks/time zones may differ.
    const timer = countersPage.timer(name);
    const first = await timer.textContent();
    await expect(timer).not.toHaveText(first!, { timeout: 3_000 });
  });

  test('user creates a counter from the defaults list', async ({ countersPage }) => {
    await countersPage.goto();
    await countersPage.createDefaultCounter('Cigarettes');

    await expect(countersPage.card('Cigarettes')).toContainText('🚬');
  });

  test('user edits a counter', async ({ api, countersPage }) => {
    const original = uniqueName('Coffee');
    const renamed = uniqueName('Energy drinks');
    await api.createCounter({ name: original, description: 'old description' });

    await countersPage.goto();
    const modal = await countersPage.openEdit(original);
    await expect(modal.nameInput).toHaveValue(original);
    await modal.nameInput.fill(renamed);
    await modal.descriptionInput.fill('new description');
    await modal.saveButton.click();
    await expect(modal.root).toBeHidden();

    await expect(countersPage.card(renamed)).toBeVisible();
    await expect(countersPage.card(original)).toHaveCount(0);
    expect((await api.counters()).map((c) => c.description)).toEqual(['new description']);
  });

  test('user deletes a counter after confirming', async ({ api, countersPage }) => {
    const name = uniqueName('Doomscrolling');
    await api.createCounter({ name });

    await countersPage.goto();
    await countersPage.delete(name);

    await expect(countersPage.card(name)).toHaveCount(0);
    await expect(countersPage.emptyState).toBeVisible();
  });

  test.describe('in a non-UTC time zone', () => {
    test.use({ timezoneId: 'Asia/Bangkok' });

    test('a brand-new counter starts from zero', async ({ countersPage }) => {

      const name = uniqueName('Fresh start');
      await countersPage.goto();
      await countersPage.createCustomCounter({ name });

      const timer = countersPage.card(name).getByText(/::/).first();
      // The first render is a static 00::00::00::00 placeholder – wait for the first real tick.
      await expect(timer).not.toHaveText('00::00::00::00');
      await expect(timer).toHaveText(/^00::00::00::\d{2}$/, { timeout: 2_000 });
    });
  });
});

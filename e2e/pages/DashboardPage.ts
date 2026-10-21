import { expect, type Locator, type Page } from '@playwright/test';

export type StatTitle = 'Active Habits' | 'Counters' | 'Completed Habits' | 'Best Streak';

export class DashboardPage {
  readonly welcome: Locator;
  readonly emptyStateHint: Locator;

  constructor(readonly page: Page) {
    this.welcome = page.getByText(/^Welcome back, .+!$/);
    this.emptyStateHint = page.getByText(/Start your journey today!/);
  }

  async goto(): Promise<void> {
    await this.page.goto('/dashboard');
    await this.expectLoaded();
  }

  async expectLoaded(): Promise<void> {
    await expect(this.page).toHaveURL(/\/dashboard$/);
    await expect(this.welcome).toBeVisible();
  }

  /** The big number on a stat tile, e.g. "2" or "0 days". */
  statValue(title: StatTitle): Locator {
    // Tile layout: icon, value, title, subtitle – the value is the title's previous sibling.
    return this.page
      .getByText(title, { exact: true })
      .and(this.page.locator(':not(nav *)'))
      .locator('xpath=preceding-sibling::div[1]');
  }
}

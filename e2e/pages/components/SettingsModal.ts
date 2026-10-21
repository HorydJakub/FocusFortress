import { expect, type Locator, type Page } from '@playwright/test';

export type SettingsTab = 'Profile' | 'Interests' | 'Custom Interests' | 'Report';

/** Settings modal opened from the user button in the nav bar. */
export class SettingsModal {
  readonly root: Locator;
  readonly heading: Locator;
  readonly nameInput: Locator;
  readonly emailInput: Locator;
  readonly dateOfBirthInput: Locator;
  readonly genderSelect: Locator;
  readonly saveProfileButton: Locator;
  readonly saveInterestsButton: Locator;
  readonly interestsTotal: Locator;

  constructor(readonly page: Page) {
    this.heading = page.getByRole('heading', { name: 'Settings', exact: true });
    // The modal card is the innermost element holding both the heading and the tab bar.
    this.root = page
      .locator('div')
      .filter({ has: this.heading })
      .filter({ has: page.getByRole('button', { name: 'Custom Interests' }) })
      .last();
    this.nameInput = this.root.locator('input[name="name"]');
    this.emailInput = this.root.locator('input[name="email"]');
    this.dateOfBirthInput = this.root.locator('input[name="dateOfBirth"]');
    this.genderSelect = this.root.locator('select[name="gender"]');
    this.saveProfileButton = this.root.getByRole('button', { name: 'Save Changes' });
    this.saveInterestsButton = this.root.getByRole('button', { name: 'Save Interests' });
    this.interestsTotal = this.root.getByText(/^Total Interests: \d+ \/ 7$/);
  }

  /** Waits until the modal is open and the profile has been fetched (otherwise the fetch overwrites edits). */
  async expectOpen(): Promise<void> {
    await expect(this.heading).toBeVisible();
    await expect(this.emailInput).toHaveValue(/@/);
  }

  async openTab(tab: SettingsTab): Promise<void> {
    await this.root.getByRole('button', { name: tab, exact: true }).click();
  }

  message(text: string | RegExp): Locator {
    return this.root.locator('span').filter({ hasText: text });
  }

  interest(name: string): Locator {
    return this.root.getByRole('button').filter({ has: this.page.getByText(name, { exact: true }) });
  }

  async close(): Promise<void> {
    // The close (X) button is the only icon-only button in the header, right after the heading.
    await this.heading.locator('xpath=following-sibling::button[1]').click();
    await expect(this.heading).toBeHidden();
  }
}

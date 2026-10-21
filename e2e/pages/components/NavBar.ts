import type { Locator, Page } from '@playwright/test';

export type Section = 'Dashboard' | 'Habits' | 'Counters' | 'My Media';

/** Top navigation bar rendered by `Layout` on every authenticated page. */
export class NavBar {
  readonly root: Locator;
  readonly logoutButton: Locator;
  /** The avatar + display-name button that opens Settings. */
  readonly userMenu: Locator;

  constructor(readonly page: Page) {
    this.root = page.locator('nav');
    this.logoutButton = this.root.getByRole('button', { name: 'Logout' });
    this.userMenu = this.root
      .getByRole('button')
      .filter({ hasNotText: /^(Dashboard|Habits|Counters|My Media|Logout)$/ });
  }

  link(section: Section): Locator {
    return this.root.getByRole('button', { name: section, exact: true });
  }

  async goTo(section: Section): Promise<void> {
    await this.link(section).click();
  }

  async openSettings(): Promise<void> {
    await this.userMenu.click();
  }

  async logout(): Promise<void> {
    await this.logoutButton.click();
  }
}

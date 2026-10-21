import { expect, type Locator, type Page } from '@playwright/test';

/** "Add New Counter" / "Edit Counter" modal. */
export class CounterFormModal {
  readonly root: Locator;
  readonly useDefaultsCheckbox: Locator;
  readonly defaultsSelect: Locator;
  readonly nameInput: Locator;
  readonly descriptionInput: Locator;
  readonly createButton: Locator;
  readonly saveButton: Locator;

  constructor(readonly page: Page) {
    this.root = page
      .locator('div')
      .filter({ has: page.getByRole('heading', { name: /^(Add New Counter|Edit Counter)$/ }) })
      .filter({ has: page.getByRole('button', { name: 'Cancel' }) })
      .last();
    this.useDefaultsCheckbox = this.root.getByLabel('Select from defaults');
    this.defaultsSelect = this.root.getByRole('combobox');
    this.nameInput = this.root.getByPlaceholder('Type the name of the counter to track');
    this.descriptionInput = this.root.getByPlaceholder('Add a description...');
    this.createButton = this.root.getByRole('button', { name: 'Create Counter' });
    this.saveButton = this.root.getByRole('button', { name: 'Save Changes' });
  }
}

/** `/counters` – "time since I quit X" timers. */
export class CountersPage {
  readonly heading: Locator;
  readonly addCounterButton: Locator;
  readonly emptyState: Locator;
  readonly modal: CounterFormModal;

  constructor(readonly page: Page) {
    this.heading = page.getByRole('heading', { level: 1, name: 'Counters' });
    this.addCounterButton = page.getByRole('button', { name: 'Add Counter' });
    this.emptyState = page.getByRole('heading', { name: 'No counters yet' });
    this.modal = new CounterFormModal(page);
  }

  async goto(): Promise<void> {
    await this.page.goto('/counters');
    await expect(this.heading).toBeVisible();
  }

  card(name: string): Locator {
    return this.page
      .locator('div')
      .filter({ has: this.page.getByRole('heading', { level: 3, name, exact: true }) })
      .filter({ has: this.page.getByRole('button', { name: 'Reset' }) })
      .last();
  }

  /** Elapsed time rendered as DD::HH::MM::SS. */
  timer(name: string): Locator {
    return this.card(name).getByText(/^\d{2}::\d{2}::\d{2}::\d{2}$/);
  }

  async createCustomCounter({ name, description }: { name: string; description?: string }): Promise<void> {
    await this.addCounterButton.click();
    await this.modal.useDefaultsCheckbox.uncheck();
    await this.modal.nameInput.fill(name);
    if (description) await this.modal.descriptionInput.fill(description);
    await this.modal.createButton.click();
    await expect(this.modal.root).toBeHidden();
  }

  async createDefaultCounter(name: 'Vodka' | 'Cigarettes' | 'THC' | 'Negative Media'): Promise<void> {
    await this.addCounterButton.click();
    await expect(this.modal.useDefaultsCheckbox).toBeChecked();
    await this.modal.defaultsSelect.selectOption(name);
    await this.modal.createButton.click();
    await expect(this.modal.root).toBeHidden();
  }

  async openEdit(name: string): Promise<CounterFormModal> {
    await this.card(name).getByRole('button', { name: 'Edit' }).click();
    await expect(this.modal.root).toBeVisible();
    return this.modal;
  }

  /** Deletes a counter, accepting the native confirm() dialog. */
  async delete(name: string): Promise<void> {
    this.page.once('dialog', (dialog) => dialog.accept());
    await this.card(name).getByRole('button', { name: 'Delete' }).click();
  }
}

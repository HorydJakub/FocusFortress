import { expect, type Locator, type Page } from '@playwright/test';

export interface HabitForm {
  name?: string;
  description?: string;
  categoryId?: number;
  subcategoryId?: number;
  durationDays?: number;
}

/** "Add New Habit" / "Edit Habit" modal. */
export class HabitFormModal {
  readonly root: Locator;
  readonly nameInput: Locator;
  readonly descriptionInput: Locator;
  readonly categorySelect: Locator;
  readonly subcategorySelect: Locator;
  readonly durationInput: Locator;
  readonly createButton: Locator;
  readonly saveButton: Locator;

  constructor(readonly page: Page) {
    this.root = page
      .locator('div')
      .filter({ has: page.getByRole('heading', { name: /^(Add New Habit|Edit Habit)$/ }) })
      .filter({ has: page.getByRole('button', { name: 'Cancel' }) })
      .last();
    this.nameInput = this.root.getByPlaceholder('e.g. Go to the gym, Meditate, Read');
    this.descriptionInput = this.root.getByPlaceholder('Optional description...');
    this.categorySelect = this.root.getByRole('combobox').nth(0);
    this.subcategorySelect = this.root.getByRole('combobox').nth(1);
    this.durationInput = this.root.getByRole('spinbutton');
    this.createButton = this.root.getByRole('button', { name: 'Create Habit' });
    this.saveButton = this.root.getByRole('button', { name: 'Save Changes' });
  }

  async fill(form: HabitForm): Promise<void> {
    if (form.name !== undefined) await this.nameInput.fill(form.name);
    if (form.description !== undefined) await this.descriptionInput.fill(form.description);
    if (form.categoryId !== undefined) await this.categorySelect.selectOption(String(form.categoryId));
    if (form.subcategoryId !== undefined) {
      // Subcategories load asynchronously after a category is chosen.
      await expect(this.subcategorySelect).toBeEnabled();
      await this.subcategorySelect.selectOption(String(form.subcategoryId));
    }
    if (form.durationDays !== undefined) await this.durationInput.fill(String(form.durationDays));
  }
}

/** `/habits` – habit table plus category/subcategory management views. */
export class HabitsPage {
  readonly heading: Locator;
  readonly addHabitButton: Locator;
  readonly manageCategoriesButton: Locator;
  readonly inProgressSection: Locator;
  readonly completedSection: Locator;
  readonly celebration: Locator;
  readonly modal: HabitFormModal;

  constructor(readonly page: Page) {
    this.heading = page.getByRole('heading', { level: 1 });
    this.addHabitButton = page.getByRole('button', { name: 'Add Habit' }).first();
    this.manageCategoriesButton = page.getByRole('button', { name: 'Manage Categories' }).first();
    this.inProgressSection = page.getByRole('heading', { level: 2, name: /In Progress/ });
    this.completedSection = page.getByRole('heading', { level: 2, name: /Completed/ });
    this.celebration = page.getByRole('heading', { name: /Congratulations!/ });
    this.modal = new HabitFormModal(page);
  }

  async goto(): Promise<void> {
    await this.page.goto('/habits');
    await expect(this.heading).toHaveText('My Habits');
  }

  /** A row in the habits table (the smallest element holding both the name and the progress bar). */
  row(habitName: string): Locator {
    return this.page
      .locator('div')
      .filter({ has: this.page.getByText(habitName, { exact: true }) })
      .filter({ hasText: /\d+\/\d+ days completed/ })
      .last();
  }

  progress(habitName: string): Locator {
    return this.row(habitName).getByText(/^\d+%$/);
  }

  streak(habitName: string): Locator {
    return this.row(habitName).getByText(/^\d+ days?$/);
  }

  markDoneButton(habitName: string): Locator {
    return this.row(habitName).getByRole('button', { name: /Mark Done|Done!/ });
  }

  async createHabit(form: HabitForm): Promise<void> {
    await this.addHabitButton.click();
    await this.modal.fill(form);
    await this.modal.createButton.click();
    await expect(this.modal.root).toBeHidden();
  }

  async openEdit(habitName: string): Promise<HabitFormModal> {
    // The kebab (⋮) button has no accessible name; it is the last button in the row.
    await this.row(habitName).getByRole('button').last().click();
    await this.page.getByRole('button', { name: 'Edit', exact: true }).click();
    await expect(this.modal.root).toBeVisible();
    return this.modal;
  }

  async markDone(habitName: string): Promise<void> {
    await this.markDoneButton(habitName).click();
  }
}

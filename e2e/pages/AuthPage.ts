import { expect, type Locator, type Page } from '@playwright/test';
import type { NewUser } from '../utils/test-data';

/** `/login` – a single page that toggles between "Sign In" and "Create Account" modes. */
export class AuthPage {
  readonly signInHeading: Locator;
  readonly createAccountHeading: Locator;
  readonly form: Locator;
  readonly nameInput: Locator;
  readonly emailInput: Locator;
  readonly dateOfBirthInput: Locator;
  readonly genderSelect: Locator;
  readonly passwordInput: Locator;
  readonly confirmPasswordInput: Locator;
  readonly rememberMe: Locator;
  readonly submitButton: Locator;
  readonly switchToSignUp: Locator;
  readonly interestsCounter: Locator;

  constructor(readonly page: Page) {
    this.signInHeading = page.getByRole('heading', { name: 'Welcome Back!' });
    this.createAccountHeading = page.getByRole('heading', { name: 'Create Account' });
    this.form = page.locator('form');
    // Labels are not associated with inputs (no htmlFor), so placeholders/names are the stable hooks.
    this.nameInput = page.getByPlaceholder('Enter your name');
    this.emailInput = page.getByPlaceholder('Enter your email');
    this.dateOfBirthInput = this.form.locator('input[name="dateOfBirth"]');
    this.genderSelect = this.form.locator('select[name="gender"]');
    this.passwordInput = page.getByPlaceholder('Enter your password');
    this.confirmPasswordInput = page.getByPlaceholder('Confirm your password');
    this.rememberMe = page.getByText('Remember me on this device');
    this.submitButton = this.form.locator('button[type="submit"]');
    this.switchToSignUp = page.getByRole('button', { name: 'Sign Up', exact: true });
    this.interestsCounter = this.form.getByText(/^\d+ \/ 7$/);
  }

  async goto(): Promise<void> {
    await this.page.goto('/login');
    await expect(this.signInHeading).toBeVisible();
  }

  async gotoRegistration(): Promise<void> {
    await this.goto();
    await this.switchToSignUp.click();
    await expect(this.createAccountHeading).toBeVisible();
  }

  /** Banner shown above the form for both success and server errors. */
  message(text: string | RegExp): Locator {
    return this.page.locator('span').filter({ hasText: text });
  }

  /** Inline client-side validation message under a field. */
  fieldError(text: string): Locator {
    return this.form.getByText(text, { exact: true });
  }

  interest(name: string): Locator {
    return this.form.getByRole('button').filter({ has: this.page.getByText(name, { exact: true }) });
  }

  async login(email: string, password: string, { rememberMe = false } = {}): Promise<void> {
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
    if (rememberMe) await this.rememberMe.click();
    await this.submitButton.click();
  }

  async fillRegistration(user: NewUser): Promise<void> {
    await this.nameInput.fill(user.name);
    await this.emailInput.fill(user.email);
    if (user.dateOfBirth) await this.dateOfBirthInput.fill(user.dateOfBirth);
    if (user.gender) await this.genderSelect.selectOption(user.gender);
    await this.passwordInput.fill(user.password);
    await this.confirmPasswordInput.fill(user.password);
    for (const interest of user.selectedInterests) {
      await this.interest(interest).click();
    }
  }

  async register(user: NewUser): Promise<void> {
    await this.fillRegistration(user);
    await this.submitButton.click();
  }
}

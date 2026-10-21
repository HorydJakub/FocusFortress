import { expect, type APIRequestContext, type APIResponse } from '@playwright/test';
import type { NewUser } from './test-data';

export interface Category {
  id: number;
  name: string;
  icon: string;
}

export interface Subcategory extends Category {
  categoryId: number;
}

export interface Habit {
  id: number;
  name: string;
  description?: string;
  categoryId: number;
  subcategoryId: number;
  icon?: string;
  durationDays: number;
  currentStreak: number;
  done: boolean;
}

export interface Counter {
  id: number;
  name: string;
  description?: string;
  icon?: string;
  startDateTime: string;
}

export interface InterestOption {
  name: string;
  icon: string;
  categoryName: string;
}

/**
 * Typed wrapper around the FocusFortress REST API, used for fast test setup.
 * The request context is expected to have `baseURL` set to `${API_URL}/`.
 */
export class FocusFortressApi {
  private token?: string;

  constructor(private readonly request: APIRequestContext) {}

  private get authHeaders(): Record<string, string> {
    return this.token ? { Authorization: `Bearer ${this.token}` } : {};
  }

  private async ok<T>(response: APIResponse, action: string): Promise<T> {
    expect(response.ok(), `${action} failed: ${response.status()} ${await response.text()}`).toBeTruthy();
    const text = await response.text();
    try {
      return JSON.parse(text) as T;
    } catch {
      return text as unknown as T;
    }
  }

  // --- auth -------------------------------------------------------------------------------

  async interestOptions(): Promise<InterestOption[]> {
    return this.ok(await this.request.get('interests/subcategories'), 'GET interests/subcategories');
  }

  async register(user: NewUser): Promise<void> {
    await this.ok(
      await this.request.post('auth/register', {
        data: { ...user, confirmPassword: user.password, dateOfBirth: user.dateOfBirth ?? null, gender: user.gender ?? null },
      }),
      'POST auth/register',
    );
  }

  async verify(token: string): Promise<void> {
    await this.ok(await this.request.get('auth/verify', { params: { token } }), 'GET auth/verify');
  }

  /** Logs in and remembers the JWT for subsequent authenticated calls. */
  async login(email: string, password: string): Promise<string> {
    const body = await this.ok<{ accessToken: string }>(
      await this.request.post('auth/login', { data: { email, password, rememberMe: true } }),
      'POST auth/login',
    );
    this.token = body.accessToken;
    return body.accessToken;
  }

  // --- habits -----------------------------------------------------------------------------

  async createCategory(name: string, icon = '🧪'): Promise<Category> {
    return this.ok(
      await this.request.post('categories', { data: { name, icon }, headers: this.authHeaders }),
      'POST categories',
    );
  }

  async createSubcategory(categoryId: number, name: string, icon = '🔬'): Promise<Subcategory> {
    return this.ok(
      await this.request.post('subcategories', { data: { name, icon, categoryId }, headers: this.authHeaders }),
      'POST subcategories',
    );
  }

  async createHabit(habit: Omit<Habit, 'id' | 'currentStreak' | 'done'>): Promise<Habit> {
    return this.ok(await this.request.post('habits', { data: habit, headers: this.authHeaders }), 'POST habits');
  }

  async habits(): Promise<Habit[]> {
    return this.ok(await this.request.get('habits', { headers: this.authHeaders }), 'GET habits');
  }

  async markHabitDone(habitId: number): Promise<number> {
    return this.ok(await this.request.post(`habits/${habitId}/done`, { headers: this.authHeaders }), 'POST habits/done');
  }

  // --- counters ---------------------------------------------------------------------------

  async createCounter(counter: { name: string; description?: string; icon?: string }): Promise<Counter> {
    return this.ok(await this.request.post('counters', { data: counter, headers: this.authHeaders }), 'POST counters');
  }

  async counters(): Promise<Counter[]> {
    return this.ok(await this.request.get('counters', { headers: this.authHeaders }), 'GET counters');
  }

  // --- profile ----------------------------------------------------------------------------

  async profile(): Promise<{ name: string; email: string; dateOfBirth: string | null; gender: string | null }> {
    return this.ok(await this.request.get('user/profile', { headers: this.authHeaders }), 'GET user/profile');
  }
}

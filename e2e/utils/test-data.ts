import { randomUUID } from 'node:crypto';

/** Short, collision-free suffix suitable for emails and entity names. */
export const uniqueId = (): string => `${Date.now().toString(36)}${randomUUID().slice(0, 6)}`;

export const uniqueEmail = (prefix = 'e2e'): string => `${prefix}.${uniqueId()}@focusfortress.test`;

export const uniqueName = (prefix: string): string => `${prefix} ${uniqueId()}`;

/** Satisfies the backend policy: >= 8 chars, upper, lower and a digit. */
export const VALID_PASSWORD = 'Fortress123';

export interface NewUser {
  name: string;
  email: string;
  password: string;
  dateOfBirth?: string;
  gender?: 'Male' | 'Female' | 'Prefer not to specify';
  selectedInterests: string[];
}

export const buildUser = (overrides: Partial<NewUser> = {}): NewUser => ({
  name: 'E2E Tester',
  email: uniqueEmail(),
  password: VALID_PASSWORD,
  // The form labels it "(Optional)" but the backend rejects null – see registration.spec.ts.
  dateOfBirth: '1995-04-12',
  selectedInterests: [],
  ...overrides,
});

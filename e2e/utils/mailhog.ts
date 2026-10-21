import { expect, type APIRequestContext } from '@playwright/test';

interface MailHogMimePart {
  Headers: Record<string, string[]>;
  Body: string;
  MIME?: { Parts: MailHogMimePart[] } | null;
}

interface MailHogMessage {
  ID: string;
  Content: { Headers: Record<string, string[]>; Body: string };
  MIME?: { Parts: MailHogMimePart[] } | null;
}

interface MailHogSearchResponse {
  total: number;
  items: MailHogMessage[];
}

export interface Email {
  subject: string;
  /** Fully decoded body (quoted-printable / base64 resolved), all MIME parts concatenated. */
  body: string;
}

const header = (headers: Record<string, string[]>, name: string): string => {
  const key = Object.keys(headers).find((k) => k.toLowerCase() === name.toLowerCase());
  return key ? headers[key].join(' ') : '';
};

const decodeQuotedPrintable = (input: string): string => {
  const bytes = input
    .replace(/=\r?\n/g, '')
    .replace(/=([0-9A-F]{2})/gi, (_, hex: string) => String.fromCharCode(parseInt(hex, 16)));
  return Buffer.from(bytes, 'latin1').toString('utf8');
};

const decodePart = (headers: Record<string, string[]>, body: string): string => {
  const encoding = header(headers, 'Content-Transfer-Encoding').toLowerCase();
  if (encoding.includes('quoted-printable')) return decodeQuotedPrintable(body);
  if (encoding.includes('base64')) return Buffer.from(body.replace(/\s/g, ''), 'base64').toString('utf8');
  return body;
};

const flattenParts = (parts: MailHogMimePart[]): MailHogMimePart[] =>
  parts.flatMap((part) => [part, ...flattenParts(part.MIME?.Parts ?? [])]);

const decodeSubject = (raw: string): string =>
  // RFC 2047 encoded-words, e.g. =?UTF-8?Q?...?= or =?UTF-8?B?...?=
  raw.replace(/=\?([^?]+)\?([QB])\?([^?]*)\?=/gi, (_, _charset, kind: string, text: string) =>
    kind.toUpperCase() === 'B'
      ? Buffer.from(text, 'base64').toString('utf8')
      : decodeQuotedPrintable(text.replace(/_/g, ' ')),
  );

/** Thin client for the MailHog v2 HTTP API. */
export class MailHog {
  constructor(private readonly request: APIRequestContext, private readonly baseUrl: string) {}

  /** Polls until at least one email addressed to `recipient` arrives and returns the newest one. */
  async waitForEmailTo(recipient: string, { timeout = 20_000 } = {}): Promise<Email> {
    let latest: MailHogMessage | undefined;
    await expect
      .poll(
        async () => {
          const response = await this.request.get(`${this.baseUrl}/api/v2/search`, {
            params: { kind: 'to', query: recipient },
          });
          expect(response.ok(), `MailHog search failed: ${response.status()}`).toBeTruthy();
          const data = (await response.json()) as MailHogSearchResponse;
          latest = data.items[0];
          return data.total;
        },
        { message: `waiting for an email to ${recipient}`, timeout },
      )
      .toBeGreaterThan(0);

    const message = latest!;
    const parts = flattenParts(message.MIME?.Parts ?? []).filter((p) => !p.MIME?.Parts?.length);
    const body = parts.length
      ? parts.map((p) => decodePart(p.Headers, p.Body)).join('\n')
      : decodePart(message.Content.Headers, message.Content.Body);

    return { subject: decodeSubject(header(message.Content.Headers, 'Subject')), body };
  }

  async waitForVerificationToken(recipient: string): Promise<string> {
    const email = await this.waitForEmailTo(recipient);
    const token = email.body.match(/verify\?token=([\w-]+)/)?.[1];
    if (!token) throw new Error(`No verification token found in email to ${recipient}:\n${email.body}`);
    return token;
  }
}

# FocusFortress E2E tests

Browser end-to-end tests built with [Playwright Test](https://playwright.dev) and TypeScript. They drive real user journeys against the full docker-compose stack: React frontend, Spring Boot API, MySQL and MailHog.

## Running

```bash
# 1. From the repo root, start the stack. The backend needs both properties files;
#    dummy OAuth/YouTube keys are fine.
cp backend/src/main/resources/application.properties.example        backend/src/main/resources/application.properties
cp backend/src/main/resources/application-docker.properties.example backend/src/main/resources/application-docker.properties
docker compose up --build -d   # wait for :3000 and :8080/api/interests to answer 200

# 2. Run the suite
cd e2e
npm ci
npx playwright install --with-deps chromium
npm test                       # HTML report: npm run report
```

| Variable          | Default                     | Purpose                                         |
|-------------------|-----------------------------|-------------------------------------------------|
| `BASE_URL`        | `http://localhost:3000`     | Frontend                                        |
| `API_URL`         | `http://localhost:8080/api` | Backend, used for fast test setup               |
| `MAILHOG_URL`     | `http://localhost:8025`     | Read the verification emails                    |
| `YOUTUBE_API_KEY` | *(empty)*                   | Enables the `@external` media test when it is real |

Run `npx playwright test --grep-invert @external` to exclude tests that need third-party APIs.

## Design

- **`pages/`** contains the Page Object Model. Each page or modal exposes locators and intent-level actions, and the specs hold the assertions. The app has no `data-testid`s and its `<label>`s aren't linked to inputs. Locators therefore prefer roles, then placeholders, then `name` attributes.
- **`fixtures/base.ts`** is for anonymous tests. It provides the page objects, a typed REST client (`utils/api-client.ts`) and a MailHog client (`utils/mailhog.ts`).
- **`fixtures/auth.ts`** is for logged-in tests. Every test gets a fresh account: it registers through the API, reads the real verification email from MailHog, verifies, and logs in. The JWT is injected into `localStorage` exactly as the app's "Remember me" login stores it, so the specs skip the login UI. The only specs that drive the login UI are the ones testing it.
- **Isolation:** each test uses a unique email and unique entity names, and tests run fully in parallel. Categories are global in the backend, so habit tests create their own uniquely named category and subcategory.
- **Known bugs** are documented as tests that state the *correct* behaviour, marked `test.fail()` with a `BUG:` annotation. Once a bug is fixed, the test starts "unexpectedly passing", which is the signal to remove the marker.

### Non-default ports

Part of the frontend calls `http://localhost:8080/api` directly instead of using `REACT_APP_API_URL`. The verification link in the email does the same. If `API_URL` points elsewhere, the base fixture re-routes those requests to `API_URL` in the browser. With the default ports, this does nothing.

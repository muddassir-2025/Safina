# Safina

Android app (Kotlin / Jetpack Compose) plus a TypeScript API hosted on **Neon**.

The API is a Neon **Function** on the `production` branch. It talks to three Neon
services, all declared in `neon.ts`:

| Service | What it is used for | Env vars |
| --- | --- | --- |
| Postgres (Lakebase) | profiles, scenes, saves, collections, reports, defaults, audit log | `DATABASE_URL` (pooled), `DATABASE_URL_UNPOOLED` (migrations) |
| Neon Auth (Managed Better Auth) | Google sign-in; user identity is the JWT `sub` | `NEON_AUTH_BASE_URL`, `NEON_AUTH_JWKS_URL` |
| Object Storage | scene HTML when `SCENE_STORAGE=s3` | `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_ENDPOINT_URL_S3`, `AWS_REGION` |

```
app/                     Android client (existing)
src/app.ts               Hono API (default export = Neon Function handler)
src/node.ts              local Node server (same app)
src/db/schema.ts         Drizzle schema
drizzle/                 generated SQL migrations
neon.ts                  Neon infrastructure-as-code (auth, bucket, function)
scripts/e2e.sh           end-to-end proof for all three services
```

## Live endpoints

- API: `https://br-quiet-dew-a1bz1a6p-api.compute.c-1.ap-southeast-1.aws.neon.tech`
- Health: `GET /health`
- Neon Auth base: `https://ep-lively-waterfall-a1hp2tlq.neonauth.ap-southeast-1.aws.neon.tech/neondb/auth`
- Neon Auth JWKS: `${NEON_AUTH_BASE_URL}/.well-known/jwks.json`

### Which Neon Auth endpoints are used

| Path | Use |
| --- | --- |
| `POST ${NEON_AUTH_BASE_URL}/sign-in/social` | starts Google sign-in (`provider: "google"`, `callbackURL`) |
| `GET ${NEON_AUTH_BASE_URL}/callback/google` | Google returns here; Neon Auth owns the session cookie |
| `GET ${NEON_AUTH_BASE_URL}/token` | exchanges the session for a 15-minute EdDSA JWT |
| `${NEON_AUTH_JWKS_URL}` | our API verifies every `Authorization: Bearer <jwt>` |

Wiring used by the API (`src/auth.ts`): `jwtVerify(token, jwks, { issuer })` where
`issuer = new URL(NEON_AUTH_BASE_URL).origin`. No shared secret is stored.

### What must be configured in Neon Auth

- **Google provider**: already enabled on this branch with Neon's shared
  development Google client. For production, add your own OAuth client with
  `neon neon-auth oauth-provider update google ...`.
- **Trusted domains**: the API origin must be trusted, otherwise sign-in fails
  with `INVALID_ORIGIN`. Already added:
  `https://br-quiet-dew-a1bz1a6p-api.compute.c-1.ap-southeast-1.aws.neon.tech`.
  Add every other origin you serve from (Render URL, custom domain):
  ```bash
  neon neon-auth domain add https://your-api.example.com
  neon neon-auth domain list
  ```

## Local development

```bash
npm install
neon deploy --env .env.local   # provisions services + pulls env vars
npm run db:migrate             # applies ./drizzle using DATABASE_URL_UNPOOLED
npm run build
npm start                      # http://localhost:8080
```

Scene storage backend is chosen by `SCENE_STORAGE` (`postgres` default, or `s3`
for the Neon Object Storage bucket).

## Deploy

The API is deployed as a Neon Function:

```bash
neon deploy --env .env.local
neon functions get api        # prints invocation_url
```

`neon.ts` declares the admin secrets so `neon deploy` uploads them with the
function. An unset required value aborts the deploy before anything changes.

### Deploying to Render instead

The same `src/app.ts` runs on a Render free web service:

- Build: `npm install && npm run build`
- Start: `npm start` (listens on `$PORT`)
- Add every variable from `.env.example` in the Render dashboard, including
  `DATABASE_URL` (pooled), `NEON_AUTH_BASE_URL`, `NEON_AUTH_JWKS_URL`, the
  `AWS_*` storage values and the `ADMIN_*` trio.
- After the first deploy, add the Render origin as a Neon Auth trusted domain.
- Free instances sleep. `GET /health` is the wake-up probe; the Android client
  already retries with backoff (`Api.withColdStartRetry`).

## Security notes

- Scene HTML is untrusted. The API rejects files over 1 MB, non-HTML files, and
  any content with `fetch(`, `XMLHttpRequest`, `WebSocket`, `EventSource`,
  dynamic `import(`, `@import url`, or external `src`/`href` URLs.
- The WebView renders scenes with `default-src 'none'` CSP, JS bridge limited to
  a whitelisted API, file access off, and no navigation.
- User JWTs are Neon Auth tokens kept in `EncryptedSharedPreferences`.
- Admin auth is completely separate: `POST /admin/login` compares a bcrypt hash,
  is limited to 5 attempts / 15 min / IP, delays failures, and issues a 30-minute
  HS256 token signed with `ADMIN_JWT_SECRET`. `/admin/*` returns **404** without a
  valid admin token. Passwords, hashes and tokens are never logged.
- Rate limits are in-memory per isolate. Use Upstash Redis if you scale beyond
  one instance.

## Known limitations

- **Native Google sign-in needs a real-device test.** `/auth/google/start` and
  `/auth/callback` implement the Custom Tab relay, but the final step reads the
  JWT from Neon Auth's `/token` with a cross-site cookie. Verify it in Chrome on
  a device; if third-party cookies are blocked, host the sign-in page on a
  trusted origin instead. The JWT verification path itself is proven by
  `scripts/e2e.sh` using a real Neon Auth session.
- `/auth/me` never creates a profile row; a row appears when the user claims a
  channel name (required before publishing).

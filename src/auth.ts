import { SignJWT, jwtVerify } from "jose";
import { createRemoteJWKSet } from "jose";
import { config } from "./config.js";

/* ------------------------------------------------------------------ */
/* User identity: Neon Auth (Managed Better Auth) JWTs                 */
/* ------------------------------------------------------------------ */

export type SceneUser = { userId: string; email: string | null };

let jwksCache: ReturnType<typeof createRemoteJWKSet> | null = null;

function jwks() {
  if (!jwksCache) {
    // Neon Auth publishes EdDSA keys; tokens expire in 15 minutes.
    jwksCache = createRemoteJWKSet(new URL(config().NEON_AUTH_JWKS_URL));
  }
  return jwksCache;
}

function issuer(): string {
  // Issuer is the origin of the Auth URL (the path is the database scope).
  return new URL(config().NEON_AUTH_BASE_URL).origin;
}

/**
 * Verifies a Neon Auth access token. Returns null for any invalid token —
 * callers decide whether a missing identity is a 401 or an anonymous request.
 */
export async function verifyUserToken(token: string | undefined | null): Promise<SceneUser | null> {
  if (!token) return null;
  // Try Neon Auth (EdDSA) first.
  try {
    const { payload } = await jwtVerify(token, jwks(), { issuer: issuer() });
    if (payload.sub) {
      return {
        userId: payload.sub,
        email: typeof payload.email === "string" ? payload.email : null,
      };
    }
  } catch {
    // fall through to reviewer token
  }
  // Reviewer (passwordless email) token.
  const reviewer = await verifyReviewerToken(token);
  if (reviewer) return reviewer;
  // Safina long-lived session token (issued after a successful Neon Auth sign-in).
  return verifySafinaUserToken(token);
}

export function bearer(header: string | undefined | null): string | null {
  if (!header) return null;
  const match = /^Bearer\s+(.+)$/i.exec(header.trim());
  return match?.[1] ?? null;
}

/* ------------------------------------------------------------------ */
/* Admin identity: separate HS256 JWT, never mixed with Neon Auth      */
/* ------------------------------------------------------------------ */

export type AdminClaims = { email: string; role: "admin"; exp: number };

const ADMIN_TTL_SECONDS = 30 * 60;

function adminKey(): Uint8Array {
  return new TextEncoder().encode(config().ADMIN_JWT_SECRET);
}

export async function signAdminToken(email: string): Promise<{ token: string; expiresAt: number }> {
  const nowSec = Math.floor(Date.now() / 1000);
  const exp = nowSec + ADMIN_TTL_SECONDS;
  const token = await new SignJWT({ role: "admin", email })
    .setProtectedHeader({ alg: "HS256", typ: "JWT" })
    .setSubject(email)
    .setIssuedAt(nowSec)
    .setIssuer("safina-admin")
    .setAudience("safina-admin")
    .setExpirationTime(exp)
    .sign(adminKey());
  return { token, expiresAt: exp * 1000 };
}

export async function verifyAdminToken(token: string | undefined | null): Promise<AdminClaims | null> {
  if (!token) return null;
  try {
    const { payload } = await jwtVerify(token, adminKey(), {
      issuer: "safina-admin",
      audience: "safina-admin",
    });
    if (payload.role !== "admin" || typeof payload.sub !== "string") return null;
    return {
      email: payload.sub,
      role: "admin",
      exp: typeof payload.exp === "number" ? payload.exp : 0,
    };
  } catch {
    return null;
  }
}

/* ------------------------------------------------------------------ */
/* Reviewer (passwordless email) identity for Play Console testing     */
/* ------------------------------------------------------------------ */

const REVIEWER_TTL_SECONDS = 7 * 24 * 60 * 60;

export async function signReviewerToken(email: string): Promise<{ token: string; expiresAt: number }> {
  const nowSec = Math.floor(Date.now() / 1000);
  const exp = nowSec + REVIEWER_TTL_SECONDS;
  const subject = `reviewer:${email.toLowerCase()}`;
  const token = await new SignJWT({ role: "reviewer", email })
    .setProtectedHeader({ alg: "HS256", typ: "JWT" })
    .setSubject(subject)
    .setIssuedAt(nowSec)
    .setIssuer("safina-reviewer")
    .setAudience("safina-reviewer")
    .setExpirationTime(exp)
    .sign(adminKey());
  return { token, expiresAt: exp * 1000 };
}

export async function verifyReviewerToken(token: string | undefined | null): Promise<SceneUser | null> {
  if (!token) return null;
  try {
    const { payload } = await jwtVerify(token, adminKey(), {
      issuer: "safina-reviewer",
      audience: "safina-reviewer",
    });
    if (typeof payload.sub !== "string") return null;
    if (payload.role !== "reviewer") return null;
    return {
      userId: payload.sub,
      email: typeof payload.email === "string" ? payload.email : null,
    };
  } catch {
    return null;
  }
}

/* ------------------------------------------------------------------ */
/* Safina long-lived session token (issued after Neon Auth sign-in)    */
/* ------------------------------------------------------------------ */

const SAFINA_USER_TTL_SECONDS = 30 * 24 * 60 * 60;

export async function signSafinaUserToken(
  userId: string,
  email: string | null
): Promise<{ token: string; expiresAt: number }> {
  const nowSec = Math.floor(Date.now() / 1000);
  const exp = nowSec + SAFINA_USER_TTL_SECONDS;
  const token = await new SignJWT({ role: "user", email })
    .setProtectedHeader({ alg: "HS256", typ: "JWT" })
    .setSubject(userId)
    .setIssuedAt(nowSec)
    .setIssuer("safina-user")
    .setAudience("safina-user")
    .setExpirationTime(exp)
    .sign(adminKey());
  return { token, expiresAt: exp * 1000 };
}

export async function verifySafinaUserToken(token: string | undefined | null): Promise<SceneUser | null> {
  if (!token) return null;
  try {
    const { payload } = await jwtVerify(token, adminKey(), {
      issuer: "safina-user",
      audience: "safina-user",
    });
    if (typeof payload.sub !== "string") return null;
    return {
      userId: payload.sub,
      email: typeof payload.email === "string" ? payload.email : null,
    };
  } catch {
    return null;
  }
}

/** Verifies a Neon Auth (EdDSA) token only — used by /auth/exchange. */
export async function verifyNeonUserToken(token: string | undefined | null): Promise<SceneUser | null> {
  if (!token) return null;
  try {
    const { payload } = await jwtVerify(token, jwks(), { issuer: issuer() });
    if (payload.sub) {
      return {
        userId: payload.sub,
        email: typeof payload.email === "string" ? payload.email : null,
      };
    }
  } catch {
    // not a Neon token
  }
  return null;
}

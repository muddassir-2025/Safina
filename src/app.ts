import { Hono, type Context } from "hono";
import { cors } from "hono/cors";
import bcrypt from "bcryptjs";
import { and, asc, count, desc, eq, ilike, inArray, isNull, or, sql } from "drizzle-orm";
import { createHash, randomUUID } from "node:crypto";
import { config, MAX_TITLE_LENGTH } from "./config.js";
import { db } from "./db/client.js";
import {
  auditLog,
  collections,
  defaultScenes,
  profiles,
  publicScenes,
  reports,
  saves,
} from "./db/schema.js";
import {
  bearer,
  signAdminToken,
  signReviewerToken,
  verifyAdminToken,
  verifyUserToken,
  type SceneUser,
} from "./auth.js";
import { storage } from "./storage.js";
import { rateLimit, userKey } from "./rate.js";
import {
  channelError,
  checkSceneHtml,
  collectionNameError,
  sceneSize,
  titleError,
} from "./validation.js";

type Vars = { userId?: string; userEmail?: string; adminEmail?: string };
type Ctx = Context<{ Variables: Vars }>;

class HttpError extends Error {
  constructor(
    readonly status: number,
    message: string,
    readonly code?: string
  ) {
    super(message);
  }
}

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function uuid(value: string | undefined): string {
  if (!value || !UUID_RE.test(value)) throw new HttpError(404, "Not found");
  return value;
}

async function optionalUser(c: Ctx): Promise<SceneUser | null> {
  try {
    return await verifyUserToken(bearer(c.req.header("authorization")));
  } catch {
    return null;
  }
}

async function requireUser(c: Ctx): Promise<SceneUser> {
  const user = await optionalUser(c);
  if (!user) throw new HttpError(401, "Sign in required", "UNAUTHORIZED");
  return user;
}

async function jsonBody(c: Ctx): Promise<Record<string, unknown>> {
  try {
    const value = await c.req.json();
    return value && typeof value === "object" ? (value as Record<string, unknown>) : {};
  } catch {
    return {};
  }
}

function intParam(value: string | undefined, fallback: number, min: number, max: number): number {
  const n = Number.parseInt(value ?? "", 10);
  if (!Number.isFinite(n)) return fallback;
  return Math.min(max, Math.max(min, n));
}

/* ------------------------------------------------------------------ */
/* Row → wire format (must match Cloud.kt's PublicScene parser)        */
/* ------------------------------------------------------------------ */

type SceneRow = typeof publicScenes.$inferSelect;
type AuthorJoin = { channelName: string | null } | null;

function authorOf(row: SceneRow, profile: AuthorJoin): string {
  if (row.isOfficial) return row.authorLabel ?? "Official";
  return profile?.channelName ?? "Unknown";
}

function sceneJson(
  row: SceneRow,
  profile: AuthorJoin,
  viewer: { userId?: string; savedIds?: Set<string>; html?: string | null }
) {
  return {
    id: row.id,
    title: row.isRemoved ? "This scene was removed" : row.title,
    author: authorOf(row, profile),
    savedCount: row.savedCount,
    size: row.size,
    createdAt: row.createdAt.getTime(),
    isSaved: viewer.savedIds ? viewer.savedIds.has(row.id) : false,
    isOwn: viewer.userId ? row.ownerId === viewer.userId : false,
    ownerId: row.isOfficial ? null : row.ownerId,
    removed: row.isRemoved,
    locked: row.locked,
    ...(viewer.html ? { html: viewer.html } : {}),
  };
}

async function savedIdsFor(userId: string | undefined, sceneIds: string[]): Promise<Set<string>> {
  if (!userId || sceneIds.length === 0) return new Set();
  const rows = await db
    .select({ sceneId: saves.sceneId })
    .from(saves)
    .where(and(eq(saves.userId, userId), inArray(saves.sceneId, sceneIds)));
  return new Set(rows.map((r) => r.sceneId));
}

async function ensureProfile(user: SceneUser) {
  const found = await db
    .select()
    .from(profiles)
    .where(eq(profiles.userId, user.userId))
    .limit(1);
  const existing = found[0];
  if (existing) return existing;
  // A profile row is created when the user picks a channel name; until then
  // there is simply no row, and /auth/me reports channelName: null.
  return null;
}

async function writeAudit(action: string, targetType: string, targetId: string, meta?: unknown) {
  await db.insert(auditLog).values({
    action,
    targetType,
    targetId,
    meta: meta ?? null,
  });
}

/* ------------------------------------------------------------------ */
/* App                                                                 */
/* ------------------------------------------------------------------ */

export const app = new Hono<{ Variables: Vars }>();

app.use("*", cors({ origin: "*", allowHeaders: ["Authorization", "Content-Type", "If-None-Match"] }));

app.onError((err, c) => {
  if (err instanceof HttpError) {
    return c.json({ error: err.message, code: err.code }, err.status as never);
  }
  console.error("[api] unhandled", err instanceof Error ? err.message : err);
  return c.json({ error: "Something went wrong", code: "INTERNAL" }, 500);
});

app.get("/health", async (c) => {
  try {
    await db.execute(sql`select 1`);
    return c.json({
      ok: true,
      db: true,
      storage: storage().kind,
      time: new Date().toISOString(),
    });
  } catch {
    return c.json({ ok: false, db: false }, 503);
  }
});

/* ---------------- auth / profile ---------------- */

app.get("/auth/me", async (c) => {
  const user = await requireUser(c);
  const profile = await ensureProfile(user);
  const isAdmin =
    (user.email ?? "").toLowerCase() === config().ADMIN_EMAIL.toLowerCase();
  return c.json({
    user: { id: user.userId, email: user.email },
    isAdmin,
    profile: profile
      ? {
          userId: profile.userId,
          email: profile.email,
          channelName: profile.channelName,
        }
      : null,
  });
});

app.get("/profile/channel/available", async (c) => {
  const user = await requireUser(c);
  const name = c.req.query("name") ?? "";
  if (channelError(name)) return c.json({ available: false, error: channelError(name) });
  const rows = await db
    .select({ userId: profiles.userId })
    .from(profiles)
    .where(sql`lower(${profiles.channelName}) = lower(${name.trim()})`)
    .limit(1);
  const owner = rows[0];
  return c.json({ available: !owner || owner.userId === user.userId });
});

app.put("/profile/channel", async (c) => {
  const user = await requireUser(c);
  const body = await jsonBody(c);
  const name = typeof body.channelName === "string" ? body.channelName.trim() : "";
  const problem = channelError(name);
  if (problem) throw new HttpError(400, problem, "INVALID_CHANNEL");

  const taken = await db
    .select({ userId: profiles.userId })
    .from(profiles)
    .where(sql`lower(${profiles.channelName}) = lower(${name})`)
    .limit(1);
  const owner = taken[0];
  if (owner && owner.userId !== user.userId) {
    throw new HttpError(409, "That channel name is taken", "CHANNEL_TAKEN");
  }

  const existing = await ensureProfile(user);
  if (existing) {
    await db.update(profiles).set({ channelName: name }).where(eq(profiles.userId, user.userId));
  } else {
    await db.insert(profiles).values({
      userId: user.userId,
      email: user.email,
      channelName: name,
    });
  }
  return c.json({
    profile: { userId: user.userId, email: user.email, channelName: name },
  });
});

/**
 * Mobile Google sign-in relay.
 *
 * The app opens this in a Custom Tab. We ask Neon Auth to start the Google
 * flow and then bounce the browser to Neon Auth. When it comes back we read a
 * JWT from Neon Auth and hand it to the app through the `safina://auth` deep
 * link. Keep the JWT out of any other page.
 */
app.get("/auth/google/start", (c) => {
  const cfg = config();
  const authBase = cfg.NEON_AUTH_BASE_URL.replace(/\/$/, "");
  const redirect = c.req.query("redirect") || "safina://auth";
  // Derive the public origin from the request so this works on any host.
  const origin = new URL(c.req.url).origin;
  const callback = `${origin}/auth/callback?redirect=${encodeURIComponent(redirect)}`;
  const html = `<!doctype html><meta charset="utf-8"><title>Signing in…</title>
<body style="font-family:system-ui;padding:2rem">
<p>Redirecting to Google…</p>
<script>
(async () => {
  try {
    const res = await fetch(${JSON.stringify(authBase + "/sign-in/social")}, {
      method: "POST",
      headers: { "content-type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ provider: "google", callbackURL: ${JSON.stringify(callback)} })
    });
    const data = await res.json();
    if (data && data.url) { location.replace(data.url); return; }
    document.body.textContent = (data && data.message) || "Could not start Google sign-in.";
  } catch (e) {
    document.body.textContent = "Could not start Google sign-in.";
  }
})();
</script></body>`;
  return c.html(html);
});

app.get("/auth/callback", (c) => {
  const cfg = config();
  const authBase = cfg.NEON_AUTH_BASE_URL.replace(/\/$/, "");
  const redirect = c.req.query("redirect") || "safina://auth";
  if (!/^safina:\/\/auth/.test(redirect)) throw new HttpError(400, "Invalid redirect");
  // Names only — never log token/hash values.
  const params = Object.keys(c.req.query());
  console.log("auth callback params", JSON.stringify(params));
  const html = `<!doctype html><meta charset="utf-8"><title>Finishing sign-in…</title>
<body style="font-family:system-ui;padding:2rem">
<p>Finishing sign-in…</p>
<p id="detail" style="color:#777;font-size:13px"></p>
<script>
(async () => {
  const qs = new URLSearchParams(location.search);
  const detail = document.getElementById("detail");
  function fail(message) {
    const providerError = qs.get("error_description") || qs.get("error") || "";
    document.querySelector("p").textContent = message;
    detail.textContent = providerError ? "Google said: " + providerError : "";
    const reason = encodeURIComponent(message + (providerError ? " — " + providerError : ""));
    fetch(${JSON.stringify("/auth/report")}, {
      method: "POST",
      headers: { "content-type": "application/json" },
      keepalive: true,
      body: JSON.stringify({ reason, params: [...qs.keys()] })
    }).catch(() => {});
  }
  try {
    let token = null;
    let exchanged = null;
    // OAuth returns a verifier that has to be exchanged for a session first
    // (this is what neon-js does before it reads /token).
    const verifier = qs.get("neon_auth_session_verifier");
    if (verifier) {
      const exchange = await fetch(
        ${JSON.stringify(authBase + "/get-session?neon_auth_session_verifier=")} + encodeURIComponent(verifier),
        { credentials: "include" }
      );
      if (exchange.ok) exchanged = await exchange.json().catch(() => null);
    }
    // /token is the endpoint that returns the Neon Auth JWT the API verifies.
    const res = await fetch(${JSON.stringify(authBase + "/token")}, { credentials: "include" });
    if (res.ok) {
      const data = await res.json();
      token = data.token || data.jwt || (data.data && data.data.token);
    }
    if (!token && exchanged) {
      token = (exchanged.session && exchanged.session.token) || exchanged.token || null;
    }
    if (!token) {
      fail("Sign-in could not be completed (HTTP " + res.status + ").");
      return;
    }
    fetch(${JSON.stringify("/auth/report")}, {
      method: "POST",
      headers: { "content-type": "application/json" },
      keepalive: true,
      body: JSON.stringify({
        reason: "redirect",
        params: [...qs.keys()],
        shape: { len: token.length, dots: (token.match(/\\./g) || []).length }
      })
    }).catch(() => {});
    location.replace(${JSON.stringify(redirect + "?token=")} + encodeURIComponent(token));
  } catch (e) {
    fail("Sign-in could not be completed (" + (e && e.message ? e.message : "network") + ").");
  }
})();
</script></body>`;
  return c.html(html);
});

/** Diagnostics from the relay page: the reason a browser-side sign-in failed. */
app.post("/auth/report", async (c) => {
  const body = await c.req.json().catch(() => ({}) as Record<string, unknown>);
  const reason = String((body as Record<string, unknown>).reason ?? "").slice(0, 300);
  const params = Array.isArray((body as Record<string, unknown>).params)
    ? ((body as Record<string, unknown>).params as unknown[]).map(String).slice(0, 20)
    : [];
  const rawShape = (body as Record<string, unknown>).shape;
  const shape = rawShape && typeof rawShape === "object"
    ? {
        len: Number((rawShape as Record<string, unknown>).len) || 0,
        dots: Number((rawShape as Record<string, unknown>).dots) || 0
      }
    : undefined;
  console.log("auth relay", JSON.stringify({ reason, params, shape }));
  return c.json({ ok: true });
});

/* ---------------- passwordless (reviewer) sign-in ---------------- */

app.post("/auth/review", async (c) => {
  const body = await jsonBody(c);
  const email = typeof body.email === "string" ? body.email.trim().toLowerCase() : "";
  if (!email || !email.includes("@")) {
    throw new HttpError(400, "A valid email is required", "INVALID_EMAIL");
  }

  const cfg = config();
  const allowed = new Set<string>();
  const reviewerEnv = (process.env.REVIEWER_EMAIL ?? "playconsole@example.com").toLowerCase();
  allowed.add(reviewerEnv);
  allowed.add(cfg.ADMIN_EMAIL.toLowerCase());

  // Allow any email that already has a profile row (manually added in Neon).
  const existingProfile = await db
    .select()
    .from(profiles)
    .where(sql`lower(${profiles.email}) = ${email}`)
    .limit(1);
  const profile = existingProfile[0];

  if (!allowed.has(email) && !profile) {
    throw new HttpError(401, "This email is not allowed to sign in", "NOT_ALLOWED");
  }

  const { token, expiresAt } = await signReviewerToken(email);
  return c.json({ token, expiresAt });
});

/* ---------------- public scenes ---------------- */

app.get("/public/scenes", async (c) => {
  const viewer = await optionalUser(c);
  const page = intParam(c.req.query("page"), 1, 1, 10_000);
  const limit = intParam(c.req.query("limit"), 20, 1, 50);
  const sortRaw = (c.req.query("sort") ?? "new").toLowerCase();
  const sort = ["top", "saved", "most_saved", "popular"].includes(sortRaw) ? "top" : "new";
  const q = (c.req.query("q") ?? "").trim();
  const excludeSaved = c.req.query("excludeSaved") === "true";

  const filters = [
    eq(publicScenes.isRemoved, false),
    eq(publicScenes.isOfficial, false),
    or(eq(profiles.blocked, false), isNull(profiles.blocked))!
  ];
  if (q) filters.push(ilike(publicScenes.title, `%${q}%`));
  if (excludeSaved && viewer) filters.push(sql`${publicScenes.id} not in (
    select ${saves.sceneId} from ${saves} where ${saves.userId} = ${viewer.userId}
  )`);

  const rows = await db
    .select({ scene: publicScenes, profile: profiles })
    .from(publicScenes)
    .leftJoin(profiles, eq(profiles.userId, publicScenes.ownerId))
    .where(and(...filters))
    .orderBy(sort === "top" ? desc(publicScenes.savedCount) : desc(publicScenes.createdAt))
    .limit(limit)
    .offset((page - 1) * limit);

  const ids = rows.map((r) => r.scene.id);
  const saved = await savedIdsFor(viewer?.userId, ids);
  return c.json({
    items: rows.map((r) => sceneJson(r.scene, r.profile, { userId: viewer?.userId, savedIds: saved })),
    hasMore: rows.length === limit,
    page,
  });
});

app.get("/public/scenes/:id", async (c) => {
  const viewer = await optionalUser(c);
  const id = uuid(c.req.param("id"));
  const rows = await db
    .select({ scene: publicScenes, profile: profiles })
    .from(publicScenes)
    .leftJoin(profiles, eq(profiles.userId, publicScenes.ownerId))
    .where(eq(publicScenes.id, id))
    .limit(1);
  const row = rows[0];
  if (!row) throw new HttpError(404, "Scene not found");
  if (row.scene.isRemoved) {
    return c.json({ scene: sceneJson(row.scene, row.profile, { userId: viewer?.userId }) });
  }
  const html = await storage().load(row.scene);
  const saved = await savedIdsFor(viewer?.userId, [id]);
  return c.json({
    scene: sceneJson(row.scene, row.profile, { userId: viewer?.userId, savedIds: saved, html }),
  });
});

app.post("/public/scenes", rateLimit({ name: "publish", limit: 10, windowMs: 60 * 60 * 1000 }), async (c) => {
  const user = await requireUser(c);
  const profile = await ensureProfile(user);
  if (!profile) {
    throw new HttpError(409, "Choose a channel name before publishing", "CHANNEL_REQUIRED");
  }
  const body = await jsonBody(c);
  const titleProblem = titleError(body.title);
  if (titleProblem) throw new HttpError(400, titleProblem, "INVALID_TITLE");
  const htmlCheck = checkSceneHtml(body.html);
  if (!htmlCheck.ok) throw new HttpError(400, htmlCheck.error, "INVALID_HTML");

  const html = (body.html as string).trim();
  const id = randomUUID();
  const stored = await storage().store(id, html);
  const [created] = await db
    .insert(publicScenes)
    .values({
      id,
      ownerId: user.userId,
      title: (body.title as string).trim(),
      html: stored.html,
      storageKey: stored.storageKey,
      size: sceneSize(html),
    })
    .returning();
  if (!created) throw new HttpError(500, "Could not save scene");
  return c.json(
    { scene: sceneJson(created, profile, { userId: user.userId, html }) },
    201
  );
});

app.delete("/public/scenes/:id", async (c) => {
  const user = await requireUser(c);
  const id = uuid(c.req.param("id"));
  const rows = await db.select().from(publicScenes).where(eq(publicScenes.id, id)).limit(1);
  const row = rows[0];
  if (!row) throw new HttpError(404, "Scene not found");
  if (row.ownerId !== user.userId) throw new HttpError(404, "Scene not found");
  if (row.locked) throw new HttpError(409, "This scene is locked by a moderator", "LOCKED");
  await db
    .update(publicScenes)
    .set({
      isRemoved: true,
      removedBy: user.userId,
      removedReason: "Deleted by author",
      removedAt: new Date(),
    })
    .where(eq(publicScenes.id, id));
  return c.json({ ok: true });
});

/* ---------------- saves + collections ---------------- */

app.post("/scenes/:id/save", rateLimit({ name: "save", limit: 120, windowMs: 60 * 60 * 1000, key: userKey }), async (c) => {
  const user = await requireUser(c);
  const id = uuid(c.req.param("id"));
  const body = await jsonBody(c);
  let collectionId: string | null = null;
  if (typeof body.collectionId === "string" && body.collectionId.trim()) {
    collectionId = uuid(body.collectionId.trim());
    const owned = await db
      .select({ id: collections.id })
      .from(collections)
      .where(and(eq(collections.id, collectionId), eq(collections.userId, user.userId)))
      .limit(1);
    if (!owned[0]) throw new HttpError(404, "Collection not found");
  }

  const sceneRows = await db.select().from(publicScenes).where(eq(publicScenes.id, id)).limit(1);
  const scene = sceneRows[0];
  if (!scene || scene.isRemoved) throw new HttpError(404, "Scene not found");

  const savedCount = await db.transaction(async (tx) => {
    const inserted = await tx
      .insert(saves)
      .values({ userId: user.userId, sceneId: id, collectionId })
      .onConflictDoNothing()
      .returning({ id: saves.id });
    if (inserted.length > 0) {
      const [updated] = await tx
        .update(publicScenes)
        .set({ savedCount: sql`${publicScenes.savedCount} + 1` })
        .where(eq(publicScenes.id, id))
        .returning({ savedCount: publicScenes.savedCount });
      return updated?.savedCount ?? scene.savedCount + 1;
    }
    // Already saved: move it between collections if asked.
    if (collectionId) {
      await tx
        .update(saves)
        .set({ collectionId })
        .where(and(eq(saves.userId, user.userId), eq(saves.sceneId, id)));
    }
    const [current] = await tx
      .select({ savedCount: publicScenes.savedCount })
      .from(publicScenes)
      .where(eq(publicScenes.id, id));
    return current?.savedCount ?? scene.savedCount;
  });

  return c.json({ savedCount });
});

app.delete("/scenes/:id/save", async (c) => {
  const user = await requireUser(c);
  const id = uuid(c.req.param("id"));
  const savedCount = await db.transaction(async (tx) => {
    const deleted = await tx
      .delete(saves)
      .where(and(eq(saves.userId, user.userId), eq(saves.sceneId, id)))
      .returning({ id: saves.id });
    if (deleted.length > 0) {
      const [updated] = await tx
        .update(publicScenes)
        .set({ savedCount: sql`greatest(${publicScenes.savedCount} - 1, 0)` })
        .where(eq(publicScenes.id, id))
        .returning({ savedCount: publicScenes.savedCount });
      return updated?.savedCount ?? 0;
    }
    const [current] = await tx
      .select({ savedCount: publicScenes.savedCount })
      .from(publicScenes)
      .where(eq(publicScenes.id, id));
    return current?.savedCount ?? 0;
  });
  return c.json({ savedCount });
});

app.get("/me/saves", async (c) => {
  const user = await requireUser(c);
  const rows = await db
    .select({
      sceneId: saves.sceneId,
      collectionId: saves.collectionId,
      scene: publicScenes,
      profile: profiles,
    })
    .from(saves)
    .leftJoin(publicScenes, eq(publicScenes.id, saves.sceneId))
    .leftJoin(profiles, eq(profiles.userId, publicScenes.ownerId))
    .where(eq(saves.userId, user.userId))
    .orderBy(desc(saves.createdAt));

  return c.json({
    items: rows.map((r) => {
      if (!r.scene) {
        return {
          sceneId: r.sceneId,
          collectionId: r.collectionId,
          removed: true,
          title: "This scene was removed",
        };
      }
      return {
        sceneId: r.sceneId,
        collectionId: r.collectionId,
        removed: r.scene.isRemoved,
        scene: sceneJson(r.scene, r.profile, { userId: user.userId, savedIds: new Set([r.sceneId]) }),
      };
    }),
  });
});

app.get("/collections", async (c) => {
  const user = await requireUser(c);
  const rows = await db
    .select({
      id: collections.id,
      name: collections.name,
      count: count(saves.id),
    })
    .from(collections)
    .leftJoin(saves, and(eq(saves.collectionId, collections.id), eq(saves.userId, user.userId)))
    .where(eq(collections.userId, user.userId))
    .groupBy(collections.id, collections.name)
    .orderBy(asc(collections.createdAt));
  return c.json({ items: rows.map((r) => ({ id: r.id, name: r.name, count: Number(r.count) })) });
});

app.post(
  "/collections",
  rateLimit({ name: "collection", limit: 30, windowMs: 60 * 60 * 1000, key: userKey }),
  async (c) => {
    const user = await requireUser(c);
    const body = await jsonBody(c);
    const problem = collectionNameError(body.name);
    if (problem) throw new HttpError(400, problem, "INVALID_NAME");
    const name = (body.name as string).trim();
    const existing = await db
      .select({ id: collections.id })
      .from(collections)
      .where(and(eq(collections.userId, user.userId), sql`lower(${collections.name}) = lower(${name})`))
      .limit(1);
    if (existing[0]) throw new HttpError(409, "You already have a collection with that name", "DUPLICATE");
    const [created] = await db
      .insert(collections)
      .values({ userId: user.userId, name })
      .returning();
    if (!created) throw new HttpError(500, "Could not create collection");
    return c.json({ collection: { id: created.id, name: created.name, count: 0 } }, 201);
  }
);

app.patch("/collections/:id", async (c) => {
  const user = await requireUser(c);
  const id = uuid(c.req.param("id"));
  const body = await jsonBody(c);
  const problem = collectionNameError(body.name);
  if (problem) throw new HttpError(400, problem, "INVALID_NAME");
  const name = (body.name as string).trim();
  const updated = await db
    .update(collections)
    .set({ name })
    .where(and(eq(collections.id, id), eq(collections.userId, user.userId)))
    .returning();
  const row = updated[0];
  if (!row) throw new HttpError(404, "Collection not found");
  return c.json({ collection: { id: row.id, name: row.name, count: 0 } });
});

app.delete("/collections/:id", async (c) => {
  const user = await requireUser(c);
  const id = uuid(c.req.param("id"));
  await db.transaction(async (tx) => {
    // Scenes fall back to Uncategorized instead of disappearing.
    await tx
      .update(saves)
      .set({ collectionId: null })
      .where(and(eq(saves.collectionId, id), eq(saves.userId, user.userId)));
    await tx
      .delete(collections)
      .where(and(eq(collections.id, id), eq(collections.userId, user.userId)));
  });
  return c.json({ ok: true });
});

app.post(
  "/scenes/:id/report",
  rateLimit({ name: "report", limit: 30, windowMs: 24 * 60 * 60 * 1000, key: userKey }),
  async (c) => {
    const user = await requireUser(c);
    const id = uuid(c.req.param("id"));
    const body = await jsonBody(c);
    const reason = typeof body.reason === "string" ? body.reason.trim().slice(0, 500) : "";
    if (!reason) throw new HttpError(400, "A reason is required", "INVALID_REASON");
    const rows = await db
      .select({ id: publicScenes.id })
      .from(publicScenes)
      .where(eq(publicScenes.id, id))
      .limit(1);
    if (!rows[0]) throw new HttpError(404, "Scene not found");
    await db.insert(reports).values({ sceneId: id, reporterId: user.userId, reason });
    return c.json({ ok: true });
  }
);

/* ---------------- default scenes (public, cached) ---------------- */

app.get("/default-scenes", async (c) => {
  const rows = await db
    .select()
    .from(defaultScenes)
    .where(eq(defaultScenes.isActive, true))
    .orderBy(asc(defaultScenes.sortOrder), asc(defaultScenes.createdAt));

  const hash = createHash("sha1");
  for (const row of rows) {
    hash.update(`${row.id}:${row.version}:${row.updatedAt.getTime()};`);
  }
  const etag = `W/"${hash.digest("hex")}"`;
  if (c.req.header("if-none-match") === etag) {
    return new Response(null, { status: 304, headers: { ETag: etag } });
  }

  c.header("ETag", etag);
  c.header("Cache-Control", "no-cache");
  return c.json({
    // Client compares this string; it changes whenever a default scene changes.
    version: etag,
    items: rows.map((row) => ({
      id: row.id,
      title: row.title,
      html: row.html,
      size: row.size,
      sortOrder: row.sortOrder,
      version: row.version,
    })),
  });
});

/* ---------------- admin ---------------- */

const adminAttempts = new Map<string, { count: number; resetAt: number }>();

app.post("/admin/login", async (c) => {
  const cfg = config();
  const ip =
    c.req.header("x-forwarded-for")?.split(",")[0]?.trim() || c.req.header("x-real-ip") || "unknown";
  const now = Date.now();
  const hit = adminAttempts.get(ip);
  if (!hit || hit.resetAt <= now) {
    adminAttempts.set(ip, { count: 1, resetAt: now + 15 * 60 * 1000 });
  } else if (hit.count >= 5) {
    // Generic response even when throttled.
    await new Promise((r) => setTimeout(r, 400));
    return c.json({ error: "Invalid credentials", code: "INVALID_CREDENTIALS" }, 401);
  } else {
    hit.count += 1;
  }

  const body = await jsonBody(c);
  const email = typeof body.email === "string" ? body.email.trim() : "";
  const password = typeof body.password === "string" ? body.password : "";

  const emailMatches = email.toLowerCase() === cfg.ADMIN_EMAIL.toLowerCase();
  // Compare even on an email mismatch so timing does not leak which half failed.
  const passwordMatches = await bcrypt.compare(password, cfg.ADMIN_PASSWORD_HASH).catch(() => false);

  if (!emailMatches || !passwordMatches) {
    await new Promise((r) => setTimeout(r, 400));
    return c.json({ error: "Invalid credentials", code: "INVALID_CREDENTIALS" }, 401);
  }

  adminAttempts.delete(ip);
  const { token, expiresAt } = await signAdminToken(cfg.ADMIN_EMAIL);
  await writeAudit("admin.login", "admin", cfg.ADMIN_EMAIL);
  return c.json({ token, expiresAt });
});

/** Admin routes answer 404 to everyone else, never 403. */
app.use("/admin/*", async (c, next) => {
  if (c.req.path === "/admin/login") return next();
  const token = bearer(c.req.header("authorization"));
  const claims = await verifyAdminToken(token);
  if (claims) {
    c.set("adminEmail", claims.email);
    return next();
  }
  // The Google account whose email matches ADMIN_EMAIL is an admin too.
  const user = await verifyUserToken(token).catch(() => null);
  if (
    user?.email &&
    user.email.toLowerCase() === config().ADMIN_EMAIL.toLowerCase()
  ) {
    c.set("adminEmail", user.email);
    return next();
  }
  return c.json({ error: "Not found" }, 404);
});

app.post("/admin/default-scenes", async (c) => {
  const body = await jsonBody(c);
  const titleProblem = titleError(body.title);
  if (titleProblem) throw new HttpError(400, titleProblem, "INVALID_TITLE");
  const htmlCheck = checkSceneHtml(body.html);
  if (!htmlCheck.ok) throw new HttpError(400, htmlCheck.error, "INVALID_HTML");
  const html = (body.html as string).trim();
  const sortOrder = Number.isFinite(Number(body.sortOrder)) ? Number(body.sortOrder) : 0;
  const [created] = await db
    .insert(defaultScenes)
    .values({
      title: (body.title as string).trim(),
      html,
      size: sceneSize(html),
      sortOrder,
    })
    .returning();
  if (!created) throw new HttpError(500, "Could not create default scene");
  await writeAudit("default_scene.create", "default_scene", created.id);
  return c.json({ scene: { id: created.id, title: created.title, version: created.version } }, 201);
});

app.put("/admin/default-scenes/:id", async (c) => {
  const id = uuid(c.req.param("id"));
  const body = await jsonBody(c);
  const titleProblem = titleError(body.title);
  if (titleProblem) throw new HttpError(400, titleProblem, "INVALID_TITLE");
  const htmlCheck = checkSceneHtml(body.html);
  if (!htmlCheck.ok) throw new HttpError(400, htmlCheck.error, "INVALID_HTML");
  const html = (body.html as string).trim();
  const sortOrder = Number.isFinite(Number(body.sortOrder)) ? Number(body.sortOrder) : 0;
  const [updated] = await db
    .update(defaultScenes)
    .set({
      title: (body.title as string).trim(),
      html,
      size: sceneSize(html),
      sortOrder,
      version: sql`${defaultScenes.version} + 1`,
      updatedAt: new Date(),
    })
    .where(eq(defaultScenes.id, id))
    .returning();
  if (!updated) throw new HttpError(404, "Not found");
  await writeAudit("default_scene.update", "default_scene", id);
  return c.json({ scene: { id: updated.id, title: updated.title, version: updated.version } });
});

app.delete("/admin/default-scenes/:id", async (c) => {
  const id = uuid(c.req.param("id"));
  const [updated] = await db
    .update(defaultScenes)
    .set({ isActive: false, version: sql`${defaultScenes.version} + 1`, updatedAt: new Date() })
    .where(eq(defaultScenes.id, id))
    .returning();
  if (!updated) throw new HttpError(404, "Not found");
  await writeAudit("default_scene.delete", "default_scene", id);
  return c.json({ ok: true });
});

app.post("/admin/public-scenes", async (c) => {
  const cfg = config();
  const body = await jsonBody(c);
  const titleProblem = titleError(body.title);
  if (titleProblem) throw new HttpError(400, titleProblem, "INVALID_TITLE");
  const htmlCheck = checkSceneHtml(body.html);
  if (!htmlCheck.ok) throw new HttpError(400, htmlCheck.error, "INVALID_HTML");
  const channel =
    typeof body.channelName === "string" && body.channelName.trim()
      ? body.channelName.trim().slice(0, 40)
      : "Official";

  const html = (body.html as string).trim();
  const id = randomUUID();
  const stored = await storage().store(id, html);
  const [created] = await db
    .insert(publicScenes)
    .values({
      id,
      ownerId: `admin:${cfg.ADMIN_EMAIL}`,
      title: (body.title as string).trim(),
      html: stored.html,
      storageKey: stored.storageKey,
      size: sceneSize(html),
      authorLabel: channel,
      isOfficial: true,
    })
    .returning();
  if (!created) throw new HttpError(500, "Could not create scene");
  await writeAudit("public_scene.create", "public_scene", created.id, { channel });
  return c.json({ scene: { id: created.id, title: created.title, author: channel } }, 201);
});

app.delete("/admin/public-scenes/:id", async (c) => {
  const id = uuid(c.req.param("id"));
  const body = await jsonBody(c);
  const reason =
    typeof body.reason === "string" && body.reason.trim()
      ? body.reason.trim().slice(0, 200)
      : "Removed by moderator";
  const [updated] = await db
    .update(publicScenes)
    .set({
      isRemoved: true,
      removedBy: `admin:${config().ADMIN_EMAIL}`,
      removedReason: reason,
      removedAt: new Date(),
    })
    .where(eq(publicScenes.id, id))
    .returning();
  if (!updated) throw new HttpError(404, "Not found");
  await writeAudit("public_scene.remove", "public_scene", id, { reason });
  return c.json({ ok: true });
});

app.post("/admin/public-scenes/:id/lock", async (c) => {
  const id = uuid(c.req.param("id"));
  const body = await jsonBody(c);
  const locked = Boolean(body.locked);
  const [row] = await db
    .update(publicScenes)
    .set({ locked })
    .where(eq(publicScenes.id, id))
    .returning({ id: publicScenes.id, locked: publicScenes.locked });
  if (!row) throw new HttpError(404, "Scene not found");
  await writeAudit(locked ? "public_scene.lock" : "public_scene.unlock", "public_scene", id);
  return c.json({ scene: row });
});

app.post("/admin/public-scenes/:id/restore", async (c) => {
  const id = uuid(c.req.param("id"));
  const [updated] = await db
    .update(publicScenes)
    .set({ isRemoved: false, removedBy: null, removedReason: null, removedAt: null })
    .where(eq(publicScenes.id, id))
    .returning();
  if (!updated) throw new HttpError(404, "Not found");
  await writeAudit("public_scene.restore", "public_scene", id);
  return c.json({ ok: true });
});

app.post("/admin/users/:id/block", async (c) => {
  const id = c.req.param("id");
  const body = await jsonBody(c);
  const blocked = Boolean(body.blocked);
  const [row] = await db
    .update(profiles)
    .set({ blocked })
    .where(eq(profiles.userId, id))
    .returning({ userId: profiles.userId, channelName: profiles.channelName, blocked: profiles.blocked });
  if (!row) throw new HttpError(404, "User not found");
  await writeAudit(blocked ? "user.block" : "user.unblock", "user", id);
  return c.json({ user: row });
});

app.get("/admin/dashboard", async (c) => {
  const removed = await db
    .select({
      id: publicScenes.id,
      title: publicScenes.title,
      ownerId: publicScenes.ownerId,
      removedBy: publicScenes.removedBy,
      removedAt: publicScenes.removedAt,
    })
    .from(publicScenes)
    .where(eq(publicScenes.isRemoved, true))
    .orderBy(desc(publicScenes.removedAt))
    .limit(20);

  const blocked = await db
    .select({
      userId: profiles.userId,
      channelName: profiles.channelName,
      email: profiles.email,
    })
    .from(profiles)
    .where(eq(profiles.blocked, true))
    .limit(50);

  const reported = await db
    .select({
      sceneId: reports.sceneId,
      reportCount: count(reports.id),
      reasons: sql<string>`string_agg(distinct ${reports.reason}, ' · ')`,
      title: publicScenes.title,
      ownerId: publicScenes.ownerId,
      isRemoved: publicScenes.isRemoved,
    })
    .from(reports)
    .leftJoin(publicScenes, eq(publicScenes.id, reports.sceneId))
    .groupBy(reports.sceneId, publicScenes.title, publicScenes.ownerId, publicScenes.isRemoved)
    .orderBy(desc(count(reports.id)))
    .limit(20);

  return c.json({ removed, blocked, reports: reported });
});

app.get("/admin/reports", async (c) => {
  const rows = await db
    .select({
      sceneId: reports.sceneId,
      reportCount: count(reports.id),
      lastReportedAt: sql<Date>`max(${reports.createdAt})`,
      reasons: sql<string>`string_agg(distinct ${reports.reason}, ' · ')`,
      title: publicScenes.title,
      authorLabel: publicScenes.authorLabel,
      isOfficial: publicScenes.isOfficial,
      isRemoved: publicScenes.isRemoved,
      ownerId: publicScenes.ownerId,
    })
    .from(reports)
    .leftJoin(publicScenes, eq(publicScenes.id, reports.sceneId))
    .groupBy(
      reports.sceneId,
      publicScenes.title,
      publicScenes.authorLabel,
      publicScenes.isOfficial,
      publicScenes.isRemoved,
      publicScenes.ownerId
    )
    .orderBy(desc(count(reports.id)));

  const owners = rows.map((r) => r.ownerId).filter((v): v is string => Boolean(v));
  const profileRows = owners.length
    ? await db
        .select({ userId: profiles.userId, channelName: profiles.channelName })
        .from(profiles)
        .where(inArray(profiles.userId, owners))
    : [];
  const names = new Map(profileRows.map((p) => [p.userId, p.channelName]));

  return c.json({
    items: rows.map((r) => ({
      sceneId: r.sceneId,
      title: r.title ?? "(deleted scene)",
      author: r.isOfficial ? r.authorLabel ?? "Official" : names.get(r.ownerId ?? "") ?? "Unknown",
      reportCount: Number(r.reportCount),
      reasons: r.reasons ?? "",
      lastReportedAt: r.lastReportedAt ? new Date(r.lastReportedAt).getTime() : 0,
      removed: r.isRemoved ?? false,
    })),
  });
});

app.post("/admin/reports/:sceneId/dismiss", async (c) => {
  const sceneId = uuid(c.req.param("sceneId"));
  await db.delete(reports).where(eq(reports.sceneId, sceneId));
  await writeAudit("reports.dismiss", "public_scene", sceneId);
  return c.json({ ok: true });
});

app.get("/admin/audit-log", async (c) => {
  const rows = await db
    .select()
    .from(auditLog)
    .orderBy(desc(auditLog.createdAt))
    .limit(100);
  return c.json({ items: rows });
});

export const limits = { maxTitleLength: MAX_TITLE_LENGTH, maxHtmlBytes: 1024 * 1024 };
export default app;

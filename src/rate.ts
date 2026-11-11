import type { MiddlewareHandler } from "hono";

/**
 * Fixed-window in-memory limiter. Single-isolate only — enough to blunt abuse
 * on one instance, not a distributed limit. Note this in the README before
 * scaling past one instance.
 */
type Hit = { count: number; resetAt: number };

const buckets = new Map<string, Hit>();

export function rateLimit(options: {
  name: string;
  limit: number;
  windowMs: number;
  key?: (c: Parameters<MiddlewareHandler>[0]) => string;
}): MiddlewareHandler {
  const { name, limit, windowMs } = options;
  return async (c, next) => {
    const ip =
      c.req.header("x-forwarded-for")?.split(",")[0]?.trim() ||
      c.req.header("x-real-ip") ||
      "unknown";
    const key = `${name}:${options.key ? options.key(c) : ip}`;
    const now = Date.now();
    const hit = buckets.get(key);
    if (!hit || hit.resetAt <= now) {
      buckets.set(key, { count: 1, resetAt: now + windowMs });
      await next();
      return;
    }
    if (hit.count >= limit) {
      const retryAfter = Math.max(1, Math.ceil((hit.resetAt - now) / 1000));
      c.header("Retry-After", String(retryAfter));
      return c.json({ error: "Too many requests. Try again later.", code: "RATE_LIMITED" }, 429);
    }
    hit.count += 1;
    await next();
  };
}

/** Session-scoped key, used by authenticated endpoints. */
export function userKey(c: Parameters<MiddlewareHandler>[0]): string {
  const userId = (c.get("userId") as string | undefined) ?? "";
  if (userId) return `u:${userId}`;
  return `ip:${c.req.header("x-forwarded-for")?.split(",")[0]?.trim() ?? "unknown"}`;
}

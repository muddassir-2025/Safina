import { drizzle } from "drizzle-orm/node-postgres";
import pg from "pg";
import { config } from "../config.js";
import * as schema from "./schema.js";

/**
 * One pool at module scope, reused across requests. Neon Functions keeps an
 * isolate alive between requests, so a per-request pool would exhaust the
 * pooler. max stays small because every isolate opens its own pool.
 *
 * DATABASE_URL is the pooled connection string; migrations use the unpooled one.
 */
const pool = new pg.Pool({
  connectionString: config().DATABASE_URL,
  max: 5,
  idleTimeoutMillis: 30_000,
  connectionTimeoutMillis: 10_000,
});

// node-postgres emits idle client failures as `error`; without a listener that
// is an uncaughtException. Log expected disconnects quietly.
pool.on("error", (err) => {
  const code = (err as { code?: string }).code;
  const expected =
    code === "ECONNRESET" || code === "EPIPE" || code === "ETIMEDOUT" || code === "57P01";
  if (!expected) console.error("[pg] unexpected idle client error", err.message);
});

export const db = drizzle(pool, { schema });
export { pool, schema };

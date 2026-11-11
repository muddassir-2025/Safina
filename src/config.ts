import { z } from "zod";

/**
 * Every value here comes from `neon deploy` (Neon-managed) or from the
 * environment the API runs in (Render / Neon Functions / local shell).
 */
const Env = z.object({
  DATABASE_URL: z.string().min(1, "DATABASE_URL is required (pooled connection string)"),
  DATABASE_URL_UNPOOLED: z.string().min(1).optional(),
  NEON_AUTH_BASE_URL: z.string().url(),
  NEON_AUTH_JWKS_URL: z.string().url(),

  // Object Storage (Neon injects these as AWS_* variables)
  AWS_ACCESS_KEY_ID: z.string().min(1).optional(),
  AWS_SECRET_ACCESS_KEY: z.string().min(1).optional(),
  AWS_ENDPOINT_URL_S3: z.string().url().optional(),
  AWS_REGION: z.string().min(1).optional(),
  SCENE_BUCKET: z.string().min(1).default("assets"),

  // postgres (TEXT column, default) | s3 (Neon Object Storage)
  SCENE_STORAGE: z.enum(["postgres", "s3"]).default("postgres"),

  // Public origin of this API, used to build OAuth callback URLs.
  PUBLIC_BASE_URL: z.string().url().default("http://localhost:8080"),

  // Admin (single account, env only)
  ADMIN_EMAIL: z.string().email(),
  ADMIN_PASSWORD_HASH: z.string().min(1),
  ADMIN_JWT_SECRET: z.string().min(32, "ADMIN_JWT_SECRET must be at least 32 characters"),
});

export type AppConfig = z.infer<typeof Env>;

let cached: AppConfig | null = null;

export function config(): AppConfig {
  if (cached) return cached;
  const parsed = Env.safeParse(process.env);
  if (!parsed.success) {
    const detail = parsed.error.issues
      .map((i) => `${i.path.join(".")}: ${i.message}`)
      .join("; ");
    throw new Error(`Invalid environment: ${detail}`);
  }
  cached = parsed.data;
  return cached;
}

export const MAX_HTML_BYTES = 1024 * 1024;
export const MAX_TITLE_LENGTH = 40;
export const MAX_COLLECTION_NAME = 40;
export const CHANNEL_RE = /^[A-Za-z0-9_]{3,20}$/;

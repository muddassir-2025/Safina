import { defineConfig } from "@neon/config/v1";

export default defineConfig({
  // Declare your Neon services here
  auth: true,
  // The Safina API (Hono). Deployed to the branch by `neon deploy`.
  functions: {
    api: {
      name: "safina api",
      source: "src/app.ts",
      env: {
        ADMIN_EMAIL: { value: process.env.ADMIN_EMAIL, required: true },
        ADMIN_PASSWORD_HASH: { value: process.env.ADMIN_PASSWORD_HASH, required: true },
        ADMIN_JWT_SECRET: { value: process.env.ADMIN_JWT_SECRET, required: true },
        SCENE_STORAGE: { value: process.env.SCENE_STORAGE ?? "postgres", required: false },
        SCENE_BUCKET: { value: process.env.SCENE_BUCKET ?? "assets", required: false },
      },
    },
  },
  buckets: {
    // "private" is the default; use "public_read" for anonymous reads
    assets: { access: "private" },
  },
  // Branch policy: per-branch tuning
  branch: (branch) => {
    if (branch.isDefault) {
      // Default branch: no overrides, uses project defaults
      return {};
    }
    if (!branch.exists) {
      // New non-default branches: auto-expire
      // Run `neon checkout <name>` to create a new branch with these settings
      return { ttl: "7d" };
    }
    // Existing branch: no changes
    return {};
  },
});

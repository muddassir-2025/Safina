import { serve } from "@hono/node-server";
import { existsSync } from "node:fs";

// Load local env before importing the app: config() validates at import time.
for (const file of [".env.local", ".env"]) {
  if (existsSync(file)) {
    try {
      process.loadEnvFile(file);
    } catch {
      // Ignore unreadable env files; missing variables are reported by config().
    }
  }
}

const { default: app } = await import("./app.js");
const port = Number(process.env.PORT ?? 8080);

serve({ fetch: app.fetch, port }, (info) => {
  console.log(`Safina API listening on http://localhost:${info.port}`);
  console.log(`Scene storage: ${process.env.SCENE_STORAGE ?? "postgres"}`);
});

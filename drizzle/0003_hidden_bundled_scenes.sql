CREATE TABLE IF NOT EXISTS "hidden_bundled_scenes" (
  "key" text PRIMARY KEY NOT NULL,
  "created_at" timestamp with time zone DEFAULT now() NOT NULL
);

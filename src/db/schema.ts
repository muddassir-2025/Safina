import {
  boolean,
  integer,
  jsonb,
  pgTable,
  text,
  timestamp,
  uniqueIndex,
  uuid,
} from "drizzle-orm/pg-core";

/**
 * Application tables. Neon Auth keeps its own users/sessions in the
 * `neon_auth` schema, so `owner_id` / `user_id` hold the Neon Auth `sub`.
 */

export const profiles = pgTable("profiles", {
  userId: text("user_id").primaryKey(),
  email: text("email"),
  channelName: text("channel_name").notNull().unique(),
  /** Moderated: a blocked author's scenes leave the public list. */
  blocked: boolean("blocked").notNull().default(false),
  createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
});

export const publicScenes = pgTable(
  "public_scenes",
  {
    id: uuid("id").primaryKey().defaultRandom(),
    ownerId: text("owner_id").notNull(),
    title: text("title").notNull(),
    /** Inline HTML when the Postgres storage backend is active. */
    html: text("html"),
    /** Object key when the Object Storage backend is active. */
    storageKey: text("storage_key"),
    size: integer("size").notNull(),
    savedCount: integer("saved_count").notNull().default(0),
    /** Admin-published scenes show this instead of a user's channel name. */
    authorLabel: text("author_label"),
    isOfficial: boolean("is_official").notNull().default(false),
    isRemoved: boolean("is_removed").notNull().default(false),
    /** Moderated: the author can no longer delete or replace it. */
    locked: boolean("locked").notNull().default(false),
    removedBy: text("removed_by"),
    removedReason: text("removed_reason"),
    removedAt: timestamp("removed_at", { withTimezone: true }),
    createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
  },
);

export const collections = pgTable(
  "collections",
  {
    id: uuid("id").primaryKey().defaultRandom(),
    userId: text("user_id").notNull(),
    name: text("name").notNull(),
    createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
  },
  (t) => ({
    userCollectionNameIdx: uniqueIndex("collections_user_name_idx").on(t.userId, t.name),
  })
);

export const saves = pgTable(
  "saves",
  {
    id: uuid("id").primaryKey().defaultRandom(),
    userId: text("user_id").notNull(),
    sceneId: uuid("scene_id").notNull(),
    collectionId: uuid("collection_id"),
    createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
  },
  (t) => ({
    userSceneIdx: uniqueIndex("saves_user_scene_idx").on(t.userId, t.sceneId),
  })
);

export const reports = pgTable("reports", {
  id: uuid("id").primaryKey().defaultRandom(),
  sceneId: uuid("scene_id").notNull(),
  reporterId: text("reporter_id").notNull(),
  reason: text("reason").notNull(),
  createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
});

export const defaultScenes = pgTable("default_scenes", {
  id: uuid("id").primaryKey().defaultRandom(),
  title: text("title").notNull(),
  html: text("html").notNull(),
  size: integer("size").notNull(),
  sortOrder: integer("sort_order").notNull().default(0),
  version: integer("version").notNull().default(1),
  isActive: boolean("is_active").notNull().default(true),
  createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
  updatedAt: timestamp("updated_at", { withTimezone: true }).notNull().defaultNow(),
});

export const auditLog = pgTable("audit_log", {
  id: uuid("id").primaryKey().defaultRandom(),
  action: text("action").notNull(),
  targetType: text("target_type").notNull(),
  targetId: text("target_id"),
  actor: text("actor").notNull().default("admin"),
  meta: jsonb("meta"),
  createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
});

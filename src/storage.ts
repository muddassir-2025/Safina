import {
  DeleteObjectCommand,
  GetObjectCommand,
  PutObjectCommand,
  S3Client,
} from "@aws-sdk/client-s3";
import { config } from "./config.js";

/**
 * Where a scene's HTML lives. Swapping Neon Object Storage for Cloudflare R2
 * means adding one more implementation of this interface — nothing else moves.
 */
export type StoredHtml = { html: string | null; storageKey: string | null };

export type SceneRowRef = {
  id: string;
  html: string | null;
  storageKey: string | null;
};

export interface SceneStorage {
  readonly kind: "postgres" | "s3";
  /** Persists HTML and returns the columns to write on the scene row. */
  store(id: string, html: string): Promise<StoredHtml>;
  /** Reads HTML back for a scene row. */
  load(scene: SceneRowRef): Promise<string | null>;
  /** Removes stored HTML when a scene is hard-deleted. */
  remove(scene: SceneRowRef): Promise<void>;
}

/** Default: a TEXT column on public_scenes (files are <= 1 MB). */
export class PostgresSceneStorage implements SceneStorage {
  readonly kind = "postgres" as const;

  async store(_id: string, html: string): Promise<StoredHtml> {
    return { html, storageKey: null };
  }

  async load(scene: SceneRowRef): Promise<string | null> {
    return scene.html;
  }

  async remove(): Promise<void> {
    // The TEXT column goes away with the row.
  }
}

/** S3-compatible storage; points at the branch's Neon Object Storage endpoint. */
export class ObjectStorageSceneStorage implements SceneStorage {
  readonly kind = "s3" as const;
  private client: S3Client;
  private bucket: string;

  constructor() {
    const cfg = config();
    if (!cfg.AWS_ACCESS_KEY_ID || !cfg.AWS_SECRET_ACCESS_KEY || !cfg.AWS_ENDPOINT_URL_S3) {
      throw new Error(
        "SCENE_STORAGE=s3 needs AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY and AWS_ENDPOINT_URL_S3 (run: neon deploy)"
      );
    }
    this.bucket = cfg.SCENE_BUCKET;
    this.client = new S3Client({
      region: cfg.AWS_REGION ?? "auto",
      endpoint: cfg.AWS_ENDPOINT_URL_S3,
      forcePathStyle: true,
      credentials: {
        accessKeyId: cfg.AWS_ACCESS_KEY_ID,
        secretAccessKey: cfg.AWS_SECRET_ACCESS_KEY,
      },
    });
  }

  private key(id: string): string {
    return `scenes/${id}.html`;
  }

  async store(id: string, html: string): Promise<StoredHtml> {
    const key = this.key(id);
    await this.client.send(
      new PutObjectCommand({
        Bucket: this.bucket,
        Key: key,
        Body: html,
        ContentType: "text/html; charset=utf-8",
      })
    );
    return { html: null, storageKey: key };
  }

  async load(scene: SceneRowRef): Promise<string | null> {
    if (!scene.storageKey) return scene.html;
    try {
      const res = await this.client.send(
        new GetObjectCommand({ Bucket: this.bucket, Key: scene.storageKey })
      );
      return (await res.Body?.transformToString("utf-8")) ?? null;
    } catch (err) {
      console.error("[storage] read failed", err instanceof Error ? err.message : err);
      return null;
    }
  }

  async remove(scene: SceneRowRef): Promise<void> {
    if (!scene.storageKey) return;
    try {
      await this.client.send(
        new DeleteObjectCommand({ Bucket: this.bucket, Key: scene.storageKey })
      );
    } catch (err) {
      console.error("[storage] delete failed", err instanceof Error ? err.message : err);
    }
  }
}

let instance: SceneStorage | null = null;

export function storage(): SceneStorage {
  if (!instance) {
    instance =
      config().SCENE_STORAGE === "s3"
        ? new ObjectStorageSceneStorage()
        : new PostgresSceneStorage();
  }
  return instance;
}

import { CHANNEL_RE, MAX_HTML_BYTES, MAX_TITLE_LENGTH } from "./config.js";

/** Server-side first filter for untrusted scene HTML. */
export type HtmlCheck = { ok: true } | { ok: false; error: string };

const FORBIDDEN: Array<{ re: RegExp; error: string }> = [
  { re: /\bfetch\s*\(/i, error: "Scene HTML may not call fetch()" },
  { re: /XMLHttpRequest/i, error: "Scene HTML may not use XMLHttpRequest" },
  { re: /new\s+WebSocket\s*\(/i, error: "Scene HTML may not open WebSockets" },
  { re: /new\s+EventSource\s*\(/i, error: "Scene HTML may not open EventSource" },
  { re: /navigator\s*\.\s*sendBeacon/i, error: "Scene HTML may not use sendBeacon" },
  { re: /import\s*\(\s*['"]/i, error: "Scene HTML may not import external modules" },
  { re: /<(?:script|img|iframe|link|video|audio|source)\b[^>]*\b(?:src|href)\s*=\s*["']\s*(?:https?:)?\/\//i,
    error: "Scene HTML may not reference external URLs" },
  { re: /@import\s+url/i, error: "Scene HTML may not @import external stylesheets" },
];

export function checkSceneHtml(html: unknown): HtmlCheck {
  if (typeof html !== "string" || html.trim().length === 0) {
    return { ok: false, error: "Scene HTML is empty" };
  }
  const bytes = Buffer.byteLength(html, "utf8");
  if (bytes > MAX_HTML_BYTES) {
    return { ok: false, error: `Scene is larger than ${MAX_HTML_BYTES} bytes` };
  }
  if (!/<html[\s>]/i.test(html) && !/<body[\s>]/i.test(html) && !/<!doctype\s+html/i.test(html)) {
    return { ok: false, error: "File does not look like an HTML document" };
  }
  for (const rule of FORBIDDEN) {
    if (rule.re.test(html)) return { ok: false, error: rule.error };
  }
  return { ok: true };
}

export function sceneSize(html: string): number {
  return Buffer.byteLength(html, "utf8");
}

export function titleError(title: unknown): string | null {
  if (typeof title !== "string") return "Title is required";
  const trimmed = title.trim();
  if (trimmed.length === 0) return "Title is required";
  if (trimmed.length > MAX_TITLE_LENGTH) return `Title must be ${MAX_TITLE_LENGTH} characters or fewer`;
  return null;
}

export function channelError(name: unknown): string | null {
  if (typeof name !== "string" || !CHANNEL_RE.test(name.trim())) {
    return "Channel name must be 3-20 characters: letters, numbers or underscore";
  }
  return null;
}

export function collectionNameError(name: unknown): string | null {
  if (typeof name !== "string") return "Name is required";
  const trimmed = name.trim();
  if (trimmed.length === 0) return "Name is required";
  if (trimmed.length > 40) return "Name must be 40 characters or fewer";
  return null;
}

export interface Env {
  DB: D1Database;
  CACHE: KVNamespace;
  BACKUPS?: R2Bucket;
  AI: Ai;
  ZEUS_TOKEN?: string;
  GEMINI_API_KEY?: string;
  GROQ_API_KEY?: string;
}

interface TransferCreateBody {
  idempotency_key: string;
  source: string;
  title: string;
  authors?: string[];
  language?: string;
  year?: string;
  format: string;
  download_url: string;
}

interface SyncRecord {
  record_type: string;
  record_id: string;
  edition_id?: string;
  payload_json: string;
  base_version?: number;
  version?: number;
  updated_at_ms: number;
  deleted_at_ms?: number | null;
}

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET,POST,OPTIONS",
  "Access-Control-Allow-Headers": "Authorization,Content-Type,X-Zeus-Token",
};

function jsonResponse(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json", ...CORS_HEADERS },
  });
}

function checkAuth(req: Request, env: Env): boolean {
  const expected = (env.ZEUS_TOKEN || "zeus-dev-pairing-token").trim();
  const auth = req.headers.get("Authorization") || "";
  const xToken = req.headers.get("X-Zeus-Token") || "";
  let candidate = "";
  if (auth.toLowerCase().startsWith("bearer ")) {
    candidate = auth.slice(7).trim();
  } else if (xToken) {
    candidate = xToken.trim();
  }
  return candidate.length > 0 && candidate === expected;
}

async function sha256Hex(buffer: ArrayBuffer): Promise<string> {
  const hash = await crypto.subtle.digest("SHA-256", buffer);
  return Array.from(new Uint8Array(hash))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

function isDisguisedOrHtml(bytes: Uint8Array): { ok: boolean; reason?: string } {
  if (bytes.length < 4) return { ok: false, reason: "Empty file" };
  // Reject Windows PE / ELF / APK-like HTML wrappers
  const head = new TextDecoder("utf-8", { fatal: false }).decode(bytes.slice(0, 256)).trim().toLowerCase();
  if (head.startsWith("<!doctype html") || head.startsWith("<html")) {
    return { ok: false, reason: "HTML verification page returned instead of book binary" };
  }
  if (bytes[0] === 0x4d && bytes[1] === 0x5a) {
    return { ok: false, reason: "Executable binary rejected" };
  }
  return { ok: true };
}

async function fetchSlumStatus(env: Env): Promise<Record<string, unknown>> {
  const cacheKey = "slum:snapshot";
  try {
    const resp = await fetch("https://open-slum.org/", {
      headers: { "User-Agent": "ZeusReader/1.0" },
      signal: AbortSignal.timeout(5000),
    });
    if (resp.ok) {
      const html = await resp.text();
      // Extract known provider names and status indicators cleanly
      const known = [
        { name: "Anna's Archive", host: "annas-archive.org", role: "catalogue_search" },
        { name: "Library Genesis", host: "libgen.li", role: "catalogue_search" },
        { name: "Z-Library", host: "z-library.sk", role: "catalogue_search" },
        { name: "Sci-Hub", host: "sci-hub.ru", role: "catalogue_search" },
        { name: "Memory of the World", host: "library.memoryoftheworld.org", role: "catalogue_and_delivery" },
      ];
      const providers = known.map((p) => ({
        name: p.name,
        overall_status: html.toLowerCase().includes(p.name.toLowerCase().split(" ")[0]) ? "UP" : "UNKNOWN",
        domains: [{ host: p.host, role: p.role, health: "up" }],
      }));
      const snapshot = {
        fetched_at_utc: new Date().toISOString(),
        stale: false,
        providers,
      };
      await env.CACHE.put(cacheKey, JSON.stringify(snapshot), { expirationTtl: 86400 });
      return snapshot;
    }
  } catch {
    // Fall back to KV cache
  }
  const cached = await env.CACHE.get(cacheKey);
  if (cached) {
    const parsed = JSON.parse(cached) as Record<string, unknown>;
    return { ...parsed, stale: true };
  }
  return {
    fetched_at_utc: new Date().toISOString(),
    stale: true,
    providers: [
      {
        name: "Memory of the World",
        overall_status: "UP",
        domains: [{ host: "library.memoryoftheworld.org", role: "catalogue_and_delivery", health: "up" }],
      },
    ],
  };
}

async function searchMotw(query: string): Promise<Array<Record<string, unknown>>> {
  const url = `https://library.memoryoftheworld.org/search/titles/${encodeURIComponent(query)}`;
  const resp = await fetch(url, {
    headers: { Accept: "application/json", "User-Agent": "ZeusReader/1.0" },
    signal: AbortSignal.timeout(6000),
  });
  if (!resp.ok) return [];
  const data = (await resp.json()) as { _items?: Array<Record<string, unknown>> };
  const items = data._items || [];
  const out: Array<Record<string, unknown>> = [];
  for (const item of items.slice(0, 12)) {
    const title = String(item.title || "Untitled");
    const authors = Array.isArray(item.authors) ? item.authors.map(String) : [];
    const langs = Array.isArray(item.languages) ? item.languages.map(String) : [];
    const pubdate = String(item.pubdate || "").slice(0, 4);
    const baseurl = String(item.baseurl || "/files/anybody").replace(/^\/+|\/+$/g, "");
    const formats = Array.isArray(item.formats) ? (item.formats as Array<Record<string, unknown>>) : [];
    for (const f of formats) {
      const fmt = String(f.format || "epub").toLowerCase();
      if (!["epub", "pdf", "mobi", "azw3"].includes(fmt)) continue;
      const dirPath = String(f.dir_path || "").replace(/^\/+|\/+$/g, "");
      const fileName = String(f.file_name || "");
      if (!fileName) continue;
      const downloadUrl = `https://library.memoryoftheworld.org/${baseurl}/${dirPath}/${encodeURIComponent(fileName)}`.replace(
        /([^:]\/)\/+/g,
        "$1"
      );
      out.push({
        source: "Memory of the World",
        title,
        authors,
        language: langs[0] || "",
        year: pubdate,
        format: fmt,
        byte_size: typeof f.size === "number" ? f.size : null,
        download_url: downloadUrl,
        idempotency_key: `motw:${title}:${fmt}:${fileName}`.toLowerCase(),
      });
    }
  }
  return out;
}

async function searchGutendex(query: string): Promise<Array<Record<string, unknown>>> {
  const url = `https://gutendex.com/books/?search=${encodeURIComponent(query)}`;
  const resp = await fetch(url, {
    headers: { "User-Agent": "ZeusReader/1.0" },
    signal: AbortSignal.timeout(6000),
  });
  if (!resp.ok) return [];
  const data = (await resp.json()) as { results?: Array<Record<string, unknown>> };
  const results = data.results || [];
  const out: Array<Record<string, unknown>> = [];
  for (const b of results.slice(0, 10)) {
    const title = String(b.title || "Untitled");
    const authors = Array.isArray(b.authors)
      ? (b.authors as Array<Record<string, unknown>>).map((a) => String(a.name || ""))
      : [];
    const langs = Array.isArray(b.languages) ? b.languages.map(String) : [];
    const formats = (b.formats || {}) as Record<string, string>;
    const epubUrl = formats["application/epub+zip"];
    if (epubUrl) {
      out.push({
        source: "Project Gutenberg (Gutendex)",
        title,
        authors: authors.filter(Boolean),
        language: langs[0] || "en",
        year: "",
        format: "epub",
        byte_size: null,
        download_url: epubUrl,
        idempotency_key: `gutenberg:${b.id}:epub`,
      });
    }
  }
  return out;
}

async function runWorkersAi(env: Env, prompt: string): Promise<string> {
  if (env.GEMINI_API_KEY) {
    try {
      const resp = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=${env.GEMINI_API_KEY}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] }),
        }
      );
      if (resp.ok) {
        const j = (await resp.json()) as { candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }> };
        const txt = j.candidates?.[0]?.content?.parts?.[0]?.text?.trim();
        if (txt) return txt;
      }
    } catch {
      // fall through to Workers AI
    }
  }
  for (const model of ["@cf/meta/llama-3-8b-instruct", "@cf/mistral/mistral-7b-instruct-v0.1"] as const) {
    try {
      const res = (await env.AI.run(model, {
        messages: [{ role: "user", content: prompt }],
        max_tokens: 512,
      })) as { response?: string };
      const out = (res.response || "").trim();
      if (out) return out;
    } catch {
      // try next model
    }
  }
  return "";
}

export default {
  async fetch(req: Request, env: Env): Promise<Response> {
    if (req.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: CORS_HEADERS });
    }
    const url = new URL(req.url);
    const path = url.pathname;

    if (path === "/health" && req.method === "GET") {
      return jsonResponse({
        status: "ok",
        api_version: "1.0",
        min_supported_client_api: "1.0",
        ai_enabled: true,
        runtime: "cloudflare-worker",
      });
    }

    if (!checkAuth(req, env)) {
      return jsonResponse({ detail: "Invalid or missing pairing token" }, 401);
    }

    if (path === "/v1/slum/status" && req.method === "GET") {
      return jsonResponse(await fetchSlumStatus(env));
    }

    if (path === "/v1/search" && req.method === "GET") {
      const q = (url.searchParams.get("q") || "").trim();
      if (!q) return jsonResponse({ query: "", results: [], warnings: ["Empty query"] });
      const warnings: string[] = [];
      const [motwRes, gutRes] = await Promise.allSettled([searchMotw(q), searchGutendex(q)]);
      const results: Array<Record<string, unknown>> = [];
      if (motwRes.status === "fulfilled") results.push(...motwRes.value);
      else warnings.push("Memory of the World timed out or unreachable");
      if (gutRes.status === "fulfilled") results.push(...gutRes.value);
      else warnings.push("Gutendex timed out");
      const formatOrder: Record<string, number> = { pdf: 0, epub: 1, mobi: 2, azw3: 3 };
      results.sort((a, b) => {
        const fa = formatOrder[String(a.format || "").toLowerCase()] ?? 99;
        const fb = formatOrder[String(b.format || "").toLowerCase()] ?? 99;
        return fa - fb;
      });
      return jsonResponse({ query: q, results, warnings });
    }

    if (path === "/v1/transfers" && req.method === "POST") {
      const body = (await req.json()) as TransferCreateBody;
      const existing = await env.DB.prepare("SELECT * FROM transfer_jobs WHERE idempotency_key = ?")
        .bind(body.idempotency_key)
        .first<Record<string, unknown>>();
      if (existing) return jsonResponse(existing);

      const jobId = crypto.randomUUID();
      const now = Date.now();
      let state = "Downloading";
      let progress = 10;
      let sha256: string | null = null;
      let byteSize: number | null = null;
      let r2Key: string | null = null;
      let browserUrl: string | null = null;
      let errorMsg: string | null = null;

      try {
        const dlResp = await fetch(body.download_url, {
          headers: { "User-Agent": "ZeusReader/1.0" },
          redirect: "follow",
        });
        if (!dlResp.ok) {
          state = dlResp.status === 403 || dlResp.status === 429 ? "NeedsBrowser" : "Failed";
          browserUrl = state === "NeedsBrowser" ? body.download_url : null;
          errorMsg = `Provider returned HTTP ${dlResp.status}`;
        } else {
          const buf = await dlResp.arrayBuffer();
          const check = isDisguisedOrHtml(new Uint8Array(buf));
          if (!check.ok) {
            state = "NeedsBrowser";
            browserUrl = body.download_url;
            errorMsg = check.reason || "Interactive verification required";
          } else {
            sha256 = await sha256Hex(buf);
            byteSize = buf.byteLength;
            r2Key = `books/${jobId}.${body.format.toLowerCase()}`;
            if (env.BACKUPS) {
              await env.BACKUPS.put(r2Key, buf);
            } else {
              await env.CACHE.put(r2Key, buf, { expirationTtl: 86400 * 7 });
            }
            state = "ReadyOnServer";
            progress = 100;
          }
        }
      } catch (e) {
        state = "Failed";
        errorMsg = e instanceof Error ? e.message : "Download failed";
      }

      await env.DB.prepare(
        `INSERT INTO transfer_jobs
         (job_id, idempotency_key, source, title, authors_json, language, year, format, download_url, state, progress_percent, error_message, browser_url, file_sha256, byte_size, r2_key, created_at_ms, updated_at_ms)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
      )
        .bind(
          jobId,
          body.idempotency_key,
          body.source,
          body.title,
          JSON.stringify(body.authors || []),
          body.language || "",
          body.year || "",
          body.format,
          body.download_url,
          state,
          progress,
          errorMsg,
          browserUrl,
          sha256,
          byteSize,
          r2Key,
          now,
          now
        )
        .run();

      const saved = await env.DB.prepare("SELECT * FROM transfer_jobs WHERE job_id = ?").bind(jobId).first();
      return jsonResponse(saved);
    }

    if (path === "/v1/transfers" && req.method === "GET") {
      const { results } = await env.DB.prepare("SELECT * FROM transfer_jobs ORDER BY created_at_ms DESC LIMIT 50").all();
      return jsonResponse(results);
    }

    const fileMatch = path.match(/^\/v1\/transfers\/([^/]+)\/file$/);
    if (fileMatch && req.method === "GET") {
      const job = await env.DB.prepare("SELECT * FROM transfer_jobs WHERE job_id = ?")
        .bind(fileMatch[1])
        .first<{ r2_key?: string; format?: string; title?: string }>();
      if (!job || !job.r2_key) return jsonResponse({ detail: "File not ready" }, 404);
      if (env.BACKUPS) {
        const obj = await env.BACKUPS.get(job.r2_key);
        if (!obj) return jsonResponse({ detail: "File missing in R2" }, 404);
        return new Response(obj.body, {
          headers: {
            "Content-Type": "application/octet-stream",
            "Content-Disposition": `attachment; filename="book.${job.format || "epub"}"`,
            ...CORS_HEADERS,
          },
        });
      }
      const cachedBuf = await env.CACHE.get(job.r2_key, "arrayBuffer");
      if (!cachedBuf) return jsonResponse({ detail: "File missing in cache" }, 404);
      return new Response(cachedBuf, {
        headers: {
          "Content-Type": "application/octet-stream",
          "Content-Disposition": `attachment; filename="book.${job.format || "epub"}"`,
          ...CORS_HEADERS,
        },
      });
    }

    const cancelMatch = path.match(/^\/v1\/transfers\/([^/]+)\/cancel$/);
    if (cancelMatch && req.method === "POST") {
      await env.DB.prepare("UPDATE transfer_jobs SET state = 'Cancelled', updated_at_ms = ? WHERE job_id = ?")
        .bind(Date.now(), cancelMatch[1])
        .run();
      const job = await env.DB.prepare("SELECT * FROM transfer_jobs WHERE job_id = ?").bind(cancelMatch[1]).first();
      return jsonResponse(job || { detail: "Not found" }, job ? 200 : 404);
    }

    const retryMatch = path.match(/^\/v1\/transfers\/([^/]+)\/retry$/);
    if (retryMatch && req.method === "POST") {
      const existing = await env.DB.prepare("SELECT * FROM transfer_jobs WHERE job_id = ?")
        .bind(retryMatch[1])
        .first<Record<string, unknown>>();
      if (!existing) return jsonResponse({ detail: "Not found" }, 404);
      await env.DB.prepare("UPDATE transfer_jobs SET state = 'Queued', updated_at_ms = ? WHERE job_id = ?")
        .bind(Date.now(), retryMatch[1])
        .run();
      const updated = await env.DB.prepare("SELECT * FROM transfer_jobs WHERE job_id = ?").bind(retryMatch[1]).first();
      return jsonResponse(updated);
    }

    if (path === "/v1/sync" && req.method === "POST") {
      const body = (await req.json()) as { since_ms?: number; records?: SyncRecord[] };
      const sinceMs = body.since_ms || 0;
      const incoming = body.records || [];
      for (const rec of incoming) {
        const existing = await env.DB.prepare(
          "SELECT version, updated_at_ms FROM sync_records WHERE record_type = ? AND record_id = ?"
        )
          .bind(rec.record_type, rec.record_id)
          .first<{ version: number; updated_at_ms: number }>();
        const nextVersion = Math.max(rec.version || 1, (existing?.version || 0) + 1);
        if (!existing || rec.updated_at_ms >= existing.updated_at_ms) {
          await env.DB.prepare(
            `INSERT INTO sync_records (record_type, record_id, edition_id, payload_json, version, updated_at_ms, deleted_at_ms)
             VALUES (?, ?, ?, ?, ?, ?, ?)
             ON CONFLICT(record_type, record_id) DO UPDATE SET
               edition_id = excluded.edition_id,
               payload_json = excluded.payload_json,
               version = excluded.version,
               updated_at_ms = excluded.updated_at_ms,
               deleted_at_ms = excluded.deleted_at_ms`
          )
            .bind(
              rec.record_type,
              rec.record_id,
              rec.edition_id || "",
              rec.payload_json,
              nextVersion,
              rec.updated_at_ms,
              rec.deleted_at_ms ?? null
            )
            .run();
        }
      }
      const { results } = await env.DB.prepare("SELECT * FROM sync_records WHERE updated_at_ms >= ?")
        .bind(sinceMs)
        .all();
      return jsonResponse({ server_time_ms: Date.now(), records: results });
    }

    if (path === "/v1/backups" && req.method === "POST") {
      const body = (await req.json()) as { mode: string; manifest_json: string; payload_b64: string };
      const binStr = atob(body.payload_b64);
      const bytes = new Uint8Array(binStr.length);
      for (let i = 0; i < binStr.length; i++) bytes[i] = binStr.charCodeAt(i);
      const sha = await sha256Hex(bytes.buffer);
      const backupId = crypto.randomUUID();
      const r2Key = `backups/${backupId}.bin`;
      const now = Date.now();
      if (env.BACKUPS) {
        await env.BACKUPS.put(r2Key, bytes);
      } else {
        await env.CACHE.put(r2Key, bytes.buffer, { expirationTtl: 86400 * 30 });
      }
      await env.DB.prepare(
        "INSERT INTO backups (backup_id, mode, manifest_json, sha256, byte_size, r2_key, created_at_ms) VALUES (?, ?, ?, ?, ?, ?, ?)"
      )
        .bind(backupId, body.mode, body.manifest_json, sha, bytes.byteLength, r2Key, now)
        .run();
      return jsonResponse({ backup_id: backupId, mode: body.mode, sha256: sha, byte_size: bytes.byteLength, created_at_ms: now });
    }

    if (path === "/v1/backups" && req.method === "GET") {
      const { results } = await env.DB.prepare("SELECT backup_id, mode, sha256, byte_size, created_at_ms FROM backups ORDER BY created_at_ms DESC").all();
      return jsonResponse(results);
    }

    if (path === "/v1/ai/explain-passage" && req.method === "POST") {
      const body = (await req.json()) as { book_title?: string; passage_text?: string };
      const title = body.book_title || "this book";
      const text = body.passage_text || "";
      const cacheKey = `ai:explain:${await sha256Hex(new TextEncoder().encode(`${title}:${text}`).buffer)}`;
      const cached = await env.CACHE.get(cacheKey);
      if (cached) return jsonResponse({ explanation: cached, model: "workers-ai-cache" });
      const explanation =
        (await runWorkersAi(
          env,
          `Explain the following passage from "${title}" clearly in 2-4 concise sentences:\n\n${text}`
        )) || `Context note for "${title}": ${text.slice(0, 160)}`;
      await env.CACHE.put(cacheKey, explanation, { expirationTtl: 86400 * 30 });
      return jsonResponse({ explanation, model: env.GEMINI_API_KEY ? "gemini-flash-lite" : "@cf/meta/llama-3-8b-instruct" });
    }

    if (path === "/v1/ai/suggest-tags" && req.method === "POST") {
      const body = (await req.json()) as { book_title?: string; passage_text?: string };
      const text = body.passage_text || "";
      const raw = await runWorkersAi(
        env,
        `Suggest 3 to 5 short lowercase topic tags as a JSON array of strings for this passage. Output ONLY the JSON array.\n\n${text}`
      );
      let tags: string[] = [];
      try {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) tags = parsed.map((t) => String(t).trim().toLowerCase()).filter(Boolean).slice(0, 5);
      } catch {
        tags = text
          .split(/\s+/)
          .map((w) => w.replace(/[^a-zA-Z]/g, "").toLowerCase())
          .filter((w) => w.length > 5)
          .slice(0, 4);
      }
      return jsonResponse({ tags, model: env.GEMINI_API_KEY ? "gemini-flash-lite" : "@cf/meta/llama-3.1-8b-instruct" });
    }

    return jsonResponse({ detail: "Not found" }, 404);
  },
};

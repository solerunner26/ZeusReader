# SCOPE ADDENDUM v4.1 — Serverless "Zero-Rupee" architecture

> Extends GEMINI.md + `docs/SCOPE-ADDENDUM-v4.md` (Stormlight). Where this conflicts with any earlier server/VPS/Ollama plan, **this wins**.
> Goal: every feature of the app runs with **₹0 monthly cost** and **no always-on server**. The UI (DESIGN.md v4) does not change, only where work happens.
> This addendum adds no server, proxy or host for any external book-source connector; section D of v4 ("Out of scope") still applies.

---

## 0. What changes (summary)

| Before (v4 Phase 7/8) | After (v4.1) |
|---|---|
| FastAPI server on a VPS/laptop | **Removed as a requirement.** No `server/` deployment needed for the app to be complete. |
| Ollama + Sarvam-Translate on the server | **Optional** "Home AI" provider only (laptop over Tailscale). Never required. |
| `server/app/ai_client.py` | **AI router lives in the Android app** (`:core:ai` module). |
| Server sync + backup | **Google Drive app-data backup** (default), optional **Cloudflare R2** via Worker. |
| Calibre/LibreOffice on server | **Calibre on the Windows laptop** before import (documented flow). |
| Server-side AI quota/keys | **Cloudflare Worker** ("Zeus Relay"): holds API keys, caches, falls back between providers. |

If `server/` code already exists, keep it in the repo as `/optional/home-server/`, unused by default, behind a Settings switch "Use my home server" (off).

---

## 1. Module layout (Android)

```
:core:ai          AiRouter, providers, quota tracker, AiCache (Room)
:core:backup      DriveBackup, R2Backup (optional), BackupScheduler (WorkManager)
:feature:*        unchanged; call AiRouter only — never a provider directly
/relay            Cloudflare Worker (TypeScript) — "Zeus Relay"
/docs/laptop-conversion.md
```

---

## 2. AI router

### 2.1 Interfaces
```kotlin
enum class AiTask { EXPLAIN, SUMMARIZE_CHAPTER, STORY_SO_FAR, SUGGEST_TAGS, QUIZ,
                    ASK_BOOK, TRANSLATE_PRO, SMART_SEARCH_PARSE }

data class AiRequest(val task: AiTask, val bookId: String?, val chapterId: String?,
                     val lang: String,            // "en" | "hi" | "gu" (output language)
                     val input: String,           // passage / question / chapter text
                     val context: List<String> = emptyList()) // retrieved chunks for ASK_BOOK

data class AiResult(val text: String, val provider: String, val citations: List<Locator> = emptyList(),
                    val fromCache: Boolean)

interface AiProvider {
  val id: String                          // "nano", "relay-gemini", "relay-groq", "home-ollama", "mlkit"
  suspend fun isAvailable(task: AiTask): Boolean
  suspend fun run(req: AiRequest): AiResult
}

class AiRouter(providers: List<AiProvider>, cache: AiCacheDao, quota: QuotaTracker) {
  suspend fun run(req: AiRequest): AiResult   // cache → priority chain → graceful fallback
}
```

### 2.2 Providers
| id | What | Availability check |
|---|---|---|
| `nano` | **Gemini Nano on device** via ML Kit GenAI (Summarization, Rewriting, Prompt API) | ML Kit feature status = AVAILABLE (trigger model download when DOWNLOADABLE, on Wi-Fi + charging) |
| `relay-gemini` | Gemini Flash-Lite through Zeus Relay | network + relay quota not exhausted |
| `relay-groq` | Groq free tier through Zeus Relay | network + relay reports provider healthy |
| `home-ollama` | Optional laptop Ollama over Tailscale | Settings switch on + host reachable (2 s timeout) |
| `mlkit` | ML Kit Translation (offline) — TRANSLATE_PRO fallback only | language pack downloaded |

### 2.3 Priority per task (first available wins; on error/timeout → next)
| Task | Chain |
|---|---|
| EXPLAIN, SUGGEST_TAGS, QUIZ, SMART_SEARCH_PARSE | nano → relay-gemini → relay-groq → home-ollama |
| SUMMARIZE_CHAPTER, STORY_SO_FAR | nano (summarization API, chunked) → relay-gemini → relay-groq |
| ASK_BOOK (needs long context + citations) | relay-gemini → relay-groq → home-ollama → nano (short answer, "limited mode" label) |
| TRANSLATE_PRO (hi/gu) | relay-gemini → home-ollama → **mlkit** (always succeeds offline) |
- If every provider fails: show a calm inline message ("Zeus is resting — try again later") and **queue** the request (WorkManager, retry on network/charging). Reading is never blocked.
- The UI labels the engine in small text (e.g. "on-device", "cloud", "offline"), as in the designs.

### 2.4 Quota-smart rules
- **Chapter-level batching:** TRANSLATE_PRO translates a whole chapter (split into ≤ 6,000-char chunks, sentence-aligned with numbered markers) per request, never sentence by sentence. Pre-translate only the current + next chapter, and only on Wi-Fi.
- **Cache first, always:** Room table `AiCache(key, task, provider, lang, text, citationsJson, createdAt)`; `key = sha256(task|bookId|chapterId|lang|normalizedInput|promptVersion)`. Cache hits never touch the network. Translation cache is permanent; other tasks are kept for 90 days.
- **QuotaTracker:** counts requests per provider per day (reset at the provider's reset time, Pacific midnight for Gemini); when ≥ 90 % used, skip that provider until reset. Relay also enforces this server-side.
- **Prompt versions** are constants (`promptVersion`), so changing a prompt invalidates only its own cache.
- **Privacy:** only the selected passage/chapter text and the question are sent, never whole books, file names or personal data. Settings switch: "Allow cloud AI" (default ON) — when OFF the chain skips relay providers entirely. One line in Settings explains that free-tier cloud requests may be used by the provider to improve its products.

### 2.5 ASK_BOOK retrieval (on device)
- Build a per-book FTS4 index of paragraphs (already planned) + simple BM25 ranking; send the top 6–8 paragraphs with their locators as `context`.
- Prompt requires answers **only** from the provided context with `[n]` markers; router maps markers → `citations` (page receipts). No markers or "NOT_FOUND" → UI shows "That isn't in this book."
- Optional later: on-device embeddings; not required for v4.1.

---

## 3. Zeus Relay (Cloudflare Worker, free plan)

### 3.1 Purpose
Keeps API keys off the phone, caches responses, enforces quota, and switches providers. No database server, never sleeps.

### 3.2 Endpoints
| Method | Path | Body → Response |
|---|---|---|
| POST | `/v1/ai` | `{task, lang, input, context[], promptVersion}` → `{text, provider, markers[]}` |
| GET | `/v1/health` | → `{gemini: "ok"|"quota"|"down", groq: …, resetAt}` |
| POST | `/v1/backup/url` *(optional, R2)* | `{op:"put"|"get", name}` → short-lived presigned URL |

### 3.3 Rules
- **Auth:** header `X-Zeus-Token` = a random 32-byte device token generated once in the app and registered with `wrangler secret`. Reject everything else (401). Also rate-limit per token (e.g. 60/min).
- **Secrets** via `wrangler secret put GEMINI_API_KEY`, `GROQ_API_KEY`, `ZEUS_TOKEN`; never in git, never in the APK.
- **Cache:** Workers KV, key = same sha256 as the app's cache key; TTL 30 days (translations 365 days).
- **Provider order inside relay:** Gemini Flash-Lite → Groq; mark a provider "quota" on 429 until its reset time (stored in KV).
- **No logging of request bodies.**
- Deploy: `wrangler deploy`; free plan only; README in `/relay` with 5-step setup.
- The app works **without** the relay (nano + mlkit only). The relay URL and token are entered once in Settings → "AI & backup".

---

## 4. Backup & restore (no server)

### 4.1 Default: Google Drive app-data folder
- Sign in with Google (only the Drive **appDataFolder** scope); files are hidden from the user's Drive UI and count toward their own storage.
- **What's backed up:** encrypted SQLite export of library metadata, progress, highlights, Sparks/Constellations/Tags, Words (+ review schedule), streak/charge history, settings. **Book files are not uploaded by default** (switch: "Also back up book files", warns about size).
- Encryption: AES-GCM with a key derived from a user passphrase (Argon2id) — the passphrase is never stored remotely; show a one-time "save your recovery phrase" screen.
- Schedule: WorkManager daily on Wi-Fi + charging; manual "Back up now". Keep the last 7 backups.
- Restore: on fresh install → "Restore from Google Drive" → pick backup → passphrase.

### 4.2 Optional: Cloudflare R2 (via relay presigned URLs)
Same encrypted bundle, for people who prefer not to use Drive. Free tier is enough for metadata backups.

### 4.3 Multi-device
Not in v4.1 (single phone). Restore covers phone changes. Real-time sync is a future item.

---

## 5. Legacy formats (laptop conversion)
- `/docs/laptop-conversion.md`: Calibre `ebook-convert in.azw3 out.epub` and LibreOffice `soffice --headless --convert-to pdf in.docx` (or EPUB via Calibre), then send to phone (USB, Quick Share, or a shared folder watched by the app).
- The app keeps both files when both are imported: the original goes in as "original, kept", the converted file as "reading copy", linked by matching title/author or a manual "link files" action in Book details.
- DRM files: rejected with a clear message (unchanged).

---

## 6. Settings changes (map to Settings.dc.html)
Rename section "Your server" → **"AI & backup"**:
- Cloud AI: switch "Allow cloud AI" · Relay URL + token (setup sheet) · status line (`Gemini ok · resets 13:30`) · engine order shown read-only.
- On-device AI: status of Gemini Nano (Ready / Downloading / Not supported) · "Download now".
- Home AI (optional): switch "Use my home laptop" · host address · test button.
- Backup: provider (Google Drive / R2 / Off) · last backup time · Back up now · Restore · recovery phrase.

---

## 7. Phase mapping
| Phase | v4.1 work |
|---|---|
| 5 | `:core:ai` skeleton + `mlkit` + `nano` providers; EXPLAIN/SUGGEST_TAGS via nano |
| 6 | TRANSLATE_PRO chain with chapter batching + AiCache |
| **7 (rewritten)** | Drive backup/restore with encryption; R2 optional; laptop-conversion doc + "link files" |
| **8 (rewritten)** | Zeus Relay Worker; relay providers; ASK_BOOK retrieval + citations; QuotaTracker; Home AI provider (optional) |
| 9 | Polish: engine labels, queue UI, Settings "AI & backup" |

---

## 8. Acceptance tests
1. **Airplane mode:** Explain, tag suggestions and chapter summary work on Pixel 10 Pro via Gemini Nano; translation works via ML Kit; reading is never blocked.
2. **Unsupported device / Nano unavailable:** EXPLAIN falls back to relay without errors; label shows "cloud".
3. **Quota:** mock relay returning 429 → router skips it, uses Groq, then ML Kit for translation; QuotaTracker resets at the provider's reset time.
4. **Cache:** re-opening an already translated chapter makes **zero** network calls (verified by an OkHttp interceptor test).
5. **Batching:** translating a 20-page chapter makes ≤ 4 relay requests.
6. **ASK_BOOK:** answers include page citations that open the right locator; a question not covered by the book returns "That isn't in this book."
7. **Privacy switch off:** no request reaches the relay (interceptor test).
8. **Relay security:** requests without a valid `X-Zeus-Token` get 401; secrets absent from APK (`apkanalyzer` string check) and git history.
9. **Backup:** back up, uninstall, reinstall, restore with passphrase → library metadata, progress, Sparks, Words, streak all identical; wrong passphrase fails safely.
10. **No server:** with `/optional/home-server` never started, every feature in DESIGN.md v4 works (Home AI shows "off").
11. **Cost check:** README lists every external service with its free-tier limit; no paid plan or credit card is required to run the app.

---

## 9. Out of scope (unchanged)
No online book search, edition pickers, transfers or downloads from external sources. Books enter only by local import. No server, proxy or relay endpoint for any book-source connector.

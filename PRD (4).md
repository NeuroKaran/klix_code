# Product Requirements Document
## AI Agent Coding Platform for Android
*Working title — rename once you've settled on a brand.*

**Status:** Draft v0.1
**Owner:** Karan
**Last updated:** 2026-09-12

---

## 1. Overview

An Android-native application that brings agentic, AI-assisted software development to a phone or tablet — conceptually similar to VS Code + Cursor/Antigravity, but built mobile-first. The user supplies their own API key for a model provider of their choice (Anthropic, OpenAI, Google, or an OpenAI-compatible endpoint). The app supplies the harness: the editor, the agent orchestration loop, tool-calling, and a local execution environment where the agent's tool calls actually run.

**Core positioning:** *bring your own model, we own the harness.* The app never sees or relays the user's API key or code through our own servers — every model call goes client-to-provider directly.

---

## 2. Problem & opportunity

Agentic coding tools (Cursor, Antigravity, Claude Code, Cline) have made "describe the feature, review the diff, ship it" a mainstream workflow — but only on desktop. There is currently no mature, general-purpose equivalent for Android that combines a real code editor, a real agent harness, and real (if constrained) code execution in one BYOK app. The opportunity is to be first to a genuinely capable mobile agentic IDE, not a chat-only "AI assistant" bolted onto a text viewer.

---

## 3. Target users

- Developers who want to review agent-generated diffs, fix small bugs, or ship quick features from a phone/tablet when away from a laptop.
- Developers already using Cursor/Claude Code/Antigravity on desktop who want a companion mobile client for the same BYOK workflow.
- Hobbyists and students who own a phone but not a personal computer, and want a real (if limited) place to write and run code.

---

## 4. Product principles

1. **BYOK, always.** The user's API key, and the model it unlocks, is theirs. We are a client, not a gateway or a reseller of inference.
2. **The harness is the product.** Editor quality, tool-calling reliability, diff review UX, and context management are where we compete — not the model.
3. **Local-first, offline-capable.** v1 assumes no server-side component beyond the model provider itself. Everything else — files, git history, sandbox — lives on-device.
4. **Trust through architecture, not policy.** "We never see your code or your key" should be true because of how the app is built, not just a line in a privacy policy.
5. **Honest about mobile constraints.** Don't pretend a phone is a workstation. Set expectations (project size, execution tier limits) clearly in-product rather than failing silently.

---

## 5. V1 scope decisions

These were decided during technical scoping and constrain everything below:

| Decision | v1 choice | Deferred to later |
|---|---|---|
| Code execution | **Local sandbox only**, fully offline | Remote/SSH execution tier, Android 16 native Linux VM tier |
| Distribution | **Sideload APK + F-Droid** | Google Play Store submission |
| Harness architecture | **All-Kotlin**, single codebase | Shared Rust/KMP core for future iOS/desktop portability |

Rationale: local-only execution removes the network-dependency and remote-target-provisioning problem entirely for v1, letting the harness and editor quality be judged on their own merits. Sideload/F-Droid distribution sidesteps Play's historical friction with apps that execute downloaded code at runtime (see Termux's own history) and removes launch-blocking policy risk. All-Kotlin keeps the team (of one, for now) inside a single toolchain and ships faster; portability is a real but explicitly later concern.

---

## 6. Core features

### 6.1 Workspace & file management
- Open a project folder via Storage Access Framework (SAF) with persisted URI permission — no broad filesystem permission requested.
- File tree view: create, rename, move, delete files/folders.
- Recent projects list on app launch.
- **Acceptance criteria:** a user can open an existing folder of code, see its file tree, and have that access persist across app restarts without re-granting permission.

### 6.2 Code editor
- Built on Sora Editor (Rosemoe/sora-editor): incremental syntax highlighting via TextMate grammars, Tree-sitter-backed parsing, LSP client support.
- Multi-tab editing, find/replace, basic code folding.
- Diff view for reviewing agent-proposed changes before they're written to disk.
- **Acceptance criteria:** editing a 1,000+ line file stays responsive (no visible input lag) on a mid-range device; at least JS/TS, Python, and Kotlin have working syntax highlighting at launch.

### 6.3 Agent harness & chat
- Chat-style panel for describing tasks in natural language.
- Plan-then-execute mode: for non-trivial requests, the agent proposes a plan (files it intends to touch, commands it intends to run) before acting, mirroring the pattern used by Cursor/Claude Code/Antigravity.
- A normalized internal tool schema (`read_file`, `write_file`, `edit_patch`, `run_command`, `search_code`, `git_status`, `git_diff`, `git_commit`, …) translated to and from each provider's native tool-calling format.
- Session context management: the harness decides how much file content, diff history, and prior tool output gets serialized into each turn, with a visible indicator of context usage.
- **Acceptance criteria:** a user can ask for a small, well-specified feature and get a working plan → diff → apply flow without leaving the app.

### 6.4 Tool execution & approval flow
- Every tool call the agent wants to make is either auto-approved (per user-configured policy, e.g. "always allow read_file") or surfaced for explicit approval before it runs.
- Destructive or irreversible actions (file overwrite, `git push`, `rm`) always require explicit approval regardless of policy settings.
- Full transcript of tool calls and their results, reviewable after the fact.
- **Acceptance criteria:** a user can see exactly what command is about to run and approve/deny it before it executes; no tool call silently mutates files without a corresponding diff being shown first.

### 6.5 Local execution sandbox (v1's only execution tier)
- A bundled, unrooted Linux userland (Alpine, via `proot`) that the agent's `run_command` tool calls execute inside — the same underlying technique Termux uses.
- Common interpreters/toolchains available out of the box or installable via the sandbox's package manager (Node.js, Python, git, common build tools).
- Clear in-app messaging about what this tier can and can't do (no GPU workloads, limited to what fits in the phone's storage/RAM, some `glibc`-linked packages may need extra setup) so users don't hit silent failures.
- **Acceptance criteria:** a user can run `npm install && npm test` (or the Python equivalent) on a small-to-medium project entirely on-device, offline, with output streamed back into the chat/terminal.

### 6.6 Terminal
- A standalone terminal view into the same local sandbox, independent of the agent — for manual commands, inspecting sandbox state, or debugging what the agent did.
- **Acceptance criteria:** a user can open a shell in the sandbox and it reflects the exact same filesystem the agent's tool calls operate on.

### 6.7 Git integration
- Implemented via JGit (pure JVM, no native binary/NDK dependency).
- Clone, stage, commit, view diff/log, branch, and push/pull (with credentials stored the same way as API keys — Keystore-backed).
- **Acceptance criteria:** a user can clone a repo, have the agent make changes, review the diff, and commit — all without touching the sandbox terminal.

### 6.8 Provider & API key management
- Settings screen to add/remove keys for Anthropic, OpenAI, Google Gemini, and any OpenAI-compatible endpoint (OpenRouter, self-hosted, etc.).
- Keys stored via Android Keystore-backed encrypted storage; never transmitted anywhere except directly to the provider's own API.
- Per-project or global default model selection.
- **Acceptance criteria:** a user can paste in a key, select it as active, and have every subsequent model call go straight from the device to that provider's endpoint — verifiable via network inspection that no other host is contacted with the key or code.

### 6.9 Background execution & notifications
- Long-running agent tasks continue via a foreground service with a persistent notification showing current state (running a command / awaiting approval / done).
- Battery-optimization-exemption prompt shown once, explained in plain language (why: Doze/App Standby will otherwise pause a multi-minute agent run).
- **Acceptance criteria:** starting a multi-step agent task, locking the phone, and returning 5 minutes later shows the task still progressed (or is clearly paused with a reason shown), not silently killed.

---

## 7. System architecture

At a high level, the app has three internal components:

- **Code editor** (Sora Editor) — the surface the user directly edits.
- **Agent harness** — orchestration loop, provider adapter, tool-calling schema, approval layer. This is the component with the most product differentiation.
- **Execution layer** — receives tool calls that need to actually *do* something (run a command, install a package) and executes them.

The harness talks outward to LLM provider APIs using the user's key. The execution layer talks to the local sandbox.

Even though v1 ships with only one execution implementation, the execution layer should be built behind a single internal interface (e.g. an `ExecutionProvider` interface with one `LocalSandboxProvider` implementation) rather than hard-wired to `proot` calls throughout the codebase. This costs little now and is what makes a future remote-execution or Android-16-VM tier an additive change instead of a rewrite.

---

## 8. Tech stack

| Layer | Choice | Why |
|---|---|---|
| App language/UI | Kotlin + Jetpack Compose | Deepest access to the OS APIs this app depends on (foreground services, SAF, process management); single toolchain for a solo/small team |
| Code editor | Sora Editor (`Rosemoe/sora-editor`) | Actively maintained, TextMate + Tree-sitter highlighting, built-in LSP client, purpose-built for Android |
| Agent harness | Custom, all-Kotlin | Normalized tool-calling schema translated per-provider; this is the core IP |
| Local execution | Bundled Alpine userland via `proot` | Same proven approach as Termux; no root required; fully offline |
| Git | JGit | Pure JVM, no native binary or NDK dependency |
| API key storage | Android Keystore-backed encrypted storage | Client-side only, never relayed through app servers |
| Storage | Storage Access Framework (persisted URI permissions) | Required under scoped storage; user grants access per project folder |
| Background execution | Foreground service + notification, battery-optimization-exemption prompt | Survives Doze/App Standby for multi-minute agent runs |

---

## 9. Non-functional requirements

- **Security/privacy:** no server-side component that sees API keys or source code; all model calls are client-to-provider; all secrets Keystore-encrypted at rest.
- **Offline capability:** editing, git operations, and sandbox execution all work with no network connection; only model calls require connectivity.
- **Performance:** editor stays responsive on mid-range Android hardware (no strict device list yet — needs a target device tier defined during design).
- **Battery/thermal:** visible time/step budget for agent runs; one-tap interrupt at all times; no silent background drain.
- **Storage footprint:** the bundled Linux rootfs should be downloaded on first use rather than bundled in the APK, to keep initial install size reasonable.

---

## 10. Out of scope for v1 / future roadmap

- Remote execution tier (SSH to a user-owned devbox, or a cloud sandbox) for projects too large or too toolchain-heavy for on-device execution.
- Opportunistic use of Android 16's native Linux VM (AVF/pKVM) as a higher-fidelity local execution tier where available — currently gated to specific devices and Developer Options, so treated as a future enhancement rather than a v1 dependency.
- Google Play Store distribution, once the sandbox/execution approach has been reviewed against current Play policy.
- Shared Rust/Kotlin Multiplatform core, if/when iOS or desktop clients are considered.
- Multi-agent / parallel task orchestration (running more than one agent task concurrently).
- Team/collaboration features (shared sessions, multi-user projects).

---

## 11. Risks & mitigations

| Risk | Mitigation |
|---|---|
| `proot`'s ptrace-based syscall emulation is slow for CPU-heavy builds | Set expectations in-product about what the sandbox is good for; keep the execution interface pluggable for a future faster tier |
| Android's phantom-process killer terminates long-running sandbox processes | Foreground service + tuned process priority; surface clear "task was interrupted by the OS" messaging rather than a silent hang |
| Sideload/F-Droid-only distribution limits discoverability vs. Play Store | Accepted trade-off for v1; revisit once sandbox approach is validated against Play policy |
| Multi-provider tool-calling format drift (Anthropic/OpenAI/Gemini each evolve independently) | Adapter layer isolated behind the normalized tool schema; provider-specific code changes shouldn't ripple into the harness or UI |
| Users misunderstand what "local sandbox" can't do (no GPU, limited toolchains) and file bugs against inherent limits | Explicit in-app messaging on sandbox capabilities, not just a README note |

---

## 12. Success metrics (draft — refine once v1 scope is locked)

- Time from "open app" to "first successful agent-applied diff" for a new user.
- % of agent tool calls that succeed without user intervention/correction.
- Session length and return-usage rate (proxy for whether mobile agentic coding is actually sticky, vs. a novelty).
- % of `run_command` calls that succeed on first try in the local sandbox (proxy for sandbox reliability).

---

## 13. Suggested phasing

1. **Editor + file management** — Sora Editor integrated, SAF-based project opening, file tree. No agent yet.
2. **Harness v0** — single-provider (start with one, e.g. Anthropic) tool-calling loop, manual approval on every tool call, no plan mode yet.
3. **Local sandbox** — `proot` + Alpine integration, `run_command` wired into the harness, standalone terminal view.
4. **Git integration** — JGit-based clone/commit/diff/push-pull.
5. **Multi-provider support** — provider adapter generalized beyond the first provider; key management UI.
6. **Polish pass** — plan-then-execute mode, context usage visibility, background execution/notifications, battery-exemption flow.
7. **Sideload/F-Droid release candidate.**

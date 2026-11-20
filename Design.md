# Klix Code (v0.4.0+) — Official UI/UX Design System (`Design.md`)

> **Design Style Name**: **Borderless Soft-Tonal Bento UI** *(Cozy Tactile Neo-Bento)*
> **Target Stack**: Kotlin + Jetpack Compose (Material 3)
> **Core Rule**: Zero `1.dp` outline borders on buttons or inner cards. Visual hierarchy is achieved purely through layered tonal surfaces, generous `22.dp–28.dp` corner radii, full-pill CTAs, expressive typography, and real 2D Klix Bot mascot assets.

---

## 1. Strict Visual & Engineering Rules

1. **Preserve Business Logic & Zero Mock Data**:
   - Keep all existing ViewModel `StateFlow`s, coroutine scopes, `libproot.so` / `SandboxManager` bridges, Sora Editor bindings, Android Keystore encryption, and `agent_sessions.json` persistence intact.
   - All onboarding and settings actions MUST execute real operations (real Alpine RootFS & `apk add` provisioning, real Keystore key storage, real `DataStore` persistence)—never static or mocked states.
2. **Borderless Soft-Tonal Surfaces (Zero Outlines)**:
   - Do **not** add `1.dp` grey/colored strokes around buttons, provider pills, or Bento cards. Use soft tonal surface contrast (`bgRootGradient` → `surfaceSheet` → `surfaceBentoGroup` → `surfaceCardItem`) and soft tonal destructive fills (`#FEE2E2` with `#DC2626` ink in Light mode).
3. **Zero Emojis — 100% Vector Tech Logos & 1.5dp Stroke Icons**:
   - Never use system Unicode emojis (`🐍`, `🐧`, `🧠`, `💡`, `✨`, `🚀`, `🛠️`, `📁`).
   - Use official vector drawables (`R.drawable.ic_logo_*`) for **Google Gemini**, **Anthropic Claude**, **OpenAI**, **OpenRouter**, **Kotlin**, **Python**, **Node.js**, **Git**, **C++**, and **Alpine Linux**, paired with `1.5.dp` monochromatic Lucide/Material vector icons (only the daily streak badge uses a flame icon).
4. **No Hand-Drawn Mascot SVGs**:
   - Never draw the Klix Bot mascot using custom `Canvas` or `ImageVector` paths. Always render the real PNG / WebP / MP4 assets (`mascot_typing`, `mascot_peeking`, `mascot_running_terminal`, `mascot_streak_fire`, `mascot_waving`, `klix_welcome_celebration_light/dark`).

---

## 2. Color & Background Gradient Tokens (`KlixThemeTokens.kt`)

### Background Gradients (Unified Across All Screens & Onboarding)

| Gradient Token | Light Theme (`Brush.verticalGradient`) | Dark Theme (`Brush.verticalGradient`) | Usage |
| :--- | :--- | :--- | :--- |
| `bgRootGradient` | `#D8EEFE` (0%) → `#E8FBF3` (48%) → `#F6F9FC` (100%) | `#0B0E1A` (0%) → `#141933` (48%) → `#121521` (100%) | Root screen background behind all onboarding pages, drawers, and elevated sheets |
| `ambientOrbAccent` | `#D5FB66` (16% alpha) & `#7DD3FC` (14% alpha) | `#D5FB66` (10% alpha) & `#6366F1` (16% alpha) | 2 slow-drifting blurred radial orbs behind `bgRootGradient` |
| `surfaceMacroTint` | `LinearGradient(#E2F9C8 -> #D4F5E9)` | `LinearGradient(#16302B -> #1E2638)` | `Custom Macros` & `Thinking / Alpine` hero card tint |
| `streakCardGradient` | `LinearGradient(#FFB36B -> #D4F87A -> #7CE8C8)` | `LinearGradient(#9A521E -> #4E6E24 -> #2B5836)` | `Daily Streak` hero card background |

### Surface, Ink & Tonal Button Tokens

| Token Name | Light Theme Value | Dark Theme Value | Usage |
| :--- | :--- | :--- | :--- |
| `surfaceSheet` | `#F6F9FC` | `#121521` | Elevated modal cards & peeking bottom sheets (`28.dp` radius, `0.dp` border) |
| `surfaceBentoGroup` | `#E7EEF5` | `#1B2030` | Grouped Bento containers (`App Theme`, `Active AI Provider`, `Quick Mentions`, file context box) |
| `surfaceCardItem` | `#FFFFFF` | `#23293D` | Unselected tiles, provider pills, workspace rows, prompt pills |
| `accentLime` | `#D5FB66` | `#D5FB66` | Active selection fill (`Google Gemini`, `Light`, `Python 3`), `+ TERM` pill, `v0.4.0` / `ACTIVE` badges, onboarding Page 2–3 CTA |
| `obsidianInk` | `#0C0D14` | `#0C0D14` | Text/icon color on `#D5FB66` surfaces; primary pill CTA & inline headline pill fill in Light Mode |
| `primaryCtaBg` | `#0C0D14` (Editorial CTA) / `#D5FB66` (Sheet CTA) | `#D5FB66` | Full-width `56.dp` pill CTAs (`Get Started »`, `Let's Ship Something »`, `Save & Close`, `Apply & Close`) |
| `primaryCtaFg` | `#FFFFFF` (on `#0C0D14`) / `#0C0D14` (on `#D5FB66`) | `#0C0D14` | Label & icon tint inside primary CTAs |
| `secondaryTonalBg` | `#E7EEF5` | `#1B2030` | Borderless secondary actions (`I'll Code Offline First`, `Try Offline Mode First`) |
| `destructiveTonalBg` | `#FEE2E2` | `#3B181E` | Borderless destructive tonal pill (`Close Without Saving`) |
| `destructiveTonalFg` | `#DC2626` | `#FCA5A5` | Crimson text & `X` icon inside destructive tonal pill |
| `trustVaultBg` | `#E6F7ED` | `#122923` | `100% On-Device Keystore Vault` trust card |
| `trustVaultFg` | `#14532D` | `#86EFAC` | Text & shield icon inside trust card |
| `inkPrimary` | `#0C0D14` | `#F8FAFC` | Display headlines, card titles, primary labels |
| `inkSecondary` | `#475569` | `#94A3B8` | Subtitles, descriptions, inactive tabs, `Skip` / `Keep Editing` |
| `inkMuted` | `#64748B` | `#64748B` | Status bar metadata, line numbers, progress dot inactive state |

---

## 3. Commercial-Free Typography (`KlixTypography.kt`)

Bundle all three **SIL Open Font License 1.1** font families in `app/src/main/res/font/`:
- **Display & Headlines**: **Bricolage Grotesque** (`ExtraBold 800`, `Bold 700`) — expressive, witty editorial curves with tight tracking.
- **UI Body, Cards & Buttons**: **Plus Jakarta Sans** (`Medium 500`, `SemiBold 600`, `Bold 700`) — clean, soothing readability.
- **Code, Filenames & CLI Tags**: **JetBrains Mono** (`Medium 500`, `SemiBold 600`) — `1.54x` line-height ratio.

| Style Token | Font Family | Size | Line Height | Weight | Letter Spacing | Usage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `heroDisplay` | Bricolage Grotesque | `32.sp–34.sp` | `38.sp` | `ExtraBold (800)` | `-0.8.sp` | Onboarding Page 1 & Page 4 headlines (`Still Carrying A Brick?`, `Excuses Deleted.`) |
| `sheetHeadline` | Bricolage Grotesque | `26.sp–28.sp` | `32.sp` | `ExtraBold (800)` | `-0.6.sp` | Onboarding Page 2–3 & modal headlines (`Phones Can't Compile?`, `Unsaved Changes`) |
| `screenTitle` | Bricolage Grotesque | `24.sp` | `30.sp` | `ExtraBold (800)` | `-0.5.sp` | `Settings`, `Klix AI Agent`, `Local Sandbox`, `Linux Terminal`, `Slash Commands` |
| `sheetHeader` | Bricolage Grotesque | `20.sp` | `26.sp` | `Bold (700)` | `-0.3.sp` | `Chat History`, `7 Days Streak`, `Alpine Linux Active`, `Klix AI Sessions` |
| `sectionHeader` | Bricolage Grotesque | `16.5.sp` | `22.sp` | `Bold (700)` | `-0.2.sp` | `App Theme`, `Active AI Provider`, `Recent Workspaces`, `Pre-Configured Toolchains` |
| `cardTitle` | Plus Jakarta Sans | `15.sp` | `20.sp` | `Bold (700)` | `-0.15.sp` | Toolchain tiles, AI provider rows, session items |
| `bodyPrimary` | Plus Jakarta Sans | `14.sp–15.sp` | `20.sp` | `SemiBold (600)` | `0.sp` | Subtitles, pill CTA labels, action rows |
| `bodySecondary` | Plus Jakarta Sans | `12.5.sp` | `17.5.sp` | `Medium (500)` | `0.1.sp` | Helper descriptions, status subtitles |
| `caption` | Plus Jakarta Sans | `11.sp` | `15.sp` | `Medium (500)` | `0.15.sp` | Timestamps, footnotes, muted descriptors |
| `codeEditor` | JetBrains Mono | `13.sp` | `20.sp` | `Medium (500)` | `0.sp` | Sora Code Editor & Alpine Linux terminal output |
| `codePill` | JetBrains Mono | `11.5.sp–12.sp` | `16.sp` | `SemiBold (600)` | `-0.1.sp` | Package commands (`python3 • pip`), `/slash` tags, filenames (`hi.py`) |

---

## 4. Shared Reusable Components (`KlixComponents.kt`)

1. **`KlixEditorialHeadlineWithPill`**:
   - Renders `Bricolage Grotesque ExtraBold` text with an embedded `#0C0D14` pill containing a `#D5FB66` circular icon badge (`</>`, `→`, `✦`) and `#D5FB66` text (used on Onboarding Page 1).
2. **`KlixPeekingMascotScaffold`**:
   - Used on **Settings**, **Klix AI Agent**, **Slash Commands**, **Onboarding Pages 2 & 3**, and **Unsaved Changes** dialogs.
   - Positions `mascot_peeking.png` centered so its paws overlap the top edge of the `28.dp` rounded `surfaceSheet` with a centered `36.dp x 4.dp` drag-handle pill.
3. **`KlixPrimaryPillButton` & `KlixTonalPillButton`**:
   - `54.dp–56.dp` height, `RoundedCornerShape(50)`, `0.dp` border.
   - Supports solid `#0C0D14` (with optional right-aligned `»` arrows), solid `#D5FB66`, neutral `#E7EEF5`, and destructive `#FEE2E2` tonal variants.
4. **`KlixFloatingBottomDock`**:
   - Floating `#121521` pill bar (`height = 64.dp`, `RoundedCornerShape(32.dp)`) with `#D5FB66` active pill highlight across `Home`, `Agent`, `Terminal`, and `Settings`.

---

## 5. Motion Choreography (Subtle & Native Compose)

- **Ambient Background**: 2 soft blurred radial orbs drifting on `rememberInfiniteTransition` (`tween(9000, LinearEasing)`).
- **Page Transitions**: `HorizontalPager` using `Modifier.graphicsLayer` for smooth crossfade (`alpha = 1f - abs(offset) * 0.35f`) and subtle mascot parallax (`translationX = offset * 24.dp.toPx()`).
- **Slide-Up Card Entrance**: Sheets slide upward `28.dp -> 0.dp` (`tween(380, FastOutSlowInEasing)`) with a `55ms` stagger across inner Bento items.
- **Tap Physics**: `animateFloatAsState` press scale (`0.96f -> 1f`, `spring(0.65f, 400f)`) + `animateColorAsState` morph to `#D5FB66`.
- **Progress Dots**: `animateDpAsState` elongating active dot from `8.dp` to `26.dp`.

---

## 6. Onboarding Flow (4 Pages — Storytelling + Real Wiring)

1. **Page 1 — Open Editorial Hook (`mascot_typing.png`)**:
   - Headline: `Still Carrying` `[ </> A Brick? ]` `Just To Code?`
   - Subtitle: `Your laptop is still booting. Meanwhile, your pocket already runs real Linux and AI.`
   - CTA: `Prove Me Wrong »` (`#0C0D14` pill) + top-right `Skip`.
2. **Page 2 — Peeking-Mascot Bento Sheet (`mascot_peeking.png` + Real Alpine Setup)**:
   - Headline: `Phones Can't Compile?`
   - Subtitle: `Cute assumption. Pick your real toolchains—installed locally, no root needed.`
   - 2x2 Borderless Grid: `Web & APIs` (`Python • Node.js`, selected `#D5FB66`), `Android Apps` (`Kotlin • Compose`), `DSA & Practice` (`C++`), `AI Workflows` (`Agents • /slash`).
   - CTA: `Install & Continue →` (`#D5FB66` pill, bound to real `StateFlow<SandboxInstallState>`).
3. **Page 3 — Peeking-Mascot Bento Sheet (`mascot_peeking.png` + Real Keystore Vault)**:
   - Headline: `Paying Rent For Your Own AI?`
   - Subtitle: `Plug in your own key. Encrypted locally on-device—zero proxy, zero middleman tax.`
   - 3 Stacked Borderless Pills: `Google Gemini` (`#D5FB66` + `Selected`), `Anthropic Claude` (`Add Key`), `OpenAI` (`Add Key`) + expandable inline API key input.
   - Trust Card (`#E6F7ED`): `100% On-Device Keystore Vault` / `AES-256 encrypted locally. Zero server proxy or telemetry.`
   - CTAs: `Connect My Key →` (`#D5FB66`) + `I'll Code Offline First` (`#E7EEF5`).
4. **Page 4 — Open Editorial Welcome (`klix_welcome_celebration_light/dark.webp` or `.mp4`)**:
   - Looping mascot celebration video/WebP with minimal sky-blue & lime/emerald corner bursts.
   - Headline: `Excuses Deleted.`
   - Subtitle: `No desk? No laptop? No problem. Small steps build big things—your pocket studio is live.`
   - CTA: `Let's Ship Something »` (`#0C0D14` pill).

---

## 7. Core App Screens & Dialogs (9 Screens + Modals)

- **Screen 1 — Sandbox Workspace Home**: Greeting + `🔥 7` streak pill, Mascot Hero Bento Card (`mascot_coding_laptop`, `Thinking: 4K`, `Alpine: Ready`), Recent Workspaces (`orbit-ui`, `palindrome.py`, `study-buddy`), and `KlixFloatingBottomDock`.
- **Screen 2 — Settings Modal**: `KlixPeekingMascotScaffold(title = "Settings")`, `App Theme` (`Light`, `Dark`, `System`), `Active AI Provider` (`Google Gemini`, `Anthropic Claude`, `OpenAI`, `OpenRouter / Groq`), `Apply & Close`.
- **Screen 3 — Klix AI Agent**: `KlixPeekingMascotScaffold`, `Chat 1 ▾`, `Quick Mentions & Model`, `What shall we do?` prompt pills, floating composer with `/` and `@`.
- **Screen 4 — Local Sandbox & Toolchains**: `Alpine Linux Active` hero card (`mascot_running_terminal`), 2x2 toolchain grid (`Node.js`, `Python 3`, `Git`, `C/C++`), `Open Linux Terminal` CTA.
- **Screen 5 — Linux Terminal**: Pill tab strip (`Alpine v1`, `+ Terminal`), `JetBrains Mono` terminal viewport, `+ TERM` / `ESC` / `TAB` / `CTRL-C` shortcuts, command input bar.
- **Screen 6 — Slash Commands Studio**: `KlixPeekingMascotScaffold`, `Built-in Quick Commands` (`/refactor`, `/fix`, `/test`, `/explain`), `Custom Macros (2)` (`surfaceMacroTint`), `+ Create Custom Command`.
- **Screen 7 — Code Editor**: File tabs (`palindrome.py`, `MainActivity.kt`), `🔥 7`, `▶ Run`, `JetBrains Mono` editor, `Klix Buddy says:` inline card (`/test`, `/optimize`), unified bottom symbol + status bar.
- **Screen 8 — Workspace Drawer & Daily Streak**: `KLIX CODE` + `v0.4.0` pill, `streakCardGradient` card (`mascot_streak_fire`, `7 Days Streak`, `M T W T F S S` checks), `Workspace Files`, floating theme & settings pill bar.
- **Screen 9 — Chat History Sheet**: `Klix AI Sessions` (`mascot_waving`, `3 Chats`, `50K Auto-Compacted`), elevated `Chat History` sheet with `ACTIVE` badge, 3 session cards, `+ Start New Chat`.
- **Modal — Unsaved Changes Dialog**: `KlixPeekingMascotScaffold` (`28.dp` `#F6F9FC` card, `mascot_peeking.png`), `Unsaved Changes`, borderless `#E7EEF5` file context box (`[Python] hi.py | • Modified | Ln 2, Col 5`), borderless `#D5FB66` `Save & Close`, borderless `#FEE2E2` `Close Without Saving`, and `Keep Editing`.

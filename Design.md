# Role & Objective
You are a Senior Android UI/UX Engineer specializing in **Kotlin** and **Jetpack Compose (Material 3)**.
Your task is to refactor and implement the complete 9-screen UI redesign for **Klix Code (v0.4.0)** — an autonomous mobile AI IDE and unrooted Alpine Linux sandbox on Android — matching the attached Light Theme and Dark Theme reference designs with pixel-level accuracy.

## Strict Engineering & Visual Rules
1. **Preserve Existing Business Logic & ViewModels**: Keep all existing ViewModel `StateFlow`s, coroutine scopes, `libproot.so` terminal bridges, Sora Editor bindings, and `agent_sessions.json` persistence intact. Only refactor the Jetpack Compose UI layer, theme tokens, and navigation chrome.
2. **Zero Emojis — 100% Vector Logos & 1.5dp Stroke Icons**: NEVER use system Unicode color emojis (`🐍`, `🐧`, `🧠`, `💡`, `✨`, `🚀`, `🛠️`, `📁`) anywhere in the UI, except for the vector flame icon on the streak pill/card. Use official vector drawables (`R.drawable.ic_logo_*`) for **Google Gemini**, **Anthropic Claude**, **OpenAI**, **OpenRouter**, **Kotlin**, **Python**, **Node.js**, **Git**, **C++**, and **Alpine Linux**, paired with 1.5dp monochromatic Lucide/Material vector icons.
3. **2D Klix Bot Mascot Integration**: Use the official 2D white-and-neon-lime Klix Bot mascot sprites (`R.drawable.mascot_peeking`, `R.drawable.mascot_coding_laptop`, `R.drawable.mascot_running_terminal`, `R.drawable.mascot_streak_fire`, `R.drawable.mascot_waving`) in the exact hero and peeking-sheet positions specified below.
4. **Adaptive Dual-Theme Primary CTA Inversion**:
   - **Light Theme Primary Pill CTA**: Deep Obsidian background (`#0C0D14`) with Pure White text (`#FFFFFF`) and Electric Lime (`#D5FB66`) leading icon/accent.
   - **Dark Theme Primary Pill CTA**: Electric Chartreuse-Lime background (`#D5FB66`) with Deep Obsidian text (`#0C0D14`) and bold weight.

## 1. Color & Gradient Design Tokens (`KlixThemeTokens.kt`)

Implement a custom `KlixPalette` `CompositionLocal` supporting both **Light Theme (Frosted Sky-to-Mint Aurora)** and **Dark Theme (Cosmic Sapphire-to-Violet Obsidian)**:

| Token Name | Light Theme Value | Dark Theme Value | Usage |
| :--- | :--- | :--- | :--- |
| `bgRootGradient` | `LinearGradient(#D8EEFE -> #E8FBF3 -> #F8FAFC)` | `LinearGradient(#0B0E1A -> #151932 -> #2A164D)` | Root screen background & top header backdrop |
| `surfaceSheet` | `#F4F8FB` (96% alpha frosted white) | `#121521` (96% alpha deep obsidian) | Main elevated bottom sheets & screen containers (`28.dp` top radius) |
| `surfaceBentoGroup` | `#E6EEF6` (soft slate-ice tint) | `#1B2030` (elevated slate-navy) | Grouped section containers (`Quick Mentions`, `What shall we do?`, `Active AI Provider`) |
| `surfaceCardItem` | `#FFFFFF` (Pure White) | `#23293D` (Charcoal-Slate card) | Individual list rows, toolchain tiles, and prompt pills |
| `surfaceMacroTint` | `LinearGradient(#E2F9C8 -> #D4F5E9)` | `LinearGradient(#16302B -> #1E2638)` | `Custom Macros (2)` & `Thinking/Alpine` hero card tint |
| `streakCardGradient` | `LinearGradient(#FFB36B -> #D4F87A -> #7CE8C8)` | `LinearGradient(#9A521E -> #4E6E24 -> #2B5836)` | `7 Days Streak` hero card background |
| `primaryCtaBg` | `#0C0D14` (Deep Obsidian) | `#D5FB66` (Electric Chartreuse-Lime) | Full-width bottom pill CTA buttons & active floating dock pill |
| `primaryCtaText` | `#FFFFFF` (Pure White) | `#0C0D14` (Deep Obsidian) | Text & icon tint inside primary CTA buttons |
| `accentLime` | `#D5FB66` (fill) / `#4D7C0F` (text) | `#D5FB66` (Electric Chartreuse-Lime) | Active provider pill (`Google Gemini`), `+ TERM` pill, `v0.4.0` & `ACTIVE` badges |
| `inkPrimary` | `#0F172A` (Deep Slate-Black) | `#F8FAFC` (Pure Alabaster-White) | Headlines, card titles, primary labels |
| `inkSecondary` | `#475569` (Muted Slate) | `#94A3B8` (Cool Silver-Slate) | Subtitles, descriptions, inactive tabs |
| `borderHairline` | `#CBD5E1` (60% alpha, `1.dp`) | `#2E364F` (80% alpha, `1.dp`) | Crisp `1.dp` card & pill borders |
| `borderActiveLime` | `#84CC16` (`1.5.dp`) | `#D5FB66` (`1.5.dp`) | Active card/pill stroke (`Klix Buddy says` dark card, active provider) |

## 2. Commercial-Free 1.25x Modular Typography (`KlixTypography.kt`)

Bundle **Plus Jakarta Sans** (`res/font/plus_jakarta_sans_*.ttf`) for UI/Display and **JetBrains Mono** (`res/font/jetbrains_mono_*.ttf`) for code, filenames, and slash commands:

| Style Token | Font Family | Size | Line Height | Weight | Letter Spacing | Target Elements |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `screenTitle` | Plus Jakarta Sans | `24.sp` | `30.sp` | `ExtraBold (800)` | `-0.5.sp` | `Hey, Prasanjit`, `Settings`, `Klix AI Agent`, `Local Sandbox`, `Linux Terminal`, `Slash Commands`, `Klix Code` |
| `sheetHeader` | Plus Jakarta Sans | `20.sp` | `26.sp` | `Bold (700)` | `-0.3.sp` | `Chat History`, `7 Days Streak`, `Alpine Linux Active`, `Klix AI Sessions` |
| `sectionHeader` | Plus Jakarta Sans | `16.sp` | `22.sp` | `Bold (700)` | `-0.15.sp` | `App Theme`, `Active AI Provider`, `Quick Mentions & Model`, `What shall we do?`, `Pre-Configured Toolchains`, `Recent Workspaces`, `Custom Macros (2)` |
| `cardTitle` | Plus Jakarta Sans | `15.sp` | `20.sp` | `Bold (700)` | `-0.1.sp` | `Node.js`, `Python 3`, `Git`, `C/C++`, `Chat 1 • palindrome.py`, `Google Gemini` |
| `bodyPrimary` | Plus Jakarta Sans | `14.sp` | `20.sp` | `SemiBold (600)` | `0.sp` | Action row labels, button text (`+ Open Sandbox Workspace`, `Apply & Close`) |
| `bodySecondary` | Plus Jakarta Sans | `12.5.sp` | `17.sp` | `Medium (500)` | `0.1.sp` | `Sandbox Workspace`, `Your local on-device environment`, `Installed ✓`, macro descriptions |
| `codeEditor` | JetBrains Mono | `13.sp` | `20.sp` (`1.54x`) | `Medium (500)` | `0.sp` | Sora Code Editor lines (`palindrome.py`), Alpine Linux terminal stdout |
| `codePill` | JetBrains Mono | `12.sp` | `16.sp` | `SemiBold (600)` | `-0.1.sp` | `/refactor`, `/fix`, `/test`, `/explain`, `/summarize`, `/optimize`, `+ TERM`, `ESC`, `TAB`, `CTRL-C` |

## 3. Shared Reusable Layout Components (`KlixComponents.kt`)

1. **`KlixPeekingMascotScaffold`**:
   - Used on **Settings**, **Klix AI Agent**, and **Slash Commands**.
   - Renders the top title & `bgRootGradient` header (`height = 170.dp`), positions `Image(painterResource(R.drawable.mascot_peeking))` centered horizontally so the mascot's hands overlap the top edge of the `Surface(shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))` sheet below it, and draws a centered `36.dp x 4.dp` drag-handle pill at the top of the sheet.
2. **`KlixPrimaryPillButton`**:
   - Full-width `54.dp` height `RoundedCornerShape(50)` button.
   - Uses `primaryCtaBg` (`#0C0D14` in Light, `#D5FB66` in Dark) and `primaryCtaText` (`#FFFFFF` in Light, `#0C0D14` in Dark).
3. **`KlixFloatingBottomDock`**:
   - Floating pill container (`height = 64.dp`, `RoundedCornerShape(32.dp)`, `#121521` dark surface with `1.dp` border) containing 4 destinations:
     - `Home` (Active state rendered as an inner `#D5FB66` pill with `#0C0D14` house icon + bold `"Home"` label)
     - `Agent` (monochrome icon + `"Agent"` small label)
     - `Terminal` (monochrome `>_` icon + `"Terminal"` small label)
     - `Settings` (monochrome gear icon + `"Settings"` small label)

## 4. Screen-by-Screen Implementation Specifications

### Screen 1: Sandbox Workspace Home
- **Top Header**: Left column with `"Hey, Prasanjit"` (`screenTitle`) and `"Sandbox Workspace"` (`bodySecondary`). Right pill badge with flame icon + `"7"` (`RoundedCornerShape(20.dp)`).
- **Mascot Hero Bento Card (`RoundedCornerShape(24.dp)`)**:
  - Soft sky-to-mint gradient fill (Light) / dark indigo-to-olive glow fill with `1.dp` neon-lime border (Dark).
  - Left side: `R.drawable.mascot_coding_laptop` (2D Klix Bot typing on `<|>` laptop).
  - Top-right speech bubble: `"What shall we build today?"`.
  - Bottom row inside card: Two side-by-side pill chips:
    1. `[Vector CPU Icon] Thinking: 4K`
    2. `[Official Alpine Linux Hexagon Logo] Alpine: Ready`
- **Primary CTA**: `KlixPrimaryPillButton(text = "+ Open Sandbox Workspace")`.
- **Recent Workspaces Section**:
  - Section title `"Recent Workspaces"`.
  - 3 stacked `56.dp` height rounded cards (`RoundedCornerShape(16.dp)`):
    1. `[Official Kotlin Logo] orbit-ui`
    2. `[Official Python Logo] palindrome.py`
    3. `[Official Node.js Hexagon Logo] study-buddy`
- **Bottom Bar**: `KlixFloatingBottomDock(selected = Home)`.

### Screen 2: Settings Modal / Screen
- **Layout**: `KlixPeekingMascotScaffold(title = "Settings")`.
- **Card 1 — `App Theme` (`surfaceBentoGroup`, `RoundedCornerShape(22.dp)`)**:
  - Header `"App Theme"`.
  - Row of 3 pill chips (`RoundedCornerShape(50)`):
    - `[Sun Vector Icon] Light` (highlighted in `#D5FB66` when Light is active)
    - `[Moon Vector Icon] Dark` (highlighted in `#D5FB66` when Dark is active)
    - `[Monitor Vector Icon] System`
- **Card 2 — `Active AI Provider` (`surfaceBentoGroup`, `RoundedCornerShape(22.dp)`)**:
  - Header `"Active AI Provider"`.
  - 4 stacked provider pill rows (`52.dp` height, `RoundedCornerShape(26.dp)`):
    1. `[Official Google Gemini Star Logo] Google Gemini` — full `#D5FB66` lime highlight fill with right pill badge `"Ready"`.
    2. `[Official Anthropic Claude Starburst Logo] Anthropic Claude` — right pill button `"Add Key"`.
    3. `[Official OpenAI Blossom Logo] OpenAI` — right pill button `"Add Key"`.
    4. `[Vector Node Graph Icon] OpenRouter / Groq` — right pill button `"Add Key"`.
- **Bottom CTA**: `KlixPrimaryPillButton(text = "Apply & Close")`.

### Screen 3: Klix AI Agent
- **Layout**: `KlixPeekingMascotScaffold` with top row `"Klix AI Agent"` on left and dropdown pill `"Chat 1 ▾"` on right.
- **Card 1 — `Quick Mentions & Model` (`surfaceBentoGroup`, `RoundedCornerShape(22.dp)`)**:
  - Row 1: Pill chip `[Official Google Gemini Logo] gemini-3.1-flash` (soft indigo-lavender tint).
  - Row 2: Two side-by-side file mention pills: `[Official Python Logo] palindrome.py` and `[Official Kotlin Logo] MainActivity.kt`.
- **Card 2 — `What shall we do?` (`surfaceBentoGroup`, `RoundedCornerShape(22.dp)`)**:
  - 4 stacked action pills (`46.dp` height, `RoundedCornerShape(23.dp)`, `surfaceCardItem` fill) with 1.5dp stroke vector icons:
    1. `[Layers Vector Icon] Summarize workspace architecture`
    2. `[Search-Code Vector Icon] Find all TODOs & unfinished items`
    3. `[Test-Tube Vector Icon] Write unit tests for palindrome.py`
    4. `[Shield-Check Vector Icon] Check for crashes or edge cases`
- **Bottom Composer Bar**:
  - Floating pill text field (`height = 52.dp`, `RoundedCornerShape(26.dp)`) with placeholder `"Ask Klix or type / ..."` and a circular send button (`38.dp`, `#0C0D14` in Light / `#D5FB66` in Dark).

### Screen 4: Local Sandbox & Toolchains
- **Top Header**: Left column with `"Local Sandbox"` (`screenTitle`) and `"Your local on-device environment"` (`bodySecondary`). Right pill badge `[Official Alpine Linux Logo] Ready`.
- **Hero Status Card (`RoundedCornerShape(24.dp)`)**:
  - Sky-to-mint gradient (Light) / Deep navy-to-purple gradient with `1.dp` border (Dark).
  - Left: `R.drawable.mascot_running_terminal` (2D Klix Bot running with `>_` prompt).
  - Right column: `"Alpine Linux Active"` (`sheetHeader`), `"On-device unrooted runtime"` (`bodySecondary`), and pill button `"⤓ Install RootFS"` / `"↻ Reinstall RootFS"`.
- **Pre-Configured Toolchains (2x2 Bento Grid)**:
  - Section title `"Pre-Configured Toolchains"`.
  - 2x2 grid (`Arrangement.spacedBy(12.dp)`) of `RoundedCornerShape(18.dp)` cards:
    1. `[Official Node.js Green Hexagon Logo]` **Node.js** / `"Installed ✓"`
    2. `[Official Python Blue-Yellow Logo]` **Python 3** / `"Installed ✓"`
    3. `[Official Git Orange Diamond Logo]` **Git** / `"Installed ✓"`
    4. `[Official C++ Blue Hexagon Logo]` **C/C++** / `"Tap to Install"`
- **Bottom CTA**: `KlixPrimaryPillButton` with leading `[>_]` lime badge + `"Open Linux Terminal"`.

### Screen 5: Linux Terminal
- **Top Header**: `"Linux Terminal"` (`screenTitle`).
- **Tab Strip**: Horizontal scrollable row of pill tabs (`height = 38.dp`, `RoundedCornerShape(19.dp)`):
  - `[Official Alpine Linux Logo] Alpine v1` (active)
  - `+ Terminal` / `[Official Python Logo] palindrome.py`
  - `[Official Node.js Logo]`
- **Terminal Viewport Card (`RoundedCornerShape(24.dp)`, `height = 290.dp`)**:
  - Monospace output (`JetBrains Mono 13.sp`):
    `klix@mobile:/workspace$`
    `python3 palindrome.py`
    `✓ Server & checks passed in 0.4s`
  - Bottom-right overlay inside card: `R.drawable.mascot_waving` with speech bubble `"All green!"`.
- **Shortcuts Section**:
  - Label `"Shortcuts"`.
  - Row of pill keycaps: `+ TERM` (filled with `#D5FB66` lime), `ESC`, `TAB`, `CTRL-C`, `↑`, `↓`.
- **Bottom Command Input Bar**:
  - Pill input field `"$ Run a command..."` with circular send button.

### Screen 6: Slash Commands Studio
- **Layout**: `KlixPeekingMascotScaffold(title = "Slash Commands")`.
- **Card 1 — `Built-in Quick Commands` (`surfaceBentoGroup`, `RoundedCornerShape(22.dp)`)**:
  - `FlowRow` of 4 pill chips (`JetBrains Mono 12.sp`) with 1.5dp vector icons:
    - `[Git-Branch Icon] /refactor`
    - `[Wrench Icon] /fix`
    - `[Shield-Check Icon] /test`
    - `[Book-Open Icon] /explain`
- **Card 2 — `Custom Macros (2)` (`surfaceMacroTint` gradient card, `RoundedCornerShape(22.dp)`)**:
  - 2 stacked inner cards (`RoundedCornerShape(16.dp)`):
    1. `[Official Google Gemini Star Logo] /summarize` — `"Summarize file in 3 key bullet points"`
    2. `[Official Anthropic Claude Starburst Logo] /optimize` — `"Analyze Big-O and optimize speed"`
- **Bottom CTA**: `KlixPrimaryPillButton(text = "+ Create Custom Command")`.

### Screen 7: Code Editor (`palindrome.py`)
- **Top Tab & Action Bar**:
  - Active file pill `[Official Python Logo] palindrome.py`, inactive file pill `[Official Kotlin Logo]`, streak pill `🔥 7`, and `"▶ Run"` pill button (`#D5FB66` in Dark, soft slate pill in Light).
- **Code Editor Card (`RoundedCornerShape(20.dp)`)**:
  - Crisp `1.dp` border (`#CBD5E1` in Light, `#2E364F` in Dark), line-number gutter (`1..10`), and syntax-highlighted `JetBrains Mono 13.sp` code (`1.54x` line height).
- **Klix Buddy Inline Assistant Card (`RoundedCornerShape(20.dp)`)**:
  - Left: 2D Klix Bot avatar (`R.drawable.mascot_peeking`).
  - Right column: `"Klix Buddy says:"` (bold), `"Ready to test palindrome.py?"`, and two pill buttons:
    - `[>_ Icon] /test`
    - `[Official Gemini Star Icon] /optimize`
- **Bottom Symbol & Status Bar**:
  - Single unified pill bar containing quick keys (`Tab`, `↶`, `↷`, `(`, `)`, `:`, `"`) on the left and `"Ln 1, Col 1 • 14pt"` on the right.

### Screen 8: Workspace Drawer & Daily Streak
- **Top Header**: `"Klix Code"` (`screenTitle`) on left, neon-lime pill `"v0.4.0"` on right.
- **Daily Streak Hero Card (`streakCardGradient`, `RoundedCornerShape(24.dp)`)**:
  - Top center: `R.drawable.mascot_streak_fire` (cheering 2D Klix Bot inside a clean vector flame aura) overlapping the top edge of the card.
  - Center title: `"7 Days Streak"` (`20.sp ExtraBold`).
  - 7-column weekly strip (`M T W T F S S`) with `#D5FB66` circular checkmark badges below each day letter.
- **Workspace Files Section**:
  - Section title `"Workspace Files"`.
  - 3 stacked file cards (`54.dp` height, `RoundedCornerShape(16.dp)`):
    1. `[Official Node.js Hexagon Logo] server.js` (with right `JS` outline badge)
    2. `[Official Python Logo] palindrome.py`
    3. `[Official Kotlin Logo] orbit-ui` (with right orange pill badge `[Git Branch Icon] main`)
- **Bottom Theme & Settings Pill Bar**:
  - Floating dark pill bar containing a segmented `"☀️ Light / 🌙 Dark"` pill toggle (highlighted in `#D5FB66`) on the left and `"⚙️ Settings"` on the right.

### Screen 9: Chat History & Klix AI Sessions
- **Top Header Banner**:
  - Left: `R.drawable.mascot_waving` (2D Klix Bot waving).
  - Right column: `"Klix AI Sessions"` (`20.sp ExtraBold`), pill chip `[Chat Bubble Vector Icon] 3 Chats`, and pill chip `[Bolt Vector Icon] 50K Auto-Compacted`.
- **Elevated Sheet (`RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)`)**:
  - Top row: `"Chat History"` (`sheetHeader`) on left, neon-lime pill badge `"ACTIVE"` on right.
  - 3 stacked session cards (`RoundedCornerShape(20.dp)`):
    1. **Card 1**: Title `"Chat 1 • palindrome.py"`, subtitle `[Official Python Logo] Python`, right icon `[Official Google Gemini Star Logo]`.
    2. **Card 2 (Soft Lavender-Indigo Tint)**: Title `"Chat 2 • orbit-ui"`, subtitle `[Official Kotlin Logo] Anthropic and Kotlin`, right icon `[Official Anthropic Claude Starburst Logo]`.
    3. **Card 3**: Title `"Chat 3 • study-buddy"`, subtitle `[Official Node.js Logo] Node.js`, right icon `[Official OpenAI Blossom Logo]`.
  - **Bottom CTA**: `KlixPrimaryPillButton(text = "+ Start New Chat")`.
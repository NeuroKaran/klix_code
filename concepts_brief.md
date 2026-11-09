# UI/UX Refactor: Dashboard, Navigation Dock, and Editor Controls

* **Author:** NeuroKaran
* **Date:** 2026-11-09
* **Status:** Review Approved
* **Target Module:** Android Jetpack Compose (`app/`)

## Overview

Klix Code requires a UI/UX refactoring of its Android Jetpack Compose application (`app/`) to transition from prototype scaffolding to a finished, interactive mobile IDE interface. The refactor consolidates developer touch targets to adhere to minimum 48dp hitboxes, removes non-functional placeholder elements, replaces static hero content with actionable workspace readiness indicators, and stabilizes navigation boundaries between the home dashboard and active editor viewports.

## Analysis

### 1. Viewport Real Estate and Virtual Keyboard (IME) Constraints
Card-based lifestyle application mockups utilize spacious padding and large container corner radii (16dp–24dp). When applied to a mobile code editor, vertical padding and persistent bars reduce visible buffer height. On screens under 380dp width with an active soft keyboard (`adjustResize`), vertical space is constrained to 250dp–320dp. The editor viewport must prioritize code line visibility and accessory bar clearance. Consequently, persistent floating navigation bars must be restricted to non-editor surfaces.

### 2. Eliminating Placeholder and Unused Controls
* **Empty State vs. Recent Workspaces**: In `HomeDashboard.kt`, hardcoded dummy items (`orbit-ui`, `palindrome.py`) are removed. When `recentProjects` is empty, an `EmptyWorkspacesCard` displays actionable entry points: "Open Folder" (`onOpenFolder`) and "Launch Sandbox Workspace" (`onOpenSandboxWorkspace`). When projects exist, `RecentWorkspacesList` renders active items.
* **Sandbox Readiness in Hero Card**: The static mascot speech bubble is replaced with a functional Sandbox Readiness Card displaying factual state:
  1. Sandbox installation status (`READY` with `LimeBrandColor` vs. `SETUP REQUIRED` with `AccentWarning`).
  2. Active AI model/provider indicator (read from `activeModelName`).
  3. Single-tap primary CTA: "Launch Sandbox Workspace" (or "Configure Sandbox" via `onConfigureSandbox` if setup is required).
  Speculative JVM garbage collector metrics and pseudo-daemon process states are omitted.
* **Consolidated Editor Top Bar**: In `MainActivity.kt`, the editor `TopAppBar` reduces horizontal clutter by displaying primary actions directly (Run button, Save icon with dirty state indicator, and an Overflow icon) while routing secondary tools (Agent, Terminal, Find & Replace, Word Wrap, Settings) through a state-aware Material 3 `DropdownMenu`.
* **Dock Navigation Boundary**: `FloatingNavDock` is strictly scoped to `HomeDashboard.kt` to prevent collision with the soft keyboard, cursor handles, and `EditorAccessoryBar`.

## Goals vs. Non-Goals

### In-Scope Goals
* Refactor `HomeDashboard.kt` with factual sandbox readiness indicators and actionable quick-start project entry points.
* Eliminate all non-functional dummy workspace items and placeholder buttons.
* Consolidate editor `TopAppBar` actions into primary controls and a `DropdownMenu` overflow menu to prevent touch target collisions on viewports under 380dp.
* Ensure all interactive touch targets meet or exceed 48dp hitboxes.
* Bind every interactive button and card click to a working Compose event handler.

### Out-of-Scope Non-Goals
* Modifying the web landing page (`landing_page/`).
* Introducing speculative Git clone functionality or buttons lacking backend implementation.
* Modifying the core Sora editor text rendering or TextMate grammar engines.
* Introducing JVM heap telemetry polling or background process monitoring daemons.

### Target User Scenarios
1. **Quick Launch & File Navigation**: User opens Klix Code, immediately sees sandbox readiness, and taps a recent project or "Open Folder" without encountering dead placeholders.
2. **Distraction-Free Mobile Editing**: User edits code with maximum vertical buffer space, accesses symbol helpers via `EditorAccessoryBar`, and executes code via a single prominent "Run" action.
3. **Tool Access via Overflow**: Secondary tools (Terminal, AI Agent, Settings) are accessible within two taps via the top bar overflow menu without cluttering the editing header.

### Acceptance Criteria & Success Metrics
* 0 non-functional buttons or dummy project entries across all touched screens.
* Minimum 48dp touch target hitbox compliance across all interactive icons.
* Zero layout overlap between navigation elements and the Android soft keyboard.
* Maintained 60fps frame rate during scroll and drawer transitions.

## Alternatives Weighed

* **Retaining 7-button horizontal top row**: Rejected due to touch target overlap and truncation on screen widths under 380dp.
* **Maintaining persistent floating dock in editor view**: Rejected due to vertical viewport collision with the soft keyboard (`WindowInsets.ime`) and `EditorAccessoryBar`.
* **Displaying Android JVM heap telemetry**: Rejected as technically misleading for PRoot Linux subprocesses and an unnecessary battery drain.

## Detailed Implementation Plan

### Stage 1: Home Dashboard & Hero Sandbox Card
1. In `HomeDashboard.kt`, update function signature to include `onConfigureSandbox: () -> Unit = {}`.
2. In `HomeDashboard.kt`, refactor the hero section:
   - Retain `Image(painterResource(R.drawable.klix_mascot_laptop))` on the left.
   - Replace the static speech bubble on the right with a status column:
     - Sandbox Status Badge: `READY` (using `LimeBrandColor`) when `isSandboxInstalled == true`, or `SETUP REQUIRED` (using `AccentWarning`) when `isSandboxInstalled == false`.
     - Active Model Pill: displays `activeModelName` with subtle surface container.
   - Primary Action Button below: renders "Launch Sandbox Workspace" (calls `onOpenSandboxWorkspace`) when `isSandboxInstalled == true`, or "Configure Sandbox" (calls `onConfigureSandbox`) when `isSandboxInstalled == false`.
3. In `HomeDashboard.kt`, replace hardcoded dummy fallback cards with `EmptyWorkspacesCard`:
   - "Open Folder" card/button bound to `onOpenFolder`.
   - "Launch Sandbox" card/button bound to `onOpenSandboxWorkspace`.
4. In `MainActivity.kt` (line 794), pass `onConfigureSandbox = { terminalViewModel.openSetupDialog() }` to `HomeDashboard`.

### Stage 2: Editor Top Bar & Action Consolidation
1. In `MainActivity.kt`, declare local menu state:
   ```kotlin
   var isOverflowMenuExpanded by rememberSaveable { mutableStateOf(false) }
   ```
2. Streamline `TopAppBar` when tabs are open:
   - Primary: Run button (`Icons.Default.PlayArrow`, lime container, executes active file in terminal).
   - Secondary: Save icon button (`Icons.Default.Save`, active lime tint when `activeTab.isModified == true`, calls `viewModel.saveActiveTab()`).
   - Overflow Menu: Single `IconButton` (`Icons.Default.MoreVert`, calls `isOverflowMenuExpanded = true`) hosting a `DropdownMenu`:
     - "AI Assistant" (`Icons.Default.SmartToy`, tinted `LimeBrandColor` when `isAgentPanelOpen` is true; clicks toggle `isAgentPanelOpen = !isAgentPanelOpen` and close menu).
     - "Terminal" (`Icons.Default.Terminal`, tinted `CyanBrandColor` when `isTerminalOpen` is true; clicks toggle `isTerminalOpen = !isTerminalOpen` and close menu).
     - "Find & Replace" (`Icons.Default.Search`, tinted `CyanBrandColor` when `isSearchOpen` is true; clicks invoke `viewModel.toggleSearch()` and close menu).
     - "Word Wrap" (`Icons.AutoMirrored.Filled.WrapText`, tinted `CyanBrandColor` when `isWordWrap` is true; clicks invoke `viewModel.toggleWordWrap()` and close menu).
     - "Settings" (`Icons.Default.Settings`, clicks invoke `agentViewModel.toggleSettings()` and close menu).
3. Touch targets: Remove hardcoded outer `.size(34.dp)` on `IconButton` and apply `.size(20.dp)` to inner icons with M3 `minimumInteractiveComponentSize()` to satisfy 48dp minimum hitboxes.

### Stage 3: Visual Polish & Tactile Feedback
1. Reuse existing `Modifier.pressEffect(0.96f)` from `GlassModifiers.kt` across cards and buttons.
2. Standardize container corner radii (16dp for list cards, 24dp for hero container, CircleShape for dock and action pills).
3. Apply existing theme color tokens from `Color.kt` and `Theme.kt` (`AccentWarning`, `LimeBrandColor`, `CyanBrandColor`).

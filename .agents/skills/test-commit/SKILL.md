---
name: test-commit
description: Thoroughly test newly implemented logic or code changes and commit them with a well-defined, descriptive, and comprehensive conventional commit message.
---

# Test and Verified Commit Workflow (`/test-commit`)

Use this workflow whenever the user invokes `/test-commit` or asks to test and commit recent changes.

## Workflow Objectives
1. Inspect all changes made to the codebase.
2. Run relevant tests, linters, and verification checks.
3. Only proceed if all tests pass.
4. Construct a detailed, comprehensive, and well-explained commit message (avoiding short or trivial messages).
5. Stage the appropriate files and create the commit.

---

## Step 1: Inspect Changes

1. Run `git status -s` and `git diff` to review all modified, staged, and untracked files.
2. Verify that no secrets, local configuration files, or build artifacts are caught in the diff:
   - Check against `.gitignore`.
   - Never commit keystores (e.g. `*.jks`, `*.keystore`), sensitive tokens, or local environment files (`local.properties`, `.env`).
3. Identify which subprojects or components were affected:
   - **Android / Kotlin app**: `app/`, `build.gradle.kts`, `gradle/`
   - **Web / Landing page**: `landing_page/`
   - **Documentation / Configs**: `docs/`, `README.md`, etc.

---

## Step 2: Thorough Testing and Verification

Run the appropriate automated verification command based on the files touched:

### For Android Changes (`app/`):
- Run unit tests:
  ```powershell
  .\gradlew.bat testDebugUnitTest
  ```
- If the change affects UI, layouts, or manifest:
  ```powershell
  .\gradlew.bat lintDebug -x test
  ```

### For Web / Frontend Changes (`landing_page/`):
- Run tests (if configured) or verify build integrity:
  ```powershell
  npm --prefix landing_page test
  # or build verification:
  npm --prefix landing_page run build
  ```

### Manual Logic Review:
- In addition to automated tests, perform a semantic code review of the diff:
  - Check null-safety, error handling, and potential edge cases.
  - Check for unintended side effects or regressions in related modules.
  - Ensure code style and conventions are maintained.

> [!IMPORTANT]
> **Zero Failure Tolerance**: If any test, build, or lint check fails, **DO NOT COMMIT**. Stop immediately, identify the root cause, inform the user with error details, or resolve the issue before proceeding.

---

## Step 3: Craft a Well-Defined & Comprehensive Commit Message

The commit message must be rich, structured, and informative. **Never use single-line or vague messages** (e.g. avoid `feat: update code` or `fix: bugs`).

### Required Structure

```text
<type>(<scope>): <imperative summary of the primary change>

[Context & Motivation]
A comprehensive paragraph explaining why this change is necessary, what problem it solves, or what capability it introduces to the system.

[Detailed Changes]
- <component/module>: Detailed explanation of the logic added or modified.
- <edge case / behavior>: Explanation of how boundary conditions, errors, or state changes are handled.
- <architecture/data flow>: Any updates to data models, interfaces, or inter-component communication.

[Verification & Testing]
- Executed: List of automated tests run (e.g., `./gradlew testDebugUnitTest`) with 100% pass status.
- Manual verification: Specific behaviors, edge cases, and states manually validated.
```

### Commit Types:
- `feat`: A new feature or capability for the user.
- `fix`: A bug fix.
- `refactor`: Code refactoring without behavioral changes.
- `perf`: Performance improvements.
- `test`: Adding or updating test cases.
- `docs`: Documentation updates.
- `chore`: Build scripts, dependencies, or tool configurations.

---

## Step 4: Stage and Commit

1. Stage only the relevant modified files (avoid `git add .` if unintended files exist):
   ```powershell
   git add <file1> <file2>
   ```
2. Execute the commit with the multi-line message:
   ```powershell
   git commit -m "<type>(<scope>): <summary>" -m "<detailed body and bullet points>"
   ```
3. Run `git log -1 --stat` and `git status` to verify the commit was successfully recorded and the working directory is in a clean state.
4. Report a concise confirmation to the user showing the commit hash and a brief overview of the tests passed.

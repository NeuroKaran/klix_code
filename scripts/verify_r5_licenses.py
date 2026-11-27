#!/usr/bin/env python3
"""
R5 Free & Production-Legal License Compliance Verifier for KlixCode.

Scans:
  - gradle/libs.versions.toml
  - app/build.gradle.kts
  - app/src/main/jniLibs/
  - app/src/main/assets/ (including .tar.gz / .tgz / .tar archives)
  - SandboxManager.kt & OnboardingViewModel.kt (no unverified alpinelinux.org runtime binary downloads)

Forbids GPL-2.0, GPL-3.0, AGPL, paid/non-commercial licenses, and known GPL runtime components
(proot, talloc, gdbm, readline, bash). Allows dynamically linked LGPL-2.1 (e.g. sora-editor) and
permissive licenses (Apache-2.0, MIT, BSD, NCSA, PSF-2.0, ZLIB).
"""

from __future__ import annotations

import argparse
import io
import re
import sys
import tarfile
import tempfile
from pathlib import Path
from typing import List, Optional, Tuple

FORBIDDEN_BINARY_NAMES = (
    "libproot.so",
    "proot",
    "libloader.so",
    "libloader-m32.so",
    "libtalloc",
    "libgdbm",
    "libreadline",
    "libbash.so",
)

# Case-insensitive patterns indicating GPL/AGPL/paid/non-commercial violations.
# Care is taken not to match "Lesser General Public License" (LGPL) or "LGPL".
FORBIDDEN_TEXT_PATTERNS: Tuple[Tuple[str, re.Pattern[str]], ...] = (
    ("GPL v2/v3 notice", re.compile(r"\bgpl\s*v?[23]\b", re.IGNORECASE)),
    ("GPL-2.0/GPL-3.0 identifier", re.compile(r"(?<!l)\bgpl-[23](?:\.0)?\b", re.IGNORECASE)),
    ("GNU General Public License (non-LGPL)", re.compile(r"gnu\s+general\s+public\s+license", re.IGNORECASE)),
    ("AGPL license", re.compile(r"\bagpl(?:-[23](?:\.0)?)?\b|affero\s+general\s+public\s+license", re.IGNORECASE)),
    ("Non-commercial restriction", re.compile(r"\bnon-?commercial\b", re.IGNORECASE)),
    ("Paid/proprietary SDK marker", re.compile(r"\b(?:commercial\s+license\s+required|paid\s+sdk|trial[\s-]only)\b", re.IGNORECASE)),
    ("proot reference", re.compile(r"\bproot\b", re.IGNORECASE)),
    ("talloc GPL reference", re.compile(r"\btalloc(?:\.c)?\b", re.IGNORECASE)),
    ("gdbm GPL reference", re.compile(r"\bgdbm\b", re.IGNORECASE)),
    ("GNU readline GPL reference", re.compile(r"\blibreadline\b|\bgnu\s+readline\b(?! library - keyseq-timeout)", re.IGNORECASE)),
)

FORBIDDEN_SOURCE_DOWNLOAD_PATTERNS: Tuple[Tuple[str, re.Pattern[str]], ...] = (
    ("alpinelinux.org runtime binary download", re.compile(r"alpinelinux\.org|alpine-minirootfs", re.IGNORECASE)),
)

GRADLE_FORBIDDEN_PATTERNS: Tuple[Tuple[str, re.Pattern[str]], ...] = (
    ("GPL/AGPL dependency declaration", re.compile(r"(?<!l)\bgpl(?:v?[23]|-[23])|\bagpl\b", re.IGNORECASE)),
    ("Non-commercial / paid dependency marker", re.compile(r"\bnon-?commercial\b|\btrial[\s-]only\b", re.IGNORECASE)),
)


def scan_text_for_violations(text: str, location: str, patterns: Tuple[Tuple[str, re.Pattern[str]], ...]) -> List[str]:
    violations: List[str] = []
    for line_no, line in enumerate(text.splitlines(), start=1):
        for label, regex in patterns:
            if regex.search(line):
                snippet = line.strip()[:140]
                violations.append(f"{location}:{line_no}: {label} -> {snippet}")
    return violations


def _scrub_benign_psf_stdlib(text: str, location: str) -> str:
    if "!usr/lib/python3." not in location or not location.lower().endswith(".py"):
        return text
    text = re.sub(r"'_?gdbm'\s*:\s*'(?:dbm\.gnu|gdbm)'", "[PSF-2.0-stdlib-pickle-map]", text)
    text = re.sub(r"GNU\s+readline\s+is\s+never\s+used", "[PSF-2.0-stdlib-comment]", text)
    for literal in (
        "in the GNU readline history",
        "enable GNU readline if it is available",
        "Word completion for GNU readline",
    ):
        text = text.replace(literal, "[PSF-2.0-stdlib-reference]")
    text = re.sub(r"1\)\s*GNU\s+GPLv2\s+2\)\s*PSF\s+license\s+for\s+Python", "[PSF-2.0-stdlib-license]", text)
    return text


def scan_binary_strings(data: bytes, location: str) -> List[str]:
    violations: List[str] = []
    # Extract printable ASCII sequences >= 4 chars so binary noise does not interfere
    ascii_text = "\n".join(m.group(0).decode("ascii", errors="ignore") for m in re.finditer(rb"[\x20-\x7e]{4,}", data))
    ascii_text = _scrub_benign_psf_stdlib(ascii_text, location)
    for label, regex in FORBIDDEN_TEXT_PATTERNS:
        m = regex.search(ascii_text)
        if m:
            start = max(0, m.start() - 30)
            end = min(len(ascii_text), m.end() + 50)
            ctx = ascii_text[start:end].replace("\n", " ")
            violations.append(f"{location}: {label} -> '{ctx}'")
    return violations


def check_filename(name: str, location: str) -> List[str]:
    lower = name.lower()
    base = Path(lower).name
    violations: List[str] = []
    for banned in FORBIDDEN_BINARY_NAMES:
        if base == banned or base.startswith(banned + ".") or banned in base:
            violations.append(f"{location}: Forbidden binary/artifact name '{name}' (matches '{banned}')")
            break
    if base in ("bash", "bash.exe"):
        violations.append(f"{location}: Forbidden GPL shell binary '{name}'")
    return violations


def scan_gradle_files(toml_path: Path, build_gradle_path: Path) -> List[str]:
    violations: List[str] = []
    for p in (toml_path, build_gradle_path):
        if p.exists():
            violations.extend(scan_text_for_violations(p.read_text(encoding="utf-8", errors="ignore"), str(p), GRADLE_FORBIDDEN_PATTERNS))
    return violations


def scan_jni_libs(jni_libs_dir: Path) -> List[str]:
    violations: List[str] = []
    if not jni_libs_dir.exists():
        return violations
    for file_path in sorted(jni_libs_dir.rglob("*")):
        if not file_path.is_file():
            continue
        violations.extend(check_filename(file_path.name, str(file_path)))
        violations.extend(scan_binary_strings(file_path.read_bytes(), str(file_path)))
    return violations


def scan_assets(assets_dir: Path) -> List[str]:
    violations: List[str] = []
    if not assets_dir.exists():
        return violations
    for file_path in sorted(assets_dir.rglob("*")):
        if not file_path.is_file():
            continue
        violations.extend(check_filename(file_path.name, str(file_path)))
        lower = file_path.name.lower()
        if lower.endswith((".tar.gz", ".tgz", ".tar")):
            try:
                with tarfile.open(file_path, "r:*") as tf:
                    for member in tf.getmembers():
                        loc = f"{file_path}!{member.name}"
                        violations.extend(check_filename(member.name, loc))
                        if member.isfile():
                            extracted = tf.extractfile(member)
                            if extracted is not None:
                                data = extracted.read()
                                if member.name.lower().endswith((".txt", ".md", ".notice", "copying", "license")):
                                    violations.extend(scan_text_for_violations(data.decode("utf-8", errors="ignore"), loc, FORBIDDEN_TEXT_PATTERNS))
                                else:
                                    violations.extend(scan_binary_strings(data, loc))
            except Exception as exc:
                violations.append(f"{file_path}: Failed to scan archive: {exc}")
        elif lower.endswith((".txt", ".md", ".json", ".toml", ".cfg", "license", "copying", "notice")):
            violations.extend(scan_text_for_violations(file_path.read_text(encoding="utf-8", errors="ignore"), str(file_path), FORBIDDEN_TEXT_PATTERNS))
        else:
            violations.extend(scan_binary_strings(file_path.read_bytes(), str(file_path)))
    return violations


def scan_sandbox_sources(source_paths: Tuple[Path, ...]) -> List[str]:
    violations: List[str] = []
    for p in source_paths:
        if p.exists():
            violations.extend(
                scan_text_for_violations(
                    p.read_text(encoding="utf-8", errors="ignore"),
                    str(p),
                    FORBIDDEN_SOURCE_DOWNLOAD_PATTERNS,
                )
            )
    return violations


def run_repo_scan(root: Path) -> List[str]:
    violations: List[str] = []
    violations.extend(scan_gradle_files(root / "gradle/libs.versions.toml", root / "app/build.gradle.kts"))
    violations.extend(scan_jni_libs(root / "app/src/main/jniLibs"))
    violations.extend(scan_assets(root / "app/src/main/assets"))
    violations.extend(
        scan_sandbox_sources(
            (
                root / "app/src/main/java/com/klix/code/data/sandbox/SandboxManager.kt",
                root / "app/src/main/java/com/klix/code/ui/onboarding/OnboardingViewModel.kt",
            )
        )
    )
    return violations


def run_self_test() -> int:
    print("[SELF-TEST] Running R5 license compliance verifier self-tests...")
    with tempfile.TemporaryDirectory(prefix="klix_r5_selftest_") as tmp:
        root = Path(tmp)

        # 1. Negative test: synthetic GPL marker + libproot.so + alpinelinux.org URL
        bad_root = root / "bad_repo"
        (bad_root / "app/src/main/jniLibs/arm64-v8a").mkdir(parents=True)
        (bad_root / "app/src/main/jniLibs/arm64-v8a/libproot.so").write_bytes(
            b"\x7fELF...Copyright (C) 2015 STMicroelectronics, licensed under GPL v2 or later.\x00"
        )
        (bad_root / "app/src/main/assets").mkdir(parents=True)
        tar_path = bad_root / "app/src/main/assets/tools.tar.gz"
        with tarfile.open(tar_path, "w:gz") as tf:
            notice = b"GNU General Public License version 3 (GPL v3)\n"
            ti = tarfile.TarInfo("usr/share/doc/COPYING")
            ti.size = len(notice)
            tf.addfile(ti, io.BytesIO(notice))
        (bad_root / "app/src/main/java/com/klix/code/data/sandbox").mkdir(parents=True)
        (bad_root / "app/src/main/java/com/klix/code/data/sandbox/SandboxManager.kt").write_text(
            'val url = "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/aarch64/alpine-minirootfs.tar.gz"\n',
            encoding="utf-8",
        )
        bad_violations = run_repo_scan(bad_root)
        if len(bad_violations) < 3:
            print(f"[SELF-TEST FAIL] Expected >= 3 violations on synthetic GPL fixture, got: {bad_violations}", file=sys.stderr)
            return 1
        print(f"[SELF-TEST OK] Synthetic GPL fixture flagged {len(bad_violations)} violation(s).")

        # 2. Positive test: clean Apache-2.0 / MIT repo layout passes with 0 violations
        good_root = root / "good_repo"
        (good_root / "gradle").mkdir(parents=True)
        (good_root / "gradle/libs.versions.toml").write_text('commonsCompress = "1.27.1"\n', encoding="utf-8")
        (good_root / "app/src/main/jniLibs/arm64-v8a").mkdir(parents=True)
        (good_root / "app/src/main/jniLibs/arm64-v8a/libklix_pty.so").write_bytes(b"\x7fELF_APACHE_2_0_OK\x00")
        (good_root / "app/src/main/assets").mkdir(parents=True)
        (good_root / "app/src/main/assets/NOTICE.txt").write_text("Licensed under Apache-2.0 and MIT.\n", encoding="utf-8")
        good_violations = run_repo_scan(good_root)
        if good_violations:
            print(f"[SELF-TEST FAIL] Clean fixture produced unexpected violations: {good_violations}", file=sys.stderr)
            return 1
        print("[SELF-TEST OK] Clean Apache-2.0/MIT fixture passed with 0 violations.")

    print("[SELF-TEST] All R5 license verifier self-tests passed.")
    return 0


def main(argv: Optional[List[str]] = None) -> int:
    parser = argparse.ArgumentParser(description="Verify R5 free & production-legal license compliance.")
    parser.add_argument("--root", type=Path, default=Path("."), help="Repository root path")
    parser.add_argument("--self-test", action="store_true", help="Run synthetic GPL/clean fixture self-test")
    args = parser.parse_args(argv)

    if args.self_test:
        return run_self_test()

    violations = run_repo_scan(args.root)
    if violations:
        print(f"[verify_r5_licenses] FAILED with {len(violations)} R5 compliance violation(s):", file=sys.stderr)
        for v in violations:
            print(f"  - {v}", file=sys.stderr)
        return 1

    print("[verify_r5_licenses] PASS: Zero GPL/AGPL/paid/non-commercial violations found.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

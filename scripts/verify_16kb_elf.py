#!/usr/bin/env python3
"""
16 KB ELF PT_LOAD alignment & 64-bit ABI compliance verifier for KlixCode.

Checks:
  (a) app/src/main/jniLibs/ contains ONLY arm64-v8a and x86_64 (fails on armeabi-v7a, x86, or 32-bit ELFs).
  (b) Every ELF in jniLibs/, optional --apk (<abi>/*.so and any ELF in APK), and optional --check-assets
      (including inside .tar.gz / .tgz / .tar archives) is ELFCLASS64 with PT_LOAD p_align >= 0x4000 (16384).
  (c) Optional --cross-check-readelf cross-checks PT_LOAD alignment via readelf (native or WSL).
  (d) --self-test generates synthetic 0x1000 and 0x4000 ELF fixtures in a temp dir and verifies
      that 0x1000 fails non-zero while 0x4000 passes (proving no false-pass).
"""

from __future__ import annotations

import argparse
import io
import os
import re
import shutil
import struct
import subprocess
import sys
import tarfile
import tempfile
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List, Optional, Tuple

ELF_MAGIC = b"\x7fELF"
ELFCLASS32 = 1
ELFCLASS64 = 2
ELFDATA2LSB = 1
ELFDATA2MSB = 2
PT_LOAD = 1
MIN_PAGE_ALIGN = 0x4000  # 16384 bytes
ALLOWED_ABIS = {"arm64-v8a", "x86_64"}


@dataclass
class ElfInspectionResult:
    location: str
    ei_class: int
    pt_load_aligns: List[int]
    errors: List[str]

    @property
    def ok(self) -> bool:
        return len(self.errors) == 0


def parse_elf_bytes(data: bytes, location: str) -> ElfInspectionResult:
    errors: List[str] = []
    if len(data) < 52 or data[:4] != ELF_MAGIC:
        return ElfInspectionResult(location, 0, [], [f"{location}: Not a valid ELF binary"])

    ei_class = data[4]
    ei_data = data[5]
    if ei_data == ELFDATA2LSB:
        endian = "<"
    elif ei_data == ELFDATA2MSB:
        endian = ">"
    else:
        return ElfInspectionResult(location, ei_class, [], [f"{location}: Unknown ELF endianness ({ei_data})"])

    e_type = struct.unpack_from(f"{endian}H", data, 16)[0]
    if ei_class == ELFCLASS32:
        errors.append(f"{location}: 32-bit ELF (ELFCLASS32) is forbidden; only 64-bit arm64-v8a/x86_64 allowed")
        e_phoff = struct.unpack_from(f"{endian}I", data, 28)[0]
        e_phentsize = struct.unpack_from(f"{endian}H", data, 42)[0]
        e_phnum = struct.unpack_from(f"{endian}H", data, 44)[0]
        p_align_offset = 28
        p_align_fmt = f"{endian}I"
        min_phentsize = 32
    elif ei_class == ELFCLASS64:
        if len(data) < 64:
            return ElfInspectionResult(location, ei_class, [], [f"{location}: Truncated ELF64 header"])
        e_phoff = struct.unpack_from(f"{endian}Q", data, 32)[0]
        e_phentsize = struct.unpack_from(f"{endian}H", data, 54)[0]
        e_phnum = struct.unpack_from(f"{endian}H", data, 56)[0]
        p_align_offset = 48
        p_align_fmt = f"{endian}Q"
        min_phentsize = 56
    else:
        return ElfInspectionResult(location, ei_class, [], [f"{location}: Unknown ELF class ({ei_class})"])

    # Relocatable object files (ET_REL == 1, e.g. crt*.o) do not have program headers by spec.
    if e_type == 1 and e_phnum == 0:
        return ElfInspectionResult(location, ei_class, [], errors)

    if e_phnum == 0 or e_phentsize < min_phentsize:
        errors.append(f"{location}: Invalid program header table (phnum={e_phnum}, phentsize={e_phentsize})")
        return ElfInspectionResult(location, ei_class, [], errors)

    pt_load_aligns: List[int] = []
    for i in range(e_phnum):
        off = e_phoff + i * e_phentsize
        if off + e_phentsize > len(data):
            errors.append(f"{location}: Truncated program header entry {i}")
            break
        p_type = struct.unpack_from(f"{endian}I", data, off)[0]
        if p_type == PT_LOAD:
            p_align = struct.unpack_from(p_align_fmt, data, off + p_align_offset)[0]
            pt_load_aligns.append(p_align)
            if p_align < MIN_PAGE_ALIGN:
                errors.append(
                    f"{location}: PT_LOAD[{len(pt_load_aligns) - 1}] p_align=0x{p_align:x} ({p_align}) < 0x{MIN_PAGE_ALIGN:x} ({MIN_PAGE_ALIGN})"
                )

    if not pt_load_aligns:
        errors.append(f"{location}: No PT_LOAD segments found")

    return ElfInspectionResult(location, ei_class, pt_load_aligns, errors)


def cross_check_with_readelf(data: bytes, location: str, expected_aligns: List[int]) -> Optional[str]:
    readelf_bin = shutil.which("readelf")
    temp_path: Optional[Path] = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=".elf") as tf:
            tf.write(data)
            temp_path = Path(tf.name)

        if readelf_bin:
            cmd = [readelf_bin, "-l", "-W", str(temp_path)]
        elif shutil.which("wsl"):
            win_fwd = str(temp_path).replace("\\", "/")
            wsl_path = subprocess.check_output(
                ["wsl", "wslpath", "-u", win_fwd],
                text=True,
                stderr=subprocess.STDOUT,
            ).strip()
            cmd = ["wsl", "readelf", "-l", "-W", wsl_path]
        else:
            return f"{location}: --cross-check-readelf requested but neither readelf nor wsl readelf is available"

        out = subprocess.check_output(cmd, text=True, stderr=subprocess.STDOUT)
        readelf_aligns: List[int] = []
        for line in out.splitlines():
            stripped = line.strip()
            if stripped.startswith("LOAD"):
                tokens = stripped.split()
                if tokens:
                    align_tok = tokens[-1]
                    readelf_aligns.append(int(align_tok, 0))
        if readelf_aligns != expected_aligns:
            return (
                f"{location}: readelf cross-check mismatch: python={expected_aligns}, "
                f"readelf={readelf_aligns}"
            )
        return None
    except Exception as exc:
        return f"{location}: readelf cross-check failed: {exc}"
    finally:
        if temp_path and temp_path.exists():
            try:
                temp_path.unlink()
            except OSError:
                pass


def inspect_tar_stream(fileobj: io.BufferedIOBase, archive_label: str, cross_check: bool) -> Tuple[int, List[str]]:
    checked = 0
    errors: List[str] = []
    try:
        with tarfile.open(fileobj=fileobj, mode="r:*") as tf:
            for member in tf.getmembers():
                if not member.isfile():
                    continue
                extracted = tf.extractfile(member)
                if extracted is None:
                    continue
                header = extracted.read(4)
                if header == ELF_MAGIC:
                    data = header + extracted.read()
                    loc = f"{archive_label}!{member.name}"
                    res = parse_elf_bytes(data, loc)
                    checked += 1
                    errors.extend(res.errors)
                    if cross_check:
                        err = cross_check_with_readelf(data, loc, res.pt_load_aligns)
                        if err:
                            errors.append(err)
    except Exception as exc:
        errors.append(f"{archive_label}: Failed to inspect tar archive: {exc}")
    return checked, errors


def check_jni_libs(jni_libs_dir: Path, cross_check: bool) -> Tuple[int, List[str]]:
    checked = 0
    errors: List[str] = []
    if not jni_libs_dir.exists():
        errors.append(f"jniLibs directory does not exist: {jni_libs_dir}")
        return checked, errors

    abi_dirs = [p for p in sorted(jni_libs_dir.iterdir()) if p.is_dir()]
    if not abi_dirs:
        errors.append(f"No ABI directories found under {jni_libs_dir}")
        return checked, errors

    for abi_dir in abi_dirs:
        abi = abi_dir.name
        if abi not in ALLOWED_ABIS:
            errors.append(f"{abi_dir}: Forbidden ABI '{abi}' (allowed: {sorted(ALLOWED_ABIS)})")

        for file_path in sorted(abi_dir.rglob("*")):
            if not file_path.is_file():
                continue
            data = file_path.read_bytes()
            if file_path.suffix == ".so" or data[:4] == ELF_MAGIC:
                res = parse_elf_bytes(data, str(file_path))
                checked += 1
                errors.extend(res.errors)
                if cross_check:
                    err = cross_check_with_readelf(data, str(file_path), res.pt_load_aligns)
                    if err:
                        errors.append(err)
    return checked, errors


def check_assets_dir(assets_dir: Path, cross_check: bool) -> Tuple[int, List[str]]:
    checked = 0
    errors: List[str] = []
    if not assets_dir.exists():
        return checked, errors

    for file_path in sorted(assets_dir.rglob("*")):
        if not file_path.is_file():
            continue
        name_lower = file_path.name.lower()
        if name_lower.endswith((".tar.gz", ".tgz", ".tar")):
            with file_path.open("rb") as f:
                c, e = inspect_tar_stream(f, str(file_path), cross_check)
                checked += c
                errors.extend(e)
        else:
            with file_path.open("rb") as f:
                magic = f.read(4)
            if magic == ELF_MAGIC:
                data = file_path.read_bytes()
                res = parse_elf_bytes(data, str(file_path))
                checked += 1
                errors.extend(res.errors)
                if cross_check:
                    err = cross_check_with_readelf(data, str(file_path), res.pt_load_aligns)
                    if err:
                        errors.append(err)
    return checked, errors


def check_apk(apk_path: Path, cross_check: bool) -> Tuple[int, List[str]]:
    checked = 0
    errors: List[str] = []
    if not apk_path.exists():
        errors.append(f"APK file does not exist: {apk_path}")
        return checked, errors

    seen_abis = set()
    with zipfile.ZipFile(apk_path, "r") as zf:
        for info in zf.infolist():
            if info.is_dir():
                continue
            entry = info.filename
            if entry.startswith("lib/"):
                parts = entry.split("/")
                if len(parts) >= 3:
                    abi = parts[1]
                    if abi not in seen_abis and abi not in ALLOWED_ABIS:
                        errors.append(f"{apk_path}!{entry}: Forbidden ABI '{abi}' inside APK")
                    seen_abis.add(abi)
                data = zf.read(info)
                res = parse_elf_bytes(data, f"{apk_path}!{entry}")
                checked += 1
                errors.extend(res.errors)
                if cross_check:
                    err = cross_check_with_readelf(data, f"{apk_path}!{entry}", res.pt_load_aligns)
                    if err:
                        errors.append(err)
            elif entry.lower().endswith((".tar.gz", ".tgz", ".tar")):
                raw = zf.read(info)
                c, e = inspect_tar_stream(io.BytesIO(raw), f"{apk_path}!{entry}", cross_check)
                checked += c
                errors.extend(e)
            else:
                with zf.open(info) as zmember:
                    magic = zmember.read(4)
                if magic == ELF_MAGIC:
                    data = zf.read(info)
                    res = parse_elf_bytes(data, f"{apk_path}!{entry}")
                    checked += 1
                    errors.extend(res.errors)
                    if cross_check:
                        err = cross_check_with_readelf(data, f"{apk_path}!{entry}", res.pt_load_aligns)
                        if err:
                            errors.append(err)
    return checked, errors


def build_synthetic_elf64(p_align: int) -> bytes:
    """Builds a minimal valid ELF64 header + 1 PT_LOAD program header with specified p_align."""
    ehdr = bytearray(64)
    ehdr[0:4] = ELF_MAGIC
    ehdr[4] = ELFCLASS64
    ehdr[5] = ELFDATA2LSB
    ehdr[6] = 1  # EV_CURRENT
    struct.pack_into("<H", ehdr, 16, 3)     # e_type = ET_DYN
    struct.pack_into("<H", ehdr, 18, 183)   # e_machine = EM_AARCH64
    struct.pack_into("<I", ehdr, 20, 1)     # e_version
    struct.pack_into("<Q", ehdr, 32, 64)    # e_phoff = 64
    struct.pack_into("<H", ehdr, 52, 64)    # e_ehsize = 64
    struct.pack_into("<H", ehdr, 54, 56)    # e_phentsize = 56
    struct.pack_into("<H", ehdr, 56, 1)     # e_phnum = 1

    phdr = bytearray(56)
    struct.pack_into("<I", phdr, 0, PT_LOAD)
    struct.pack_into("<I", phdr, 4, 5)        # PF_R | PF_X
    struct.pack_into("<Q", phdr, 8, 0)        # p_offset
    struct.pack_into("<Q", phdr, 16, 0)       # p_vaddr
    struct.pack_into("<Q", phdr, 24, 0)       # p_paddr
    struct.pack_into("<Q", phdr, 32, 120)     # p_filesz
    struct.pack_into("<Q", phdr, 40, 120)     # p_memsz
    struct.pack_into("<Q", phdr, 48, p_align) # p_align
    return bytes(ehdr + phdr)


def run_self_test() -> int:
    print("[SELF-TEST] Running synthetic 16 KB ELF verifier self-tests...")
    with tempfile.TemporaryDirectory(prefix="klix_elf_selftest_") as tmp:
        root = Path(tmp)

        # 1. Negative test: synthetic 0x1000 ELF inside jniLibs MUST fail non-zero
        bad_jni = root / "bad_jni" / "arm64-v8a"
        bad_jni.mkdir(parents=True)
        (bad_jni / "libbad_4kb.so").write_bytes(build_synthetic_elf64(0x1000))
        _, bad_errors = check_jni_libs(root / "bad_jni", cross_check=False)
        if not bad_errors:
            print("[SELF-TEST FAIL] 0x1000 synthetic ELF produced zero errors (false-pass)!", file=sys.stderr)
            return 1
        print(f"[SELF-TEST OK] Synthetic 0x1000 ELF caught: {bad_errors[0]}")

        # 2. Negative test: synthetic 0x1000 ELF inside assets .tar.gz MUST fail non-zero
        assets_bad = root / "assets_bad"
        assets_bad.mkdir(parents=True)
        tar_path = assets_bad / "sysroot.tar.gz"
        bad_elf = build_synthetic_elf64(0x1000)
        with tarfile.open(tar_path, "w:gz") as tf:
            ti = tarfile.TarInfo("usr/bin/bad_tool")
            ti.size = len(bad_elf)
            tf.addfile(ti, io.BytesIO(bad_elf))
        _, asset_errors = check_assets_dir(assets_bad, cross_check=False)
        if not asset_errors:
            print("[SELF-TEST FAIL] 0x1000 ELF inside assets .tar.gz was not detected!", file=sys.stderr)
            return 1
        print(f"[SELF-TEST OK] Synthetic 0x1000 ELF in .tar.gz caught: {asset_errors[0]}")

        # 3. Positive test: synthetic 0x4000 ELF in arm64-v8a & x86_64 MUST pass
        good_jni = root / "good_jni"
        for abi in ("arm64-v8a", "x86_64"):
            (good_jni / abi).mkdir(parents=True)
            (good_jni / abi / "libgood_16kb.so").write_bytes(build_synthetic_elf64(0x4000))
        checked, good_errors = check_jni_libs(good_jni, cross_check=False)
        if good_errors or checked != 2:
            print(f"[SELF-TEST FAIL] Synthetic 0x4000 ELFs failed unexpectedly: {good_errors}", file=sys.stderr)
            return 1
        print("[SELF-TEST OK] Synthetic 0x4000 ELFs passed.")

    print("[SELF-TEST] All ELF verifier self-tests passed.")
    return 0


def main(argv: Optional[List[str]] = None) -> int:
    parser = argparse.ArgumentParser(description="Verify 16 KB ELF PT_LOAD alignment and 64-bit ABIs.")
    parser.add_argument("--jni-libs", type=Path, default=None, help="Path to jniLibs directory (e.g. app/src/main/jniLibs)")
    parser.add_argument("--apk", type=Path, default=None, help="Optional APK path to inspect")
    parser.add_argument("--check-assets", action="store_true", help="Also scan app/src/main/assets (or --assets) for unaligned ELFs")
    parser.add_argument("--assets", type=Path, default=Path("app/src/main/assets"), help="Assets directory path")
    parser.add_argument("--cross-check-readelf", action="store_true", help="Cross-check PT_LOAD alignment with readelf / wsl readelf")
    parser.add_argument("--self-test", action="store_true", help="Run synthetic 0x1000/0x4000 self-test and exit")
    args = parser.parse_args(argv)

    if args.self_test:
        return run_self_test()

    if args.jni_libs is None and args.apk is None and not args.check_assets:
        args.jni_libs = Path("app/src/main/jniLibs")

    total_checked = 0
    all_errors: List[str] = []

    if args.jni_libs is not None:
        c, e = check_jni_libs(args.jni_libs, args.cross_check_readelf)
        total_checked += c
        all_errors.extend(e)

    if args.apk is not None:
        c, e = check_apk(args.apk, args.cross_check_readelf)
        total_checked += c
        all_errors.extend(e)

    if args.check_assets:
        c, e = check_assets_dir(args.assets, args.cross_check_readelf)
        total_checked += c
        all_errors.extend(e)

    print(f"[verify_16kb_elf] Inspected {total_checked} ELF binaries.")
    if all_errors:
        print(f"[verify_16kb_elf] FAILED with {len(all_errors)} violation(s):", file=sys.stderr)
        for err in all_errors:
            print(f"  - {err}", file=sys.stderr)
        return 1

    print("[verify_16kb_elf] PASS: All binaries are 64-bit (arm64-v8a/x86_64) with PT_LOAD p_align >= 0x4000.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

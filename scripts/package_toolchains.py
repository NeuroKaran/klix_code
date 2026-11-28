#!/usr/bin/env python3
"""
Split arm64-v8a native toolchains into:
  1. Slim base assets/sandbox-data.tgz (Python 3.12 stdlib + SSL certs only)
  2. On-demand Node.js bundle: app/build/outputs/toolchains/klix-toolchain-nodejs-arm64-v8a.tar.gz
  3. On-demand C/C++ bundle: app/build/outputs/toolchains/klix-toolchain-cpp-arm64-v8a.tar.gz
And strip x86_64 + non-Python libs from app/src/main/jniLibs.
"""
from __future__ import annotations

import hashlib
import io
import shutil
import tarfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JNI_ARM64 = ROOT / "app" / "src" / "main" / "jniLibs" / "arm64-v8a"
JNI_X86_64 = ROOT / "app" / "src" / "main" / "jniLibs" / "x86_64"
ASSETS_TGZ = ROOT / "app" / "src" / "main" / "assets" / "sandbox-data.tgz"
OUT_DIR = ROOT / "app" / "build" / "outputs" / "toolchains"

NODE_LIBS = [
    "libnode.so",
    "libicudata.so",
    "libicui18n.so",
    "libicuuc.so",
    "libcares.so",
    "libc++_shared.so",
]

CPP_LIBS = [
    "libLLVM.so",
    "libclang-cpp.so",
    "libclang.so",
    "libxml2.so",
    "libiconv.so",
    "libcharset.so",
    "libzstd.so",
    "libc++_shared.so",
    "libandroid-glob.so",
]

KEEP_ARM64_LIBS = {
    "libpython3.so",
    "libdash.so",
    "libz.so",
    "libbz2.so",
    "libffi.so",
    "libsqlite3.so",
    "libssl.so",
    "libcrypto.so",
    "libexpat.so",
}


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def add_local_file(tf: tarfile.TarFile, src: Path, arcname: str, mode: int = 0o755) -> None:
    data = src.read_bytes()
    ti = tarfile.TarInfo(name=arcname)
    ti.size = len(data)
    ti.mode = mode
    tf.addfile(ti, io.BytesIO(data))


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    orig_bytes = ASSETS_TGZ.read_bytes()

    # 1. Node.js on-demand bundle
    node_tgz = OUT_DIR / "klix-toolchain-nodejs-arm64-v8a.tar.gz"
    with tarfile.open(node_tgz, "w:gz", compresslevel=9) as tf:
        for lib in NODE_LIBS:
            src = JNI_ARM64 / lib
            if not src.exists():
                raise FileNotFoundError(f"Missing {src}")
            add_local_file(tf, src, f"lib/{lib}")

    # 2. C/C++ (Clang/LLVM/LLD + sysroot) on-demand bundle
    cpp_tgz = OUT_DIR / "klix-toolchain-cpp-arm64-v8a.tar.gz"
    with tarfile.open(cpp_tgz, "w:gz", compresslevel=9) as tf:
        for lib in CPP_LIBS:
            src = JNI_ARM64 / lib
            if not src.exists():
                raise FileNotFoundError(f"Missing {src}")
            add_local_file(tf, src, f"lib/{lib}")

        lld_src = JNI_ARM64 / "liblld.so"
        if not lld_src.exists():
            raise FileNotFoundError(f"Missing {lld_src}")
        add_local_file(tf, lld_src, "lib/ld.lld")
        link = tarfile.TarInfo(name="lib/liblld.so")
        link.type = tarfile.SYMTYPE
        link.linkname = "ld.lld"
        link.mode = 0o777
        tf.addfile(link)

        # Copy usr/sysroot and usr/lib/clang from original sandbox-data.tgz
        with tarfile.open(fileobj=io.BytesIO(orig_bytes), mode="r:gz") as orig_tf:
            for m in orig_tf.getmembers():
                norm = m.name.lstrip("./")
                if norm.startswith("usr/sysroot") or norm.startswith("usr/lib/clang"):
                    f = orig_tf.extractfile(m) if m.isfile() else None
                    tf.addfile(m, f)

    # 3. Slim base sandbox-data.tgz (Python 3.12 stdlib + SSL certs only)
    slim_tmp = ASSETS_TGZ.with_suffix(".tgz.tmp")
    with tarfile.open(slim_tmp, "w:gz", compresslevel=9) as tf:
        with tarfile.open(fileobj=io.BytesIO(orig_bytes), mode="r:gz") as orig_tf:
            for m in orig_tf.getmembers():
                norm = m.name.lstrip("./")
                if norm.startswith("usr/lib/python3") or norm.startswith("etc/ssl"):
                    f = orig_tf.extractfile(m) if m.isfile() else None
                    tf.addfile(m, f)
    shutil.move(slim_tmp, ASSETS_TGZ)

    # 4. Remove x86_64 and non-base arm64 libs from jniLibs
    if JNI_X86_64.exists():
        shutil.rmtree(JNI_X86_64)

    for item in list(JNI_ARM64.iterdir()):
        if item.name not in KEEP_ARM64_LIBS:
            item.unlink()

    for label, p in [
        ("slim sandbox-data.tgz", ASSETS_TGZ),
        ("nodejs bundle", node_tgz),
        ("cpp bundle", cpp_tgz),
    ]:
        print(f"{label:24s} | {p.stat().st_size:10d} bytes ({p.stat().st_size/1e6:6.2f} MB) | sha256={sha256_file(p)}")


if __name__ == "__main__":
    main()


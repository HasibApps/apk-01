#!/usr/bin/env python3
"""Gradle বিল্ড লগ থেকে এরর লাইনগুলো বের করে annotation + summary আকারে দেখায়।"""
import os
import re

PATTERN = re.compile(
    r"^(e: file:|e: |error:|FAILURE: Build failed|> Task .* FAILED|"
    r".*What went wrong|Caused by:|.*Could not (resolve|find|download|GET)|"
    r".*Unresolved reference|AAPT: error|AAPT2: error|.*Execution failed for task|"
    r".*Dependency requires|.*is not compatible with|.*requires (JDK|Java))"
)

MAX_CHARS = 50000
SUMMARY = os.environ.get("GITHUB_STEP_SUMMARY")
LOG = os.environ.get("BUILD_LOG", "build.log")


def main() -> int:
    try:
        text = open(LOG, encoding="utf-8", errors="replace").read()
    except FileNotFoundError:
        text = ""
    lines, seen = [], set()
    for raw in text.splitlines():
        line = raw.strip()
        if line and PATTERN.match(line) and line not in seen:
            seen.add(line)
            lines.append(line)
            if len(lines) >= 400:
                break
    body = "\n".join(lines) if lines else "build.log এ কোনো এরর লাইন পাওয়া যায়নি।"

    if SUMMARY:
        with open(SUMMARY, "a", encoding="utf-8") as fh:
            fh.write("## ❌ Gradle build failed\n\n```\n")
            fh.write(body[:MAX_CHARS])
            fh.write("\n```\n")

    # Escaping for workflow command: % , CR , LF
    esc = body.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
    print("::error title=Gradle build failed::" + esc[:4000])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

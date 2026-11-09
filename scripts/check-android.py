#!/usr/bin/env python3
"""One Gradle invocation, bounded console output, complete logs; never retries."""
import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
TEST_ROOT = "app/src/test/java/"
# Only reviewed, narrow presentation/transport scopes belong here.
# Storage, engine, workout lifecycle, dependencies and unknown Android files use full regression.
SCOPES = {
    "app/src/main/java/com/example/data/ChantalHomePresence.kt": ["ChantalHomePresenceTest", "ChantalNarrativeUiTest", "ChantalStageSixRegressionTest"],
    "app/src/main/java/com/example/ui/ChantalHomePresenceView.kt": ["ChantalNarrativeUiTest", "WorkoutChantalAccessibilityTest"],
    "app/src/main/java/com/example/ui/ChantalAgeProgressPanel.kt": ["ChantalAgeProgressTest", "ChantalNarrativeUiTest", "WorkoutChantalAccessibilityTest"],
    "app/src/main/java/com/example/ui/ChantalStoryFeedbackView.kt": ["ChantalStoryFeedbackUiTest", "ChantalNarrativeUiTest"],
    "app/src/main/java/com/example/domain/ChantalVisualState.kt": ["ChantalVisualStateTest", "ChantalAnimatedCompanionTest"],
    "app/src/main/java/com/example/data/ChantalOpenRouter.kt": ["ChantalOpenRouterTest", "ChantalNarrativeSessionTest", "ChantalNarrativeUiTest"],
    "app/src/main/java/com/example/data/ChantalGeminiAccess.kt": ["ChantalGeminiAccessTest", "ChantalGeminiSettingsTest", "ChantalOpenRouterTest"],
    "app/src/main/java/com/example/data/ChantalNarrativeGuide.kt": ["ChantalGenerationTest", "ChantalNarrativeSessionTest", "ChantalStoryPreviewTest"],
}
EXPECTED_CERT = "7cb0deb615308fa329eaba824b772767a93f1d2587dec50f6958a9e42794d8c7"


def changed_paths(base):
    # Include staged, unstaged, deleted and untracked files; committed changes via --base.
    tracked = subprocess.check_output(["git", "diff", "--name-only", "-z", base, "--"], cwd=ROOT, stderr=subprocess.PIPE)
    untracked = subprocess.check_output(["git", "ls-files", "--others", "--exclude-standard", "-z"], cwd=ROOT)
    return sorted(set(os.fsdecode(p) for p in (tracked + untracked).split(b"\0") if p))


def select(paths):
    tests = set()
    for path in paths:
        p = Path(path)
        if path.startswith(TEST_ROOT) and p.suffix == ".kt":
            # A deleted test/helper may affect others: no silent skip.
            if not (ROOT / path).is_file() or not p.stem.endswith("Test"):
                return None, "test helper/deletion"
            tests.add(".".join(p.with_suffix("").parts[4:]))
        elif path in SCOPES:
            tests.update("com.example." + t for t in SCOPES[path])
        elif (path.startswith("app/")
              or p.name.endswith((".gradle", ".gradle.kts"))
              or path.startswith("gradle/") or p.name in {"gradle.properties", "gradlew", "gradlew.bat"}):
            return None, "critical or unmapped Android change: " + path
    return sorted(tests), "reviewed related tests" if tests else "no Android changes"


def parse_reports(directory):
    totals = dict(tests=0, failures=0, errors=0, skipped=0)
    failures = []
    files = sorted(directory.glob("TEST-*.xml"))
    for file in files:
        suite = ET.parse(file).getroot()
        for key in totals:
            totals[key] += int(suite.get(key, "0"))
        for case in suite.iter("testcase"):
            for failure in list(case.findall("failure")) + list(case.findall("error")):
                failures.append((suite.get("name", "") + "." + case.get("name", ""),
                                 failure.get("message", "")[:600]))
    return totals, failures, files


def verify_signature(output):
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    candidates = list((Path(sdk) / "build-tools").glob("*/apksigner")) if sdk else []
    signer = shutil.which("apksigner")
    if candidates:
        signer = str(max(candidates, key=lambda p: tuple(int(n) for n in re.findall(r"\d+", p.parent.name))))
    if not signer:
        raise RuntimeError("apksigner unavailable; signature verification is mandatory for release")
    result = subprocess.run([signer, "verify", "--verbose", "--print-certs",
                             str(ROOT / "app/build/outputs/apk/debug/app-debug.apk")],
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    (output / "signing.log").write_text(result.stdout)
    certs = re.findall(r"certificate SHA-256 digest: ([0-9a-fA-F]+)", result.stdout)
    if result.returncode or certs != [EXPECTED_CERT] or "Number of signers: 1" not in result.stdout:
        raise RuntimeError("APK signature verification failed; see signing.log")


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", nargs="?", default="changed", choices=["changed", "related", "compile", "full", "release"])
    parser.add_argument("tests", nargs="*", help="Test classes or Gradle patterns (related mode only)")
    parser.add_argument("--base", default="HEAD", help="Git reference for changed mode")
    parser.add_argument("--dry-run", action="store_true", help="Show selected tasks/tests without invoking Gradle")
    args = parser.parse_args(argv)
    if args.tests and args.mode != "related":
        parser.error("test filters are allowed only in related mode")
    tests, reason = [], args.mode
    if args.mode == "changed":
        try:
            tests, reason = select(changed_paths(args.base))
        except (OSError, subprocess.CalledProcessError):
            print("FAIL: cannot read Git changes; check repository and --base")
            return 2
    elif args.mode == "related":
        if not args.tests:
            parser.error("related requires at least one test class/pattern")
        tests = sorted(set(t if "." in t else "com.example." + t for t in args.tests))
    elif args.mode in {"full", "release"}:
        tests = None
    tasks = [":app:compileDebugKotlin"] if args.mode == "compile" else [":app:testDebugUnitTest"]
    if args.mode == "release":
        tasks.append(":app:assembleDebug")
    if args.mode == "changed" and tests == []:
        tasks = []
    selection = dict(mode=args.mode, reason=reason, tasks=tasks, tests=tests, apk_requested=args.mode == "release")
    if args.dry_run:
        print(json.dumps(selection, ensure_ascii=False))
        return 0
    if not tasks:
        print("SKIP: no Android changes. No Gradle/APK run.")
        return 0
    output = ROOT / "build/checks" / (datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S") + "-" + str(os.getpid()))
    output.mkdir(parents=True)
    log = output / "gradle.log"
    cmd = [os.environ.get("ANDROID_GRADLE_BIN", str(ROOT / "gradlew"))]
    cmd += shlex.split(os.environ.get("ANDROID_GRADLE_ARGS", ""))
    cmd += tasks
    for test in tests or []:
        cmd += ["--tests", test]
    cmd += ["--console=plain"]
    print("Checking: " + reason + "; " + (str(len(tests)) + " selected classes/patterns" if tests else "full scope"), flush=True)
    started = time.time()
    try:
        with log.open("w") as stream:
            code = subprocess.run(cmd, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT).returncode
        content = log.read_text(errors="replace")
        totals, failures, files = dict(tests=0, failures=0, errors=0, skipped=0), [], []
        report_origin = "not run"
        if ":app:testDebugUnitTest" in tasks:
            if re.search(r"> Task :app:testDebugUnitTest (UP-TO-DATE|FROM-CACHE)\b", content):
                report_origin = "Gradle cache/up-to-date"
            elif re.search(r"> Task :app:testDebugUnitTest(?:\s|$)", content):
                report_origin = "this run"
            if report_origin != "not run":
                totals, failures, files = parse_reports(ROOT / "app/build/test-results/testDebugUnitTest")
                # Never count old XML from a failed compilation/test JVM startup as new results.
                if code and (report_origin != "this run" or not any(p.stat().st_mtime >= started for p in files)):
                    totals, failures, files = dict(tests=0, failures=0, errors=0, skipped=0), [], []
                    report_origin = "no current test reports"
                if code == 0 and (not files or totals["tests"] == 0 or totals["failures"] or totals["errors"]):
                    raise RuntimeError("Gradle succeeded without valid passing test reports")
                if files:
                    (output / "xml").mkdir()
                    for file in files:
                        shutil.copy2(file, output / "xml" / file.name)
        if code == 0 and ":app:testDebugUnitTest" in tasks and not files:
            raise RuntimeError("No test reports: refusing to report a passing regression")
        apk_built = args.mode == "release" and code == 0
        if apk_built:
            verify_signature(output)
        summary = dict(selection, **totals, filters=tests, passed=totals["tests"]-totals["failures"]-totals["errors"]-totals["skipped"],
                       exit_code=code, report_origin=report_origin, apk_built=apk_built,
                       seconds=round(time.time()-started, 1), log=str(log.relative_to(ROOT)))
        (output / "summary.json").write_text(json.dumps(summary, indent=2, ensure_ascii=False) + "\n")
        print(("PASS" if code == 0 else "FAIL") + f": passed={summary['passed']} failed={totals['failures']+totals['errors']} skipped={totals['skipped']}; reports={report_origin}; APK={'requested' if args.mode == 'release' else 'not requested'}")
        print("Logs: " + str(output.relative_to(ROOT)))
        if code:
            for name, message in failures[:3]:
                print(name + ": " + message)
            if not failures:
                # Build/JVM errors: bounded excerpt, full details stay on disk.
                lines = content.splitlines()
                anchors = [i for i, line in enumerate(lines) if any(s in line for s in ("e: ", "What went wrong", "FAILURE:", "error:"))]
                start = anchors[0] if anchors else max(0, len(lines)-15)
                print("\n".join(lines[start:start+15])[:2500])
        return code
    except (OSError, RuntimeError, ET.ParseError) as error:
        (output / "summary.json").write_text(json.dumps(dict(selection, exit_code=1, error=str(error)), indent=2) + "\n")
        print("FAIL: " + str(error) + "; logs: " + str(output.relative_to(ROOT)))
        return 1


if __name__ == "__main__":
    sys.exit(main())

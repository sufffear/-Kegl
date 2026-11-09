"""Runner safety tests. No Java, network, Android installation or APK build required."""
import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("check_android", Path(__file__).parents[1] / "check-android.py")
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)


class SelectionTest(unittest.TestCase):
    def test_narrow_source_includes_integration_tests(self):
        tests, _ = runner.select(["app/src/main/java/com/example/data/ChantalHomePresence.kt"])
        self.assertIn("com.example.ChantalHomePresenceTest", tests)
        self.assertIn("com.example.ChantalStageSixRegressionTest", tests)

    def test_critical_or_unknown_source_requires_full_regression(self):
        for path in ["app/src/main/java/com/example/data/ChantalProgressRepository.kt",
                     "app/src/main/java/com/example/ui/Unknown.kt", "app/build.gradle.kts",
                     "gradle/libs.versions.toml", "app/src/main/res/values/colors.xml", "app/proguard-rules.pro",
                     "app/src/main/java/other/package/ChantalHomePresence.kt"]:
            self.assertIsNone(runner.select([path])[0])

    def test_scope_map_targets_existing_sources_and_tests(self):
        for source, tests in runner.SCOPES.items():
            self.assertTrue((runner.ROOT / source).is_file(), source)
            for test in tests:
                self.assertTrue((runner.ROOT / runner.TEST_ROOT / "com/example" / (test + ".kt")).is_file(), test)

    def test_test_package_and_no_android_changes(self):
        tests, _ = runner.select(["app/src/test/java/com/example/ChantalOpenRouterTest.kt"])
        self.assertEqual(["com.example.ChantalOpenRouterTest"], tests)
        self.assertEqual([], runner.select(["docs/example.md", "scripts/check-android.py"])[0])
        self.assertIsNone(runner.select(["app/src/test/java/com/example/DeletedTest.kt"])[0])

    def test_git_includes_untracked_and_deleted_files(self):
        with patch.object(runner.subprocess, "check_output", side_effect=[b"deleted.kt\0staged.kt\0", b"untracked.kt\0"]):
            self.assertEqual(["deleted.kt", "staged.kt", "untracked.kt"], runner.changed_paths("HEAD~1"))


class ExecutionTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.root_patch = patch.object(runner, "ROOT", self.root)
        self.root_patch.start()
        self.addCleanup(self.root_patch.stop)
        self.addCleanup(self.temp.cleanup)
        self.stdout = io.StringIO()
        self.calls = []

    def fake_gradle(self, cmd, **kwargs):
        self.calls.append(cmd)
        kwargs['stdout'].write("> Task :app:testDebugUnitTest\n")
        report = self.root / "app/build/test-results/testDebugUnitTest/TEST-sample.xml"
        report.parent.mkdir(parents=True, exist_ok=True)
        report.write_text('<testsuite tests="3" failures="0" errors="0" skipped="1"/>')
        return subprocess.CompletedProcess(cmd, 0)

    def run_main(self, args, fake=None):
        with contextlib.redirect_stdout(self.stdout), patch.object(runner.subprocess, "run", side_effect=fake or self.fake_gradle):
            return runner.main(args)

    def test_related_is_single_invocation_without_packaging(self):
        self.assertEqual(0, self.run_main(["related", "ChantalOpenRouterTest"]))
        self.assertEqual(1, len(self.calls))
        self.assertIn("com.example.ChantalOpenRouterTest", self.calls[0])
        self.assertNotIn(":app:assembleDebug", self.calls[0])
        self.assertIn("passed=2 failed=0 skipped=1", self.stdout.getvalue())
        self.assertTrue(list((self.root / "build/checks").glob("*/xml/TEST-*.xml")))

    def test_release_keeps_all_tests_and_mandatory_signature(self):
        with patch.object(runner, "verify_signature") as verify:
            self.assertEqual(0, self.run_main(["release"]))
            self.assertEqual(1, len(self.calls))
            self.assertIn(":app:assembleDebug", self.calls[0])
            self.assertNotIn("--tests", self.calls[0])
            verify.assert_called_once()

    def test_full_never_packages_apk(self):
        self.assertEqual(0, self.run_main(["full"]))
        self.assertNotIn("--tests", self.calls[0])
        self.assertNotIn(":app:assembleDebug", self.calls[0])

    def test_failure_does_not_retry_or_count_stale_reports(self):
        report = self.root / "app/build/test-results/testDebugUnitTest/TEST-old.xml"
        report.parent.mkdir(parents=True)
        report.write_text('<testsuite tests="999" failures="0"/>')
        os.utime(report, (1, 1))
        def fail(cmd, **kwargs):
            self.calls.append(cmd)
            kwargs['stdout'].write('FAILURE: compilation failed\n')
            return subprocess.CompletedProcess(cmd, 1)
        self.assertEqual(1, self.run_main(["full"], fail))
        self.assertEqual(1, len(self.calls))
        self.assertIn("passed=0", self.stdout.getvalue())

    def test_signature_failure_blocks_release(self):
        with patch.object(runner, "verify_signature", side_effect=RuntimeError("bad certificate")):
            self.assertEqual(1, self.run_main(["release"]))
        self.assertEqual(1, len(self.calls))

    def test_success_without_reports_is_rejected(self):
        def empty(cmd, **kwargs):
            return subprocess.CompletedProcess(cmd, 0)
        self.assertEqual(1, self.run_main(["full"], empty))

    def test_failed_test_returns_nonzero_and_bounded_diagnostic(self):
        def fail(cmd, **kwargs):
            self.fake_gradle(cmd, **kwargs)
            report = self.root / "app/build/test-results/testDebugUnitTest/TEST-sample.xml"
            report.write_text('<testsuite name="Sample" tests="1" failures="1" errors="0"><testcase name="broken"><failure message="expected 1"/></testcase></testsuite>')
            return subprocess.CompletedProcess(cmd, 1)
        self.assertEqual(1, self.run_main(["related", "SampleTest"], fail))
        self.assertEqual(1, len(self.calls))
        self.assertIn("failed=1", self.stdout.getvalue())
        self.assertIn("Sample.broken: expected 1", self.stdout.getvalue())

    def test_signature_verifier_rejects_wrong_multiple_and_invalid_signatures(self):
        def result(text, code=0):
            return subprocess.CompletedProcess([], code, stdout=text)
        good = "Number of signers: 1\nSigner #1 certificate SHA-256 digest: " + runner.EXPECTED_CERT + "\n"
        with patch.object(runner.shutil, "which", return_value="fake-apksigner"), patch.dict(os.environ, {"ANDROID_HOME": str(self.root)}):
            for text, code in [(good.replace(runner.EXPECTED_CERT, "0"*64), 0), (good+good, 0), (good, 1)]:
                with patch.object(runner.subprocess, "run", return_value=result(text, code)):
                    with self.assertRaises(RuntimeError):
                        runner.verify_signature(self.root)
            with patch.object(runner.subprocess, "run", return_value=result(good)):
                runner.verify_signature(self.root)

    def test_cached_results_are_labelled_not_claimed_as_executed(self):
        def cached(cmd, **kwargs):
            result = self.fake_gradle(cmd, **kwargs)
            kwargs['stdout'].write('> Task :app:testDebugUnitTest UP-TO-DATE\n')
            return result
        self.assertEqual(0, self.run_main(["full"], cached))
        self.assertIn("cache/up-to-date", self.stdout.getvalue())


if __name__ == "__main__":
    unittest.main()

from __future__ import annotations

import subprocess
import tempfile
import unittest
from pathlib import Path

from validate_release import (
    ValidationError,
    extract_gradle_version,
    extract_tv_gradle_version,
    parse_tag,
    previous_tag_tv_version_code,
    previous_tag_version_code,
    read_minimum_supported_version_code,
    validate_release,
)


REPOSITORY = Path(__file__).resolve().parents[2]


def gradle_source(name: str = "1.0.11", code: int = 42) -> str:
    return (
        f'extra["bitchordVersionCode"] = {code}\n'
        f'extra["bitchordVersionName"] = "{name}"\n'
    )


def tv_gradle_source(name: str = "1.9", code: int = 27) -> str:
    return f'versionCode = {code}\nversionName = "{name}"\n'


class ReleaseValidatorTest(unittest.TestCase):
    def test_accepts_plain_semver_release_tag(self) -> None:
        self.assertEqual((1, 0, 11), parse_tag("v1.0.11"))

    def test_rejects_non_release_tag_syntax(self) -> None:
        for tag in ("1.0.11", "v1.0", "v1.0.11-beta1", "v01.0.11"):
            with self.subTest(tag=tag), self.assertRaises(ValidationError):
                parse_tag(tag)

    def test_extracts_phone_version_name_and_code(self) -> None:
        self.assertEqual(("1.0.11", 42), extract_gradle_version(gradle_source()))

    def test_rejects_missing_or_duplicate_phone_version_declarations(self) -> None:
        for source in (
            "plugins { }\n",
            gradle_source() + 'extra["bitchordVersionCode"] = 43\n',
        ):
            with self.subTest(source=source), self.assertRaises(ValidationError):
                extract_gradle_version(source)

    def test_extracts_positive_numeric_tv_metadata(self) -> None:
        self.assertEqual(("1.9", 27), extract_tv_gradle_version(tv_gradle_source()))

    def test_rejects_missing_duplicate_or_invalid_tv_metadata(self) -> None:
        for source in (
            "android { }\n",
            tv_gradle_source() + "versionCode = 28\n",
            tv_gradle_source(name="1.9-beta"),
            tv_gradle_source(code=0),
        ):
            with self.subTest(source=source), self.assertRaises(ValidationError):
                extract_tv_gradle_version(source)

    def test_phone_tv_release_metadata_contract(self) -> None:
        phone_source = (REPOSITORY / "build.gradle.kts").read_text(encoding="utf-8")
        tv_source = (REPOSITORY / "tv/build.gradle.kts").read_text(encoding="utf-8")
        phone_name, phone_code = extract_gradle_version(phone_source)
        tv_name, tv_code = extract_tv_gradle_version(tv_source)
        release = validate_release(f"v{phone_name}", phone_source, tv_source)
        self.assertGreater(phone_code, 0)
        self.assertEqual(phone_name, release.version_name)
        self.assertEqual(("1.9", 27), (release.tv_version_name, release.tv_version_code))
        self.assertEqual((tv_name, tv_code), (release.tv_version_name, release.tv_version_code))

    def test_rejects_tag_that_differs_from_phone_version_name(self) -> None:
        with self.assertRaisesRegex(ValidationError, "does not match"):
            validate_release("v1.0.11", gradle_source(name="1.0.10"), tv_gradle_source())

    def test_requires_phone_and_tv_version_codes_to_increase(self) -> None:
        with self.assertRaisesRegex(ValidationError, "Phone Gradle versionCode.*must exceed"):
            validate_release("v1.0.11", gradle_source(code=41), tv_gradle_source(), previous_version_code=41)
        with self.assertRaisesRegex(ValidationError, "TV Gradle versionCode.*must exceed"):
            validate_release(
                "v1.0.11", gradle_source(), tv_gradle_source(), previous_tv_version_code=27
            )

    def test_accepts_monotonic_phone_and_tv_version_codes(self) -> None:
        release = validate_release(
            "v1.0.11",
            gradle_source(),
            tv_gradle_source(),
            previous_version_code=41,
            previous_tv_version_code=26,
        )
        self.assertEqual((42, "1.9", 27), (release.version_code, release.tv_version_name, release.tv_version_code))

    def test_minimum_supported_version_code_is_optional_and_bounded_by_release(self) -> None:
        optional = validate_release("v1.0.11", gradle_source(), tv_gradle_source())
        mandatory = validate_release(
            "v1.0.11", gradle_source(), tv_gradle_source(), minimum_supported_version_code=41
        )
        self.assertEqual(0, optional.minimum_supported_version_code)
        self.assertEqual(41, mandatory.minimum_supported_version_code)
        with self.assertRaisesRegex(ValidationError, "no greater than"):
            validate_release("v1.0.11", gradle_source(), tv_gradle_source(), minimum_supported_version_code=43)

    def test_release_policy_rejects_malformed_negative_and_duplicate_values(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            policy = Path(temp) / "policy.json"
            for contents in (
                "[]",
                '{"minimum_supported_android_phone_version_code": true}',
                '{"minimum_supported_android_phone_version_code": -1}',
                '{"minimum_supported_android_phone_version_code": 1, "minimum_supported_android_phone_version_code": 2}',
            ):
                policy.write_text(contents, encoding="utf-8")
                with self.subTest(contents=contents), self.assertRaises(ValidationError):
                    read_minimum_supported_version_code(policy)
            policy.write_text('{"minimum_supported_android_phone_version_code": 41}', encoding="utf-8")
            self.assertEqual(41, read_minimum_supported_version_code(policy))

    def test_rejects_tag_not_newer_than_latest_published_release(self) -> None:
        with self.assertRaisesRegex(ValidationError, "not newer"):
            validate_release(
                "v1.0.10", gradle_source(name="1.0.10"), tv_gradle_source(), latest_release_tag="v1.0.10"
            )

    def test_rejects_existing_release(self) -> None:
        with self.assertRaisesRegex(ValidationError, "already exists"):
            validate_release("v1.0.11", gradle_source(), tv_gradle_source(), release_already_exists=True)

    def test_reads_phone_and_tv_codes_from_most_recent_prior_git_tag(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            subprocess.run(["git", "init", "-q"], cwd=root, check=True)
            subprocess.run(["git", "config", "user.email", "test@example.invalid"], cwd=root, check=True)
            subprocess.run(["git", "config", "user.name", "Release Test"], cwd=root, check=True)
            gradle = root / "build.gradle.kts"
            gradle.write_text(gradle_source(name="1.0.10", code=41), encoding="utf-8")
            tv_gradle = root / "tv/build.gradle.kts"
            tv_gradle.parent.mkdir()
            tv_gradle.write_text(tv_gradle_source(name="1.8", code=26), encoding="utf-8")
            subprocess.run(["git", "add", "build.gradle.kts", "tv/build.gradle.kts"], cwd=root, check=True)
            subprocess.run(["git", "commit", "-qm", "tag fixture"], cwd=root, check=True)
            subprocess.run(["git", "tag", "v1.0.10"], cwd=root, check=True)
            self.assertEqual(41, previous_tag_version_code("v1.0.11", root, "build.gradle.kts"))
            self.assertEqual(26, previous_tag_tv_version_code("v1.0.11", root, "tv/build.gradle.kts"))


if __name__ == "__main__":
    unittest.main()

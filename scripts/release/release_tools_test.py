import json
import tempfile
import unittest
from contextlib import redirect_stdout
from io import StringIO
from pathlib import Path

from scripts.release.release_common import (
    build_manifest,
    read_gradle_properties,
    update_forge_update_json,
    version_type,
)


class ReleaseToolsTest(unittest.TestCase):
    def test_read_gradle_properties_ignores_comments_and_blank_lines(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            props = Path(temp_dir) / "gradle.properties"
            props.write_text(
                "\n# comment\nmod_version=2.1.2\nminecraft_version=1.20.1\n",
                encoding="utf-8",
            )

            values = read_gradle_properties(props)

        self.assertEqual(values["mod_version"], "2.1.2")
        self.assertEqual(values["minecraft_version"], "1.20.1")

    def test_version_type_detects_release_channels(self):
        self.assertEqual(version_type("2.1.2"), "release")
        self.assertEqual(version_type("2.1.2-alpha"), "alpha")
        self.assertEqual(version_type("2.1.2-beta"), "beta")

    def test_build_manifest_rejects_alpha_and_beta_when_stable_required(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            artifact = Path(temp_dir) / "dragonminez-2.1.2-alpha.jar"
            artifact.write_bytes(b"jar-bytes")

            with self.assertRaisesRegex(ValueError, "stable releases"):
                build_manifest(
                    gradle_properties={
                        "mod_version": "2.1.2-alpha",
                        "minecraft_version": "1.20.1",
                        "forge_version": "47.4.10",
                    },
                    artifact_path=artifact,
                    commit_sha="abc123",
                    repository="DragonMineZ/dragonminez",
                    run_id="42",
                    server_url="https://github.com",
                    changelog="Release notes",
                    require_stable=True,
                )

    def test_build_manifest_includes_checksum_and_bot_payload_fields(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            artifact = Path(temp_dir) / "dragonminez-2.1.2.jar"
            artifact.write_bytes(b"jar-bytes")

            manifest = build_manifest(
                gradle_properties={
                    "mod_version": "2.1.2",
                    "minecraft_version": "1.20.1",
                    "forge_version": "47.4.10",
                },
                artifact_path=artifact,
                commit_sha="abc123",
                repository="DragonMineZ/dragonminez",
                run_id="42",
                server_url="https://github.com",
                changelog="Release notes",
                require_stable=True,
            )

        self.assertEqual(manifest["version"], "2.1.2")
        self.assertEqual(manifest["release_type"], "release")
        self.assertEqual(manifest["minecraft_version"], "1.20.1")
        self.assertEqual(manifest["forge_version"], "47.4.10")
        self.assertEqual(manifest["artifact"]["name"], "dragonminez-2.1.2.jar")
        self.assertEqual(
            manifest["artifact"]["sha256"],
            "829b21a069ff177599d32249ba84e0979b39f7fcba8a437607be0b9b06b51c20",
        )
        self.assertEqual(manifest["targets"], ["modrinth", "curseforge"])
        self.assertEqual(
            manifest["workflow_run_url"],
            "https://github.com/DragonMineZ/dragonminez/actions/runs/42",
        )

    def test_update_forge_update_json_sets_latest_recommended_and_version_text(self):
        current = {
            "homepage": "https://www.curseforge.com/minecraft/mc-mods/dragonminez",
            "promos": {"1.20.1-latest": "2.1.0", "1.20.1-recommended": "2.1.0"},
            "1.20.1": {"2.1.0": "end of z intro"},
        }

        updated = update_forge_update_json(
            current,
            minecraft_version="1.20.1",
            version="2.1.2",
            description="Release notes",
        )

        self.assertEqual(updated["promos"]["1.20.1-latest"], "2.1.2")
        self.assertEqual(updated["promos"]["1.20.1-recommended"], "2.1.2")
        self.assertEqual(updated["1.20.1"]["2.1.2"], "Release notes")
        self.assertEqual(updated["1.20.1"]["2.1.0"], "end of z intro")

    def test_update_json_output_remains_pretty_printed(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            update_file = Path(temp_dir) / "update.json"
            update_file.write_text(
                json.dumps({"homepage": "https://example.com", "promos": {}}, indent=2),
                encoding="utf-8",
            )

            from scripts.release.update_forge_update_json import main

            exit_code = main(
                [
                    "--file",
                    str(update_file),
                    "--minecraft-version",
                    "1.20.1",
                    "--version",
                    "2.1.2",
                    "--description",
                    "Release notes",
                ]
            )

            text = update_file.read_text(encoding="utf-8")

        self.assertEqual(exit_code, 0)
        self.assertIn('  "promos": {', text)
        self.assertTrue(text.endswith("\n"))

    def test_prepare_release_writes_manifest_and_bot_payload_files(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            props = root / "gradle.properties"
            artifact = root / "build" / "libs" / "dragonminez-2.1.2.jar"
            manifest_path = root / "release-manifest.json"
            bot_payload_path = root / "release-bot-payload.json"
            artifact.parent.mkdir(parents=True)
            props.write_text(
                "mod_version=2.1.2\nminecraft_version=1.20.1\nforge_version=47.4.10\n",
                encoding="utf-8",
            )
            artifact.write_bytes(b"jar-bytes")

            from scripts.release.prepare_release import main

            with redirect_stdout(StringIO()):
                exit_code = main(
                    [
                        "--gradle-properties",
                        str(props),
                        "--artifact",
                        str(artifact),
                        "--manifest",
                        str(manifest_path),
                        "--bot-payload",
                        str(bot_payload_path),
                        "--require-stable",
                    ]
                )

            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
            bot_payload = json.loads(bot_payload_path.read_text(encoding="utf-8"))

        self.assertEqual(exit_code, 0)
        self.assertEqual(manifest["version"], "2.1.2")
        self.assertEqual(bot_payload["event_type"], "dragonminez_release_candidate")
        self.assertEqual(bot_payload["client_payload"]["version"], "2.1.2")


if __name__ == "__main__":
    unittest.main()

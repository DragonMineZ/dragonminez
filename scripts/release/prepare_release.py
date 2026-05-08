import argparse
import json
import os
from pathlib import Path

from scripts.release.release_common import build_manifest, read_gradle_properties, write_json


def read_changelog(path):
    if not path:
        return ""
    changelog_path = Path(path)
    if not changelog_path.is_file():
        raise FileNotFoundError(f"Changelog file does not exist: {changelog_path}")
    return changelog_path.read_text(encoding="utf-8").strip()


def main(argv=None):
    parser = argparse.ArgumentParser(description="Build the DragonMineZ release manifest.")
    parser.add_argument("--gradle-properties", default="gradle.properties")
    parser.add_argument("--artifact", required=True)
    parser.add_argument("--manifest", default="release-manifest.json")
    parser.add_argument("--bot-payload", default="release-bot-payload.json")
    parser.add_argument("--changelog-file")
    parser.add_argument("--require-stable", action="store_true")
    parser.add_argument("--commit-sha", default=os.environ.get("GITHUB_SHA", "local"))
    parser.add_argument("--repository", default=os.environ.get("GITHUB_REPOSITORY", "DragonMineZ/dragonminez"))
    parser.add_argument("--run-id", default=os.environ.get("GITHUB_RUN_ID", "local"))
    parser.add_argument("--server-url", default=os.environ.get("GITHUB_SERVER_URL", "https://github.com"))
    args = parser.parse_args(argv)

    manifest = build_manifest(
        gradle_properties=read_gradle_properties(args.gradle_properties),
        artifact_path=args.artifact,
        commit_sha=args.commit_sha,
        repository=args.repository,
        run_id=args.run_id,
        server_url=args.server_url,
        changelog=read_changelog(args.changelog_file),
        require_stable=args.require_stable,
    )
    write_json(args.manifest, manifest)
    write_json(args.bot_payload, manifest["bot_payload"])
    print(json.dumps(manifest["bot_payload"], indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

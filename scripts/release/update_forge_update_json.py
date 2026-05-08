import argparse
import json
from pathlib import Path

from scripts.release.release_common import update_forge_update_json, write_json


def main(argv=None):
    parser = argparse.ArgumentParser(description="Update Forge update.json for a DragonMineZ release.")
    parser.add_argument("--file", default="update.json")
    parser.add_argument("--minecraft-version", required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--description", required=True)
    args = parser.parse_args(argv)

    update_path = Path(args.file)
    data = json.loads(update_path.read_text(encoding="utf-8"))
    updated = update_forge_update_json(
        data,
        minecraft_version=args.minecraft_version,
        version=args.version,
        description=args.description,
    )
    write_json(update_path, updated)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

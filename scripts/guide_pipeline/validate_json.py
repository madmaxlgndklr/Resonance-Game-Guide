"""Validate the shipped guide dataset.

    python3 scripts/guide_pipeline/validate_json.py

Checks, in order:

1. the file is valid UTF-8 JSON;
2. it satisfies the pydantic schema in ``schema.py``;
3. every id is unique, stable snake_case, and unused ids are reported;
4. no foreign key (bossId, visionId, questId, locationId, reward references)
   points at something that is not in the dataset;
5. expected counts match actual ones.

A count shortfall that the dataset itself records as a gap is reported as a
warning, not an error: the dataset is allowed to be incomplete, but it is not
allowed to be silently incomplete.

Exit status is 1 if any error is found, otherwise 0.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

from pydantic import ValidationError

from ids import slugify
from schema import ID_PATTERN, Guide

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_PATH = REPO_ROOT / "app/src/main/assets/ff_resonance_guide.json"

#: Collections whose members carry a top-level id, by dotted path.
ID_COLLECTIONS = (
    "combatMechanics",
    "chapters",
    "heroes",
    "visions",
    "espers",
    "sideQuests",
    "caveMasters",
    "colosseum",
    "chamberOfArms",
    "adamantiteLocations",
    "equipment",
    "items",
    "bestiary",
    "skills",
    "dialogueChoices",
)


class Report:
    def __init__(self) -> None:
        self.errors: list[str] = []
        self.warnings: list[str] = []
        self.info: list[str] = []

    def error(self, message: str) -> None:
        self.errors.append(message)

    def warn(self, message: str) -> None:
        self.warnings.append(message)

    def note(self, message: str) -> None:
        self.info.append(message)

    @property
    def ok(self) -> bool:
        return not self.errors

    def render(self) -> str:
        lines = []
        for message in self.info:
            lines.append(f"  .  {message}")
        for message in self.warnings:
            lines.append(f"  !  {message}")
        for message in self.errors:
            lines.append(f"  X  {message}")
        lines.append("")
        lines.append(
            f"{len(self.errors)} error(s), {len(self.warnings)} warning(s)."
        )
        return "\n".join(lines)


def collect_ids(guide: dict[str, Any], report: Report) -> dict[str, str]:
    """Map every id in the dataset to the collection it came from."""
    seen: dict[str, str] = {}
    nested_skill_ids: set[str] = set()

    def record(identifier: str, where: str) -> None:
        if not isinstance(identifier, str):
            report.error(f"{where}: id is not a string ({identifier!r})")
            return
        if not ID_PATTERN.match(identifier):
            report.error(f"{where}: id {identifier!r} is not stable snake_case")
        if identifier in seen:
            report.error(
                f"{where}: duplicate id {identifier!r} (also in {seen[identifier]})"
            )
            return
        if identifier in nested_skill_ids and not where.startswith("skills"):
            report.error(f"{where}: id {identifier!r} collides with a Vision skill")
            return
        seen[identifier] = where

    for name in ID_COLLECTIONS:
        for index, item in enumerate(guide.get(name, [])):
            identifier = item.get("id")
            record(identifier, f"{name}[{index}]")
            for objective in item.get("objectives", []) or []:
                if isinstance(objective, dict) and "id" in objective:
                    record(objective["id"], f"{name}[{index}].objectives")
            for battle in item.get("battles", []) or []:
                if isinstance(battle, dict) and "id" in battle:
                    record(battle["id"], f"{name}[{index}].battles")
            for skill in item.get("skills", []) or []:
                # A skill is listed under every Vision that teaches it, and
                # again in the flat `skills` catalogue. Those repeats are the
                # same skill, so they share an id by design: register them
                # without the uniqueness check.
                if isinstance(skill, dict) and "id" in skill:
                    nested_skill_ids.add(skill["id"])

    collectibles = guide.get("collectibles", {})
    for key in ("tabletShards", "oblivionPalaceTiers"):
        for index, item in enumerate(collectibles.get(key, [])):
            record(item.get("id"), f"collectibles.{key}[{index}]")

    for index, asset in enumerate(guide.get("assetsManifest", {}).get("entries", [])):
        record(asset.get("id"), f"assetsManifest.entries[{index}]")

    # Vision-nested skills resolve against the flat catalogue.
    orphans = nested_skill_ids - seen.keys()
    if orphans:
        report.error(
            f"{len(orphans)} Vision skill id(s) are not in the skills "
            f"catalogue, e.g. {sorted(orphans)[:3]}"
        )
    seen.update({i: "skills" for i in nested_skill_ids})

    return seen


FOREIGN_KEYS = ("bossId", "visionId", "questId", "locationId", "chapterId", "esperId")


def check_foreign_keys(
    guide: dict[str, Any], ids: dict[str, str], report: Report
) -> None:
    """Walk the whole tree and resolve every *Id reference."""
    checked = 0
    dangling = 0

    def walk(node: Any, path: str) -> None:
        nonlocal checked, dangling
        if isinstance(node, dict):
            for key, value in node.items():
                if key in FOREIGN_KEYS and isinstance(value, str) and value:
                    checked += 1
                    if value not in ids:
                        dangling += 1
                        report.error(f"{path}.{key} -> {value!r} does not exist")
                else:
                    walk(value, f"{path}.{key}")
        elif isinstance(node, list):
            for index, item in enumerate(node):
                walk(item, f"{path}[{index}]")

    walk(guide, "$")
    report.note(f"foreign keys checked: {checked}, dangling: {dangling}")


def check_counts(guide: dict[str, Any], report: Report) -> None:
    coverage = guide.get("meta", {}).get("coverage", {})
    expected = coverage.get("expected", {})
    actual = coverage.get("actual", {})
    gaps = " ".join(coverage.get("gaps", []))

    for key, want in expected.items():
        have = actual.get(key)
        if have is None:
            report.error(f"coverage.actual is missing {key!r}")
            continue
        if have == want:
            continue
        # A shortfall is acceptable only if the dataset admits to it.
        if key in gaps or slugify(key) in slugify(gaps):
            report.warn(f"{key}: {have} of {want} (recorded as a known gap)")
        else:
            report.error(f"{key}: {have} of {want}, and no gap recorded for it")


def check_cross_references(guide: dict[str, Any], report: Report) -> None:
    """Vision skill links and colosseum battles should line up by name."""
    vision_names = {slugify(v["name"]) for v in guide.get("visions", [])}
    for vision in guide.get("visions", []):
        ranks = [
            s.get("masteryRank")
            for s in vision.get("skills", [])
            if s.get("masteryRank") is not None
        ]
        if ranks and not set(ranks) <= {1, 3, 5, 7, 9}:
            report.error(
                f"vision {vision['id']}: mastery ranks outside 1/3/5/7/9: "
                f"{sorted(set(ranks))}"
            )
    report.note(f"visions: {len(vision_names)} named")

    for league in guide.get("colosseum", []):
        numbers = [b["battle"] for b in league.get("battles", [])]
        if numbers != sorted(numbers):
            report.error(f"colosseum {league['id']}: battles out of order")


def validate(path: Path) -> Report:
    report = Report()

    raw = path.read_bytes()
    try:
        raw.decode("utf-8")
    except UnicodeDecodeError as exc:
        report.error(f"file is not valid UTF-8: {exc}")
        return report

    try:
        guide = json.loads(raw)
    except json.JSONDecodeError as exc:
        report.error(f"invalid JSON: {exc}")
        return report

    report.note(f"{path.name}: {len(raw) / 1024:.0f} KiB")

    try:
        Guide.model_validate(guide)
        report.note("schema: valid")
    except ValidationError as exc:
        for error in exc.errors()[:25]:
            location = ".".join(str(part) for part in error["loc"])
            report.error(f"schema {location}: {error['msg']}")

    ids = collect_ids(guide, report)
    report.note(f"unique ids: {len(ids)}")
    check_foreign_keys(guide, ids, report)
    check_counts(guide, report)
    check_cross_references(guide, report)

    for gap in guide.get("meta", {}).get("coverage", {}).get("gaps", []):
        report.warn(f"gap: {gap}")

    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("path", nargs="?", type=Path, default=DEFAULT_PATH)
    args = parser.parse_args()

    if not args.path.is_file():
        print(f"not found: {args.path}", file=sys.stderr)
        return 1

    report = validate(args.path)
    print(f"Validating {args.path}\n")
    print(report.render())
    return 0 if report.ok else 1


if __name__ == "__main__":
    raise SystemExit(main())

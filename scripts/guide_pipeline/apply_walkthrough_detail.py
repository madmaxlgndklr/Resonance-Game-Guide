"""Merge ``walkthrough_detail.json`` into an already-built dataset.

    python3 scripts/guide_pipeline/apply_walkthrough_detail.py

``build_guide.py`` is the real pipeline, but a full run re-crawls roughly
1,100 item and enemy detail pages to rebuild tables this change does not
touch. This script applies exactly the parts of that run which depend on
``walkthrough_detail.json`` -- the chapters' ``walkthroughAreas``, the
``dialogueChoices`` table, the coverage counts and gaps that go with them, and
the source registry snapshot -- onto the dataset that is already shipped.

It calls the same ``build_guide`` helpers the pipeline does, so an area id
produced here is the id a full rebuild would produce. Everything else in the
file is left untouched.

Pass ``--check`` to verify the shipped asset already matches, which is what
CI runs; it writes nothing and exits non-zero on a difference.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

from build_guide import (
    DATASET_VERSION,
    EXPECTED,
    OUTPUT,
    build_walkthrough_areas,
    load_walkthrough_detail,
)
from sources import SOURCES, Status

#: Gaps this script owns. A gap whose text starts with one of these is dropped
#: before the current set is appended, so re-running does not stack duplicates
#: and a gap that has been closed does not linger.
OWNED_GAP_PREFIXES = (
    "dialogueChoices:",
    "Active Time Chats:",
    "chapters.walkthroughAreas:",
)


def merged(guide: dict[str, Any], detail: dict[str, Any]) -> dict[str, Any]:
    guide = json.loads(json.dumps(guide))  # don't mutate the caller's copy
    by_chapter = detail.get("chapters", {})
    source_url = detail.get("sourceUrl")

    for chapter in guide["chapters"]:
        chapter["walkthroughAreas"] = build_walkthrough_areas(
            chapter["id"], by_chapter.get(str(chapter["chapterNumber"]), []), source_url
        )

    dialogue_choices = detail.get("dialogueChoices", [])
    guide["dialogueChoices"] = dialogue_choices

    # GuideSeeder reseeds only when datasetVersion moves, so content that
    # lands without a bump is written to the asset and never read into Room.
    # The merge therefore owns this field, and --check catches a missed bump.
    guide["meta"]["datasetVersion"] = DATASET_VERSION
    if detail.get("lastUpdated"):
        guide["meta"]["lastUpdated"] = detail["lastUpdated"]

    coverage = guide["meta"]["coverage"]
    coverage["expected"] = dict(EXPECTED)
    coverage["actual"]["walkthroughAreas"] = sum(
        len(c["walkthroughAreas"]) for c in guide["chapters"]
    )
    coverage["actual"]["dialogueChoices"] = len(dialogue_choices)

    gaps = [
        gap
        for gap in coverage.get("gaps", [])
        if not gap.startswith(OWNED_GAP_PREFIXES)
    ]
    if not dialogue_choices:
        gaps.append(
            "dialogueChoices: empty. No source we are permitted to crawl "
            "publishes the stat-bonus conversation options."
        )
    gaps.append(
        "Active Time Chats: no complete list. The source walkthrough states "
        "its own coverage is incomplete and that some chats have limited "
        "availability. Those we have are named in walkthroughAreas[].steps "
        "rather than shipped as a checklist that cannot be finished."
    )
    missing = [
        c["chapterNumber"] for c in guide["chapters"] if not c["walkthroughAreas"]
    ]
    if missing:
        gaps.append(
            f"chapters.walkthroughAreas: no area notes for chapter(s) {missing}."
        )
    coverage["gaps"] = gaps

    # sources.py is the record of what each host permits, and rpgsite's
    # robots.txt has changed since the dataset was last built. Re-snapshot it.
    guide["meta"]["sources"] = [
        {
            "key": s.key,
            "host": s.host,
            "status": s.status.value,
            "covers": s.covers,
            "note": s.note,
            "used": s.status is Status.CRAWL_OK,
        }
        for s in SOURCES
    ]
    return guide


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("path", nargs="?", type=Path, default=OUTPUT)
    parser.add_argument(
        "--check",
        action="store_true",
        help="verify the file already matches; write nothing",
    )
    args = parser.parse_args()

    if not args.path.is_file():
        print(f"not found: {args.path}", file=sys.stderr)
        return 1

    before = json.loads(args.path.read_text(encoding="utf-8"))
    after = merged(before, load_walkthrough_detail())

    if before == after:
        print(f"{args.path.name}: already up to date")
        return 0

    if args.check:
        print(
            f"::error::{args.path} does not match walkthrough_detail.json. "
            "Run python3 scripts/guide_pipeline/apply_walkthrough_detail.py "
            "and commit the result.",
            file=sys.stderr,
        )
        return 1

    args.path.write_text(
        json.dumps(after, indent=1, ensure_ascii=False) + "\n", encoding="utf-8"
    )
    areas = after["meta"]["coverage"]["actual"]["walkthroughAreas"]
    print(
        f"{args.path.name}: {areas} walkthrough areas, "
        f"{len(after['dialogueChoices'])} dialogue choices"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

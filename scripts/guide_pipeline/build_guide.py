"""Build the shipped guide dataset from the verified seed plus permitted crawls.

    python3 build_guide.py --seed path/to/ff_resonance_android_guide_data.json

The seed file is the authoritative baseline: hand-verified chapter, hero,
vision, esper, quest, cave-master, colosseum and chamber records. On top of
that we layer the sub-tables we are allowed to crawl (see sources.py).

Nothing is invented. Where a table has no permitted source, it is written
empty and the gap is recorded in ``meta.coverage.gaps`` so the app -- and
anyone reading the JSON -- can see exactly what is missing and why.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any

import parse_atma as P
from fetch import Fetcher, NotPermitted
from ids import PREFIX, make_id, numbered_id, slugify
from schema import Guide
from sources import SOURCES, Status

REPO_ROOT = Path(__file__).resolve().parents[2]
OUTPUT = REPO_ROOT / "app/src/main/assets/ff_resonance_guide.json"
DATASET_VERSION = "0.2.0"

#: Totals the seed's own meta.worldTotals asserts, used for the coverage check.
EXPECTED = {
    "chapters": 9,
    "heroes": 8,
    "visions": 26,
    "espers": 9,
    "sideQuests": 33,
    "caveMasters": 15,
    "colosseumBattles": 20,
    "chamberOfArms": 12,
    "adamantiteLocations": 8,
    "tabletShards": 99,
}


def clean_cell(text: str) -> str:
    """Collapse atma's tooltip duplication in a table cell.

    Item cells render as "Potion Potion Restores 500 HP. x 1" because the
    name is repeated inside a tooltip. Drop the immediate repeat.
    """
    text = re.sub(r"\s+", " ", text or "").strip()
    match = re.match(r"^(.*?)\s+\1\b\s*(.*)$", text)
    if match:
        name, rest = match.groups()
        return f"{name} {rest}".strip()
    return text


def split_item_cell(text: str) -> dict[str, Any]:
    """Split "Name Name Description x 2" into its parts."""
    cleaned = clean_cell(text)
    quantity = None
    quantity_match = re.search(r"[x×]\s*(\d+)\s*$", cleaned)
    if quantity_match:
        quantity = int(quantity_match.group(1))
        cleaned = cleaned[: quantity_match.start()].strip()
    name_match = re.match(r"^(.*?)\s+\1\s*(.*)$", cleaned)
    if name_match:
        name, description = name_match.groups()
    else:
        parts = cleaned.split(". ", 1)
        name, description = parts[0], (parts[1] if len(parts) > 1 else "")
    return {
        "name": name.strip(),
        "description": description.strip() or None,
        "quantity": quantity,
    }


def strip_repeats(text: str) -> str:
    """Turn "Wyvern Wyvern Magma Tortoise" into "Wyvern, Magma Tortoise"."""
    cleaned = clean_cell(text)
    seen: list[str] = []
    for word in cleaned.split():
        if not seen or seen[-1] != word:
            seen.append(word)
    return " ".join(seen)


# -- seed -> schema ----------------------------------------------------------


def build_chapters(seed: list[dict[str, Any]]) -> list[dict[str, Any]]:
    chapters = []
    for raw in seed:
        number = raw["chapterNumber"]
        chapter_id = make_id(PREFIX["chapter"], f"{number:02d}", raw["title"])
        objectives = [
            {
                "id": f"{chapter_id}_{PREFIX['objective']}_{step['step']:02d}",
                "step": step["step"],
                "text": step["text"],
                "sourceUrl": raw.get("sourceUrl"),
            }
            for step in raw.get("objectives", [])
        ]
        chapters.append(
            {
                "id": chapter_id,
                "chapterNumber": number,
                "title": raw["title"],
                "region": raw.get("region"),
                "recommendedLevel": raw.get("recommendedLevel"),
                "firstObjective": raw.get("firstObjective"),
                "pointOfNoReturn": raw.get("pointOfNoReturn"),
                "storyBosses": raw.get("storyBosses", []),
                "optionalFights": raw.get("optionalFights", []),
                "notableCollectibles": raw.get("notableCollectibles", []),
                "objectives": objectives,
                "sourceUrl": raw.get("sourceUrl"),
            }
        )
    return chapters


def passthrough(raw: dict[str, Any], prefix: str, *name_parts: str) -> dict[str, Any]:
    """Copy a seed record, re-deriving a deterministic id from its name."""
    record = dict(raw)
    name = next((record[p] for p in name_parts if record.get(p)), None)
    record["id"] = make_id(prefix, name) if name else record["id"]
    return record


# -- atma crawl --------------------------------------------------------------


def crawl_equipment(fetcher: Fetcher, notes: list[str]) -> list[dict[str, Any]]:
    equipment: list[dict[str, Any]] = []
    for slot, page in (
        ("weapon", "weapons"),
        ("armor", "armor"),
        ("accessory", "accessories"),
    ):
        try:
            html = fetcher.get(f"{P.BASE}/{page}")
        except NotPermitted as exc:
            notes.append(f"equipment/{slot}: {exc}")
            continue
        for record in P.parse_catalog(html, slot):
            stats = record.stats
            equipment.append(
                {
                    "id": make_id(PREFIX[slot], record.slug),
                    "name": record.name,
                    "slot": slot,
                    "equipType": record.attributes.get("type"),
                    "catalogNumber": record.catalogNumber,
                    "physAtk": stats.get("physAtk"),
                    "magAtk": stats.get("magAtk"),
                    "def": stats.get("def"),
                    "magDef": stats.get("magDef"),
                    "speed": stats.get("speed"),
                    "sanctity": stats.get("sanctity"),
                    "effect": record.effect,
                    # Acquisition and steal tables live only on sources that
                    # forbid crawling; left null rather than guessed.
                    "acquisitionMethod": None,
                    "stealSource": None,
                    "sourceUrl": record.url,
                }
            )
    return equipment


def crawl_items(fetcher: Fetcher, notes: list[str]) -> list[dict[str, Any]]:
    try:
        html = fetcher.get(f"{P.BASE}/items")
    except NotPermitted as exc:
        notes.append(f"items: {exc}")
        return []
    return [
        {
            "id": make_id(PREFIX["item"], record.slug),
            "name": record.name,
            "category": record.attributes.get("type"),
            "description": record.effect,
            "acquisitionMethod": None,
            "sourceUrl": record.url,
        }
        for record in P.parse_catalog(html, "item")
    ]


_LEVEL_SUFFIX = re.compile(r"\s*Lv\.\s*(\d+)\s*$")


def crawl_bestiary(fetcher: Fetcher, notes: list[str]) -> list[dict[str, Any]]:
    try:
        html = fetcher.get(f"{P.BASE}/enemies")
    except NotPermitted as exc:
        notes.append(f"bestiary: {exc}")
        return []
    enemies = []
    for record in P.parse_catalog(html, "enemy"):
        name = record.name
        level = None
        level_match = _LEVEL_SUFFIX.search(name)
        if level_match:
            level = int(level_match.group(1))
            name = _LEVEL_SUFFIX.sub("", name).strip()
        enemies.append(
            {
                "id": make_id(PREFIX["enemy"], record.slug, name),
                "name": name,
                "enemyType": record.attributes.get("type"),
                "level": level,
                # Per-enemy HP, drops and steals are on the detail pages;
                # run with --details to fill them in.
                "hp": None,
                "staggerGauge": None,
                "sourceUrl": record.url,
            }
        )
    return enemies


def crawl_skills(fetcher: Fetcher, notes: list[str]) -> list[dict[str, Any]]:
    try:
        html = fetcher.get(f"{P.BASE}/skills")
    except NotPermitted as exc:
        notes.append(f"skills: {exc}")
        return []
    skills = []
    for record in P.parse_catalog(html, "skill"):
        primary = record.sources[0] if record.sources else {}
        skills.append(
            {
                "id": make_id(PREFIX["skill"], record.slug, record.name),
                "name": record.name,
                "category": record.attributes.get("category"),
                "skillType": record.attributes.get("type"),
                "description": record.effect,
                "masteryRank": primary.get("masteryRank"),
                "apCost": primary.get("apCost"),
                "awakeningTier": primary.get("awakening"),
                "sourceUrl": record.url,
            }
        )
    return skills


def attach_vision_skills(
    visions: list[dict[str, Any]], fetcher: Fetcher, notes: list[str]
) -> None:
    """Hang each Vision's learnable skills off the Vision record."""
    try:
        html = fetcher.get(f"{P.BASE}/skills")
    except NotPermitted as exc:
        notes.append(f"vision skills: {exc}")
        return
    by_vision: dict[str, list[dict[str, Any]]] = {}
    for record in P.parse_catalog(html, "skill"):
        for source in record.sources:
            entry = {
                "id": make_id(PREFIX["skill"], record.slug, record.name),
                "name": record.name,
                "category": record.attributes.get("category"),
                "skillType": record.attributes.get("type"),
                "description": record.effect,
                "masteryRank": source.get("masteryRank"),
                "apCost": source.get("apCost"),
                "awakeningTier": source.get("awakening"),
                "sourceUrl": record.url,
            }
            by_vision.setdefault(slugify(source["vision"]), []).append(entry)

    for vision in visions:
        matched = by_vision.get(slugify(vision["name"]), [])
        matched.sort(
            key=lambda s: (
                s["masteryRank"] if s["masteryRank"] is not None else 99,
                s["awakeningTier"] if s["awakeningTier"] is not None else 99,
                s["name"],
            )
        )
        # One skill can appear at several awakening tiers; keep each id once.
        unique: dict[str, dict[str, Any]] = {}
        for skill in matched:
            unique.setdefault(skill["id"], skill)
        vision["skills"] = list(unique.values())


def crawl_oblivion_tiers(fetcher: Fetcher, notes: list[str]) -> list[dict[str, Any]]:
    try:
        html = fetcher.get(f"{P.BASE}/tablet-shards")
    except NotPermitted as exc:
        notes.append(f"oblivion palace tiers: {exc}")
        return []
    tiers = []
    for table in P.parse_tables(html):
        chamber = table["caption"]
        if not chamber or "Chamber" not in chamber:
            continue
        for row in table["rows"]:
            total = row.get("Total Shards", "").strip()
            if not total.isdigit():
                continue
            reward = split_item_cell(row.get("Reward", ""))
            tiers.append(
                {
                    "id": make_id(PREFIX["shard"], "tier", chamber, total),
                    "chamber": chamber,
                    "totalShards": int(total),
                    "rewardName": reward["name"],
                    "rewardDescription": reward["description"],
                    "quantity": reward["quantity"],
                    "sourceUrl": f"{P.BASE}/tablet-shards",
                }
            )
    return tiers


def enrich_side_quests(
    quests: list[dict[str, Any]], fetcher: Fetcher, notes: list[str]
) -> None:
    """Add the region column from atma's quest table to the seed quests."""
    try:
        html = fetcher.get(f"{P.BASE}/quests")
    except NotPermitted as exc:
        notes.append(f"side quest regions: {exc}")
        return
    by_name = {}
    for table in P.parse_tables(html):
        for row in table["rows"]:
            name = clean_cell(row.get("Name", ""))
            if name:
                by_name[slugify(name)] = row
    for quest in quests:
        row = by_name.get(slugify(quest["title"]))
        if not row:
            continue
        quest["region"] = row.get("Region") or None
        if not quest.get("rewards"):
            reward = split_item_cell(row.get("Rewards", ""))
            if reward["name"]:
                quest["rewards"] = [reward["name"]]


def build_colosseum(
    seed: list[dict[str, Any]], fetcher: Fetcher, notes: list[str]
) -> list[dict[str, Any]]:
    """Seed leagues, with atma's per-battle names and levels layered on."""
    atma_rows: dict[str, list[dict[str, str]]] = {}
    try:
        html = fetcher.get(f"{P.BASE}/colosseum")
        for table in P.parse_tables(html):
            if table["caption"].endswith("League"):
                atma_rows[slugify(table["caption"])] = table["rows"]
    except NotPermitted as exc:
        notes.append(f"colosseum detail: {exc}")

    leagues = []
    for raw in seed:
        league_id = make_id(PREFIX["colosseum"], raw["name"])
        rows = atma_rows.get(slugify(raw["name"]), [])
        battles = []
        for index, battle in enumerate(raw.get("battles", [])):
            row = rows[index] if index < len(rows) else {}
            battles.append(
                {
                    "id": f"{league_id}_b{battle['battle']:02d}",
                    "battle": battle["battle"],
                    "recLevel": battle.get("recLevel"),
                    "name": clean_cell(row.get("Name", "")) or None,
                    "enemies": battle.get("enemies")
                    or strip_repeats(row.get("Main Enemy", ""))
                    or None,
                    "firstWinReward": battle.get("firstWinReward")
                    or split_item_cell(row.get("First-time Reward", ""))["name"]
                    or None,
                    "repeatReward": raw.get("repeatReward"),
                }
            )
        leagues.append(
            {
                "id": league_id,
                "name": raw["name"],
                "recommendedLevels": raw.get("recommendedLevels"),
                "repeatReward": raw.get("repeatReward"),
                "clearCertificate": raw.get("clearCertificate"),
                "battles": battles,
                "sourceUrl": raw.get("sourceUrl"),
            }
        )
    return leagues


# -- assembly ----------------------------------------------------------------


def enrich_from_details(
    records: list[dict[str, Any]],
    parser,
    fetcher: Fetcher,
    label: str,
    notes: list[str],
) -> int:
    """Fetch each record's own page and merge the extra facts in.

    Cached pages make this resumable: interrupt it and run again, and only
    the pages not yet fetched cost a request.
    """
    filled = 0
    failures = 0
    for index, record in enumerate(records, start=1):
        url = record.get("sourceUrl")
        if not url:
            continue
        try:
            detail = parser(fetcher.get(url))
        except NotPermitted as exc:
            notes.append(f"{label} details: {exc}")
            return filled
        except Exception:
            failures += 1
            if failures > 25:
                notes.append(
                    f"{label} details: stopped after {failures} fetch failures "
                    f"at record {index}; re-run to resume."
                )
                return filled
            continue
        merged = False
        for key, value in detail.items():
            if key == "name" or value in (None, [], {}):
                continue
            if record.get(key) in (None, [], {}):
                record[key] = value
                merged = True
        if merged:
            filled += 1
        if index % 100 == 0:
            print(f"  {label}: {index}/{len(records)}", flush=True)
    return filled


def build(seed_path: Path, offline: bool = False, details: bool = False) -> dict[str, Any]:
    seed = json.loads(seed_path.read_text(encoding="utf-8"))
    fetcher = Fetcher(offline=offline)
    gaps: list[str] = []

    visions = [passthrough(v, PREFIX["vision"], "name") for v in seed["visions"]]
    attach_vision_skills(visions, fetcher, gaps)

    side_quests = [
        passthrough(q, PREFIX["quest"], "title") for q in seed["sideQuests"]
    ]
    enrich_side_quests(side_quests, fetcher, gaps)

    equipment = crawl_equipment(fetcher, gaps)
    items = crawl_items(fetcher, gaps)
    bestiary = crawl_bestiary(fetcher, gaps)

    detail_counts: dict[str, int] = {}
    if details:
        detail_counts["equipment"] = enrich_from_details(
            equipment, P.parse_item_detail, fetcher, "equipment", gaps
        )
        detail_counts["items"] = enrich_from_details(
            items, P.parse_item_detail, fetcher, "items", gaps
        )
        detail_counts["bestiary"] = enrich_from_details(
            bestiary, P.parse_enemy_detail, fetcher, "bestiary", gaps
        )
    skills = crawl_skills(fetcher, gaps)
    oblivion = crawl_oblivion_tiers(fetcher, gaps)

    chapters = build_chapters(seed["walkthrough"])

    # Tablet shard locations: the only sources that publish all 99 forbid
    # automated access, so the table ships empty rather than invented.
    ign = next(s for s in SOURCES if s.key == "ign")
    gaps.append(
        "collectibles.tabletShards: 0 of 99 locations. The published "
        f"location lists are on {ign.host}, which forbids automated "
        "retrieval and dataset creation. Needs licensing, or manual entry."
    )
    if details:
        missing_acq = sum(1 for e in equipment if not e.get("acquisitionMethod"))
        if missing_acq:
            gaps.append(
                f"equipment.acquisitionMethod: {missing_acq} of {len(equipment)} "
                "rows have no published acquisition route."
            )
        missing_hp = sum(1 for e in bestiary if e.get("hp") is None)
        if missing_hp:
            gaps.append(
                f"bestiary.hp: {missing_hp} of {len(bestiary)} enemies have no "
                "published stat block."
            )
    else:
        gaps.append(
            "equipment.acquisitionMethod and bestiary stats/drops/steals are "
            "null: detail pages not fetched. Re-run with --details."
        )
    gaps.append(
        "dialogueChoices: empty. No permitted source publishes the "
        "stat-bonus conversation options."
    )

    actual = {
        "chapters": len(chapters),
        "heroes": len(seed["playableHeroes"]),
        "visions": len(visions),
        "espers": len(seed["espers"]),
        "sideQuests": len(side_quests),
        "caveMasters": len(seed["caveMasters"]),
        "colosseumBattles": sum(len(l.get("battles", [])) for l in seed["colosseumLeagues"]),
        "chamberOfArms": len(seed["chamberOfArms"]),
        "adamantiteLocations": len(seed["adamantiteLocations"]),
        "tabletShards": 0,
        "equipment": len(equipment),
        "items": len(items),
        "bestiary": len(bestiary),
        "skills": len(skills),
        "oblivionPalaceTiers": len(oblivion),
        "equipmentWithAcquisition": sum(
            1 for e in equipment if e.get("acquisitionMethod")
        ),
        "enemiesWithStats": sum(1 for e in bestiary if e.get("hp") is not None),
        "enemiesWithStealTable": sum(1 for e in bestiary if e.get("stealables")),
    }

    seed_meta = seed.get("meta", {})
    guide = {
        "meta": {
            "gameTitle": seed_meta.get("gameTitle", "FINAL FANTASY RESONANCE"),
            "schemaVersion": "2.0.0",
            "datasetVersion": DATASET_VERSION,
            "lastUpdated": seed_meta.get("lastUpdated", ""),
            "generatedBy": "scripts/guide_pipeline/build_guide.py",
            "releaseDate": seed_meta.get("releaseDate"),
            "publisher": seed_meta.get("publisher"),
            "platforms": seed_meta.get("platforms", []),
            "worldTotals": seed_meta.get("worldTotals", {}),
            "coverage": {"expected": EXPECTED, "actual": actual, "gaps": gaps},
            "sources": [
                {
                    "key": s.key,
                    "host": s.host,
                    "status": s.status.value,
                    "covers": s.covers,
                    "note": s.note,
                    "used": s.status is Status.CRAWL_OK,
                }
                for s in SOURCES
            ],
        },
        "combatMechanics": [
            {
                "id": m["id"],
                "name": m["name"],
                "summary": m["summary"],
                "sourceUrl": m.get("sourceUrl"),
            }
            for m in seed.get("combatMechanics", {}).get("coreSystems", [])
        ],
        "chapters": chapters,
        "heroes": [passthrough(h, PREFIX["hero"], "name") for h in seed["playableHeroes"]],
        "visions": visions,
        "espers": [passthrough(e, PREFIX["esper"], "name") for e in seed["espers"]],
        "sideQuests": side_quests,
        "caveMasters": [
            passthrough(c, PREFIX["cave_master"], "name") for c in seed["caveMasters"]
        ],
        "colosseum": build_colosseum(seed["colosseumLeagues"], fetcher, gaps),
        "chamberOfArms": [
            passthrough(c, PREFIX["chamber"], "guardian") for c in seed["chamberOfArms"]
        ],
        "adamantiteLocations": [
            passthrough(a, PREFIX["adamantite"], "source")
            for a in seed["adamantiteLocations"]
        ],
        "equipment": equipment,
        "items": items,
        "bestiary": bestiary,
        "skills": skills,
        "collectibles": {"tabletShards": [], "oblivionPalaceTiers": oblivion},
        "dialogueChoices": [],
        "assetsManifest": build_assets_manifest(seed),
    }
    return guide


def build_assets_manifest(seed: dict[str, Any]) -> dict[str, Any]:
    """Record where art lives and under what terms -- without downloading it.

    Square Enix key art, press-kit files and ripped game sprites are the
    publisher's copyright. Recording the URL and the licence position is
    useful; bundling the bytes into a distributed APK would be redistribution,
    so every entry ships with ``bundled: false``.
    """
    entries = []
    for raw in seed.get("assetsManifest", {}).get("keyArt", []):
        entries.append(
            {
                "id": raw.get("id") or make_id(PREFIX["asset"], raw["title"]),
                "title": raw["title"],
                "url": raw["url"],
                "sourceUrl": raw.get("sourceUrl"),
                "localPath": None,
                "thumbnailPath": None,
                "licenseStatus": (
                    "Square Enix copyright. Promotional and press material is "
                    "licensed for editorial coverage, not for redistribution "
                    "inside an application. Not bundled."
                ),
                "bundled": False,
            }
        )
    return {
        "pressKitUrl": seed.get("assetsManifest", {}).get("pressKitUrl"),
        "licenceNote": (
            "No third-party or publisher artwork is bundled. Wiki map "
            "recompositions are watermarked and excluded; official art is "
            "copyright Square Enix and recorded here by URL only."
        ),
        "entries": entries,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--seed", type=Path, required=True)
    parser.add_argument("--out", type=Path, default=OUTPUT)
    parser.add_argument("--offline", action="store_true", help="cache only")
    parser.add_argument(
        "--details",
        action="store_true",
        help="also fetch each item/enemy page for acquisition, drops and steals",
    )
    args = parser.parse_args()

    guide = build(args.seed, offline=args.offline, details=args.details)
    Guide.model_validate(guide)  # fail the build rather than ship bad data

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(
        json.dumps(guide, ensure_ascii=False, indent=1) + "\n", encoding="utf-8"
    )

    coverage = guide["meta"]["coverage"]
    print(f"wrote {args.out.relative_to(REPO_ROOT)} "
          f"({args.out.stat().st_size / 1024:.0f} KiB)")
    for key, value in coverage["actual"].items():
        expected = coverage["expected"].get(key)
        flag = "" if expected in (None, value) else f"  (expected {expected})"
        print(f"  {key:22s} {value:5d}{flag}")
    print(f"\n{len(coverage['gaps'])} recorded gaps:")
    for gap in coverage["gaps"]:
        print(f"  - {gap}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

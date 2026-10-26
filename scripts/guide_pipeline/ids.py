"""Deterministic, immutable snake_case identifiers.

Checklist state in Room is keyed by these strings, so an id must never change
for a given real-world thing once it has shipped. They are therefore derived
only from stable inputs: the thing's own name, and where available the game's
internal catalog number as exposed by the source. Never derive an id from a
list position, a crawl order, or anything else that can shift.
"""

from __future__ import annotations

import re
import unicodedata

_NON_ALNUM = re.compile(r"[^a-z0-9]+")


def slugify(value: str) -> str:
    """Lowercase, ASCII-folded, underscore-joined form of ``value``."""
    folded = unicodedata.normalize("NFKD", value)
    folded = folded.encode("ascii", "ignore").decode("ascii")
    # Typographic apostrophes drop out with the fold above; keep letters only.
    return _NON_ALNUM.sub("_", folded.lower()).strip("_")


def make_id(prefix: str, *parts: object) -> str:
    """Build ``prefix_part1_part2`` from ``parts``, skipping empty ones."""
    tail = "_".join(slugify(str(p)) for p in parts if p not in (None, ""))
    return f"{prefix}_{tail}" if tail else prefix


def numbered_id(prefix: str, number: int, width: int = 3) -> str:
    """Build a zero-padded id such as ``shard_001``."""
    return f"{prefix}_{number:0{width}d}"


#: Prefixes used across the dataset. Kept in one place so a new table cannot
#: silently reuse another's namespace.
PREFIX = {
    "chapter": "ch",
    "objective": "obj",
    "chest": "chest",
    "shard": "shard",
    "magicite": "mag",
    "vision": "vis",
    "esper": "esper",
    "hero": "hero",
    "quest": "sq",
    "cave_master": "cm",
    "colosseum": "col",
    "chamber": "coa",
    "adamantite": "adam",
    "weapon": "wpn",
    "armor": "arm",
    "accessory": "acc",
    "item": "item",
    "enemy": "enemy",
    "skill": "skill",
    "location": "loc",
    "boss": "boss",
    "dialogue": "dlg",
    "mechanic": "mech",
    "asset": "asset",
}

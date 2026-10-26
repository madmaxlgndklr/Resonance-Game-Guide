"""Parser for PowerPyx's tablet shard location guide.

The page lists each shard as ``N. Area - directions`` in its own paragraph,
grouped under a ``Chapter N`` heading.

We take the facts: the shard's number, the chapter it becomes reachable in,
and the area it sits in. We deliberately do **not** ship the guide's
directions: that wording is the author's own editorial writing. Our concise
descriptions live in ``shard_descriptions.json``, written in our own words,
and the record keeps a ``sourceUrl`` so a reader can go and read the original.
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Any

from bs4 import BeautifulSoup

URL = "https://www.powerpyx.com/final-fantasy-resonance-all-tablet-shard-locations/"

#: "12. Siren's Tower - In the next room, up the stairs..."
#: The separator is an en dash on this page, but accept a hyphen too.
_ENTRY = re.compile(
    r"^(?P<number>\d{1,2})\.\s*(?P<area>[^–—-]{2,60}?)\s*[–—-]\s*(?P<directions>.+)$"
)
_CHAPTER = re.compile(r"^Chapter\s+(\d+)$", re.I)


@dataclass
class ShardFact:
    number: int
    chapter: int | None
    area: str
    #: The source's own directions. Kept in memory for review only; never
    #: written into the shipped dataset.
    directions: str

    def as_dict(self) -> dict[str, Any]:
        return {
            "number": self.number,
            "chapter": self.chapter,
            "area": self.area,
            "directions": self.directions,
        }


def parse_shards(html: str) -> list[ShardFact]:
    soup = BeautifulSoup(html, "lxml")
    body = soup.select_one("article, .entry-content, main") or soup
    shards: list[ShardFact] = []
    chapter: int | None = None

    for node in body.find_all(["h1", "h2", "h3", "p", "li"]):
        text = re.sub(r"\s+", " ", node.get_text(" ", strip=True))
        if not text:
            continue
        if node.name in {"h1", "h2", "h3"}:
            match = _CHAPTER.match(text)
            chapter = int(match.group(1)) if match else chapter
            continue
        match = _ENTRY.match(text)
        if match is None:
            continue
        number = int(match.group("number"))
        if not 1 <= number <= 99:
            continue
        shards.append(
            ShardFact(
                number=number,
                chapter=chapter,
                area=match.group("area").strip(),
                directions=match.group("directions").strip(),
            )
        )

    # The page lists each shard once, in order; drop any accidental repeat
    # (comment sections can echo an entry).
    unique: dict[int, ShardFact] = {}
    for shard in shards:
        unique.setdefault(shard.number, shard)
    return [unique[key] for key in sorted(unique)]

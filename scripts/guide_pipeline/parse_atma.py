"""Parsers for atma.gg catalog and table pages.

The site renders its catalogs server-side as filterable cards: each card
carries ``data-search-text`` plus a link to the item's own page, and
equipment cards carry the game's internal catalog number in ``data-sort-id``
and their stats as JSON in ``data-sort-values``. That is what we read; the
site's prose is not copied.
"""

from __future__ import annotations

import json
import re
from dataclasses import dataclass, field
from typing import Any

from bs4 import BeautifulSoup, Tag

BASE = "https://atma.gg/final-fantasy-resonance"

#: Stat abbreviations as the cards label them, mapped to our field names.
STAT_FIELDS = {
    "ATK": "physAtk",
    "MATK": "magAtk",
    "DEF": "def",
    "MDEF": "magDef",
    "SPD": "speed",
    "SNC": "sanctity",
    "HP": "hp",
    "MP": "mp",
}


@dataclass
class CatalogRecord:
    slug: str
    name: str
    url: str
    kind: str
    catalogNumber: int | None = None
    attributes: dict[str, str] = field(default_factory=dict)
    stats: dict[str, Any] = field(default_factory=dict)
    effect: str | None = None
    sources: list[dict[str, Any]] = field(default_factory=list)

    def as_dict(self) -> dict[str, Any]:
        return {
            "slug": self.slug,
            "name": self.name,
            "sourceUrl": self.url,
            "kind": self.kind,
            "catalogNumber": self.catalogNumber,
            **({"attributes": self.attributes} if self.attributes else {}),
            **({"stats": self.stats} if self.stats else {}),
            **({"effect": self.effect} if self.effect else {}),
            **({"sources": self.sources} if self.sources else {}),
        }


def _text(node: Tag | None) -> str:
    return re.sub(r"\s+", " ", node.get_text(" ", strip=True)) if node else ""


def _slug_from_href(href: str) -> str:
    return href.rstrip("/").rsplit("/", 1)[-1]


def _card_name(card: Tag, anchor: Tag, href: str) -> str:
    """The display name of a catalog card.

    Cards come in two shapes. When the card *is* the link (visions), its own
    text is the whole card, so the name comes from the heading inside it.
    Otherwise the name is the text of the link pointing at the detail page --
    headings are unreliable there, because some cards carry section
    subheadings such as "Sources" that would otherwise win.
    """
    if card.name == "a":
        heading = card.select_one("h1, h2, h3")
        if heading:
            return _text(heading)
    else:
        for link in card.select("a[href]"):
            if link.get("href") == href:
                text = _text(link)
                if text:
                    return text
        heading = card.select_one("h1, h2")
        if heading:
            return _text(heading)
    return card.get("data-search-text", "").title()


def _parse_stats(card: Tag) -> tuple[dict[str, Any], str | None]:
    """Read the card's stat chips, and any trailing effect sentence."""
    stats: dict[str, Any] = {}
    raw = card.get("data-sort-values")
    if raw:
        try:
            for key, value in json.loads(raw).items():
                stats[STAT_FIELDS.get(key.upper(), key)] = value
        except json.JSONDecodeError:
            pass

    for pair in card.select("dl > div"):
        label = _text(pair.find("dt"))
        value = _text(pair.find("dd"))
        if not label or not value:
            continue
        field_name = STAT_FIELDS.get(label.upper())
        if field_name is None:
            continue
        try:
            stats[field_name] = int(value)
        except ValueError:
            stats[field_name] = value

    # The effect is the one block of prose on the card that is not a stat chip
    # or the item name. Factual mechanical text, kept verbatim only because it
    # is a game string rather than editorial writing.
    effect = None
    for candidate in card.select("p"):
        text = _text(candidate)
        if text and not text.upper() in STAT_FIELDS and len(text) > 12:
            effect = text
            break
    return stats, effect


#: "Cloud MR 3 (AP 15)" -- which Vision teaches a skill, at what Mastery Rank,
#: for how much AP. Both are optional: a Vision's innate skills show neither.
#: A label may also name an Awakening tier, as in "Cloud Awk. 2".
_SKILL_SOURCE = re.compile(
    r"^(?P<vision>.+?)"
    r"(?:\s+Awk\.\s*(?P<awk>\d+))?"
    r"(?:\s+MR\s*(?P<mr>\d+))?"
    r"(?:\s*\(\s*AP\s*(?P<ap>\d+)\s*\))?$"
)


def parse_skill_sources(card: Tag) -> list[dict[str, Any]]:
    """Read the Vision / Mastery Rank / AP entries from a skill card.

    The link text carries all three, as in ``Cloud MR 3 (AP 15)``.
    """
    found: list[dict[str, Any]] = []
    for link in card.select('a[href*="/visions/"]'):
        label = _text(link)
        if not label:
            continue
        match = _SKILL_SOURCE.match(label)
        if match is None:
            found.append(
                {
                    "vision": label,
                    "awakening": None,
                    "masteryRank": None,
                    "apCost": None,
                }
            )
            continue
        found.append(
            {
                "vision": match.group("vision").strip(),
                "awakening": int(match.group("awk")) if match.group("awk") else None,
                "masteryRank": int(match.group("mr")) if match.group("mr") else None,
                "apCost": int(match.group("ap")) if match.group("ap") else None,
            }
        )
    return found


def parse_catalog(html: str, kind: str) -> list[CatalogRecord]:
    """Parse any atma catalog page into records.

    Handles both card shapes the site uses: equipment-style ``<article>``
    cards with sort metadata, and link-style cards (visions, enemies).
    """
    soup = BeautifulSoup(html, "lxml")
    records: list[CatalogRecord] = []
    seen: set[str] = set()

    for card in soup.select("[data-search-text]"):
        anchor = card if card.name == "a" else card.select_one("a[href]")
        if anchor is None:
            continue
        href = anchor.get("href") or ""
        if "/" not in href:
            continue
        slug = _slug_from_href(href)
        if not slug or slug in seen:
            continue
        seen.add(slug)

        name = _card_name(card, anchor, href)

        catalog_number = None
        raw_number = card.get("data-sort-id")
        if raw_number and raw_number.isdigit():
            catalog_number = int(raw_number)

        attributes = {
            key.removeprefix("data-"): value
            for key, value in card.attrs.items()
            if key.startswith("data-")
            and key
            not in {
                "data-search-text",
                "data-sort-id",
                "data-sort-values",
                "data-catalog-card",
                "data-vision-card",
            }
            and isinstance(value, str)
            and value
        }

        stats, effect = _parse_stats(card)
        sources = parse_skill_sources(card) if kind == "skill" else []
        records.append(
            CatalogRecord(
                slug=slug,
                name=name,
                url=f"https://atma.gg{href}" if href.startswith("/") else href,
                kind=kind,
                catalogNumber=catalog_number,
                attributes=attributes,
                stats=stats,
                effect=effect,
                sources=sources,
            )
        )
    return records


def parse_tables(html: str) -> list[dict[str, Any]]:
    """Parse every ``<table>`` on a page into header-keyed row dicts."""
    soup = BeautifulSoup(html, "lxml")
    tables: list[dict[str, Any]] = []
    for index, table in enumerate(soup.select("table")):
        caption = _text(table.find("caption"))
        heading = table.find_previous(["h2", "h3", "h4"])
        headers = [_text(cell) for cell in table.select("thead th")]
        if not headers:
            first_row = table.select_one("tr")
            if first_row:
                headers = [_text(cell) for cell in first_row.select("th, td")]

        rows: list[dict[str, str]] = []
        for row in table.select("tbody tr") or table.select("tr")[1:]:
            cells = [_text(cell) for cell in row.select("th, td")]
            if not any(cells):
                continue
            if headers and len(headers) == len(cells):
                rows.append(dict(zip(headers, cells)))
            else:
                rows.append({f"col{i}": value for i, value in enumerate(cells)})

        tables.append(
            {
                "index": index,
                "caption": caption or _text(heading),
                "headers": headers,
                "rows": rows,
            }
        )
    return tables


def parse_detail(html: str) -> dict[str, Any]:
    """Pull the definition-list facts and stat chips off an item/enemy page."""
    soup = BeautifulSoup(html, "lxml")
    facts: dict[str, Any] = {}

    for pair in soup.select("dl > div, dl"):
        label = _text(pair.find("dt"))
        value = _text(pair.find("dd"))
        if label and value and label.lower() not in facts:
            facts[label.lower()] = value

    tables = parse_tables(html)
    if tables:
        facts["tables"] = [t for t in tables if t["rows"]]

    heading = soup.select_one("h1")
    if heading:
        facts["name"] = _text(heading)
    return facts


CATALOG_PAGES = {
    "weapon": "weapons",
    "armor": "armor",
    "accessory": "accessories",
    "item": "items",
    "enemy": "enemies",
    "vision": "visions",
    "skill": "skills",
}

TABLE_PAGES = {
    "tabletShards": "tablet-shards",
    "quests": "quests",
    "colosseum": "colosseum",
}


# -- detail pages ------------------------------------------------------------


def _dl_pairs(dl: Tag) -> dict[str, str]:
    pairs: dict[str, str] = {}
    terms = dl.find_all("dt")
    for term in terms:
        value = term.find_next_sibling("dd")
        key = _text(term)
        if key and value is not None:
            pairs.setdefault(key, _text(value))
    return pairs


def _section(soup: BeautifulSoup, heading: str) -> Tag | None:
    node = soup.find(["h2", "h3"], string=re.compile(rf"^\s*{heading}\s*$", re.I))
    return node.find_next_sibling() if node else None


def _int(value: str | None) -> int | None:
    if not value:
        return None
    digits = re.sub(r"[^\d-]", "", value)
    try:
        return int(digits)
    except ValueError:
        return None


#: "Potion Potion Restores 200 HP. 8.33%" -> name and rate.
def _loot_entry(value: str) -> dict[str, Any] | None:
    value = re.sub(r"\s+", " ", value or "").strip()
    if not value or value.lower() == "none":
        return None
    rate = None
    rate_match = re.search(r"([\d.]+)\s*%\s*$", value)
    if rate_match:
        rate = float(rate_match.group(1))
        value = value[: rate_match.start()].strip()
    repeat = re.match(r"^(.*?)\s+\1\b\s*(.*)$", value)
    name = repeat.group(1) if repeat else value.split(".")[0]
    return {"name": name.strip(), "dropRate": rate}


def parse_enemy_detail(html: str) -> dict[str, Any]:
    """Stats, affinities and loot from an enemy page.

    Layout: the first definition list is EXP/Gil, the second the stat block,
    and a later one the Drop/Steal table. Anything absent stays ``None``.
    """
    soup = BeautifulSoup(html, "lxml")
    lists = soup.select("dl")
    detail: dict[str, Any] = {}

    for dl in lists[:4]:
        pairs = _dl_pairs(dl)
        if "HP" in pairs:
            detail["hp"] = _int(pairs.get("HP"))
            detail["staggerGauge"] = _int(pairs.get("BP"))
            detail["stats"] = {
                key.lower(): _int(pairs.get(key))
                for key in ("ATK", "DEF", "MATK", "MDEF", "SANC", "SPD")
                if pairs.get(key)
            }
        if "EXP" in pairs:
            detail["exp"] = _int(pairs.get("EXP"))
            detail["gil"] = _int(pairs.get("Gil"))
        if "Drop" in pairs or "Steal" in pairs:
            drops = [
                entry
                for entry in (_loot_entry(pairs.get(k, "")) for k in ("Drop", "Rare drop"))
                if entry
            ]
            steals = [
                entry
                for entry in (_loot_entry(pairs.get(k, "")) for k in ("Steal", "Rare steal"))
                if entry
            ]
            detail["drops"] = drops
            detail["stealables"] = steals

    affinities = _section(soup, "Affinities")
    if affinities is not None:
        detail["weaknesses"] = sorted(
            {
                img.get("alt", "").strip()
                for img in affinities.select("img")
                if img.get("alt", "").strip()
            }
        )

    traits = _section(soup, "Traits")
    if traits is not None:
        text = _text(traits)
        detail["traits"] = text if text and "No " not in text[:4] else None

    heading = soup.select_one("h1")
    if heading:
        detail["name"] = _text(heading)
    return detail


def parse_item_detail(html: str) -> dict[str, Any]:
    """Acquisition routes and sell price from an item/equipment page."""
    soup = BeautifulSoup(html, "lxml")
    detail: dict[str, Any] = {}

    obtain = soup.find(["h2", "h3"], string=re.compile(r"How to Obtain", re.I))
    if obtain is not None:
        routes: list[str] = []
        for sibling in obtain.find_all_next():
            if sibling.name in {"h1", "h2"} and sibling is not obtain:
                break
            if sibling.name in {"h3", "h4"}:
                label = _text(sibling)
                body = sibling.find_next_sibling()
                value = _text(body) if body else ""
                if label:
                    routes.append(f"{label}: {value}".strip(": ").strip())
        if not routes:
            block = obtain.find_next_sibling()
            if block is not None:
                text = _text(block)
                if text:
                    routes.append(text)
        if routes:
            detail["acquisitionMethod"] = " | ".join(dict.fromkeys(routes))

    for dl in soup.select("dl"):
        pairs = _dl_pairs(dl)
        if "Sell Price" in pairs:
            detail["sellPrice"] = _int(pairs["Sell Price"])
    text = _text(soup.select_one("main") or soup)
    sell = re.search(r"Sell Price\s+([\d,]+)\s*Gil", text)
    if sell and "sellPrice" not in detail:
        detail["sellPrice"] = _int(sell.group(1))
    return detail

"""Registry of guide data sources and what each one permits.

Every fetch in this pipeline goes through :mod:`fetch`, which refuses any host
that is not marked ``CRAWL_OK`` here. The statuses below were read from each
site's own robots.txt and terms on 2026-10-26; re-run ``python3 -m
guide_pipeline.sources --recheck`` to print the live files and review them
again.

Why this file exists: several of the sources originally planned for this
dataset forbid automated access. Rather than leave that implicit, the
pipeline encodes it, so a future contributor cannot accidentally crawl a
source that said no.
"""

from __future__ import annotations

import sys
from dataclasses import dataclass
from enum import Enum


class Status(str, Enum):
    #: robots.txt permits automated access for a generic agent.
    CRAWL_OK = "crawl_ok"
    #: The site names Anthropic/Claude crawlers in robots.txt and disallows them.
    BLOCKS_CLAUDE = "blocks_claude"
    #: Terms forbid automated retrieval and/or dataset creation outright.
    FORBIDS_SCRAPING = "forbids_scraping"
    #: robots.txt unavailable (5xx); RFC 9309 says assume complete disallow.
    ROBOTS_UNAVAILABLE = "robots_unavailable"
    #: Reachable, but the paths holding the files we want are disallowed.
    ASSETS_DISALLOWED = "assets_disallowed"


@dataclass(frozen=True)
class Source:
    key: str
    host: str
    base_url: str
    status: Status
    covers: str
    note: str

    @property
    def crawlable(self) -> bool:
        return self.status is Status.CRAWL_OK


SOURCES: tuple[Source, ...] = (
    Source(
        key="atma",
        host="atma.gg",
        base_url="https://atma.gg/final-fantasy-resonance",
        status=Status.CRAWL_OK,
        covers=(
            "weapons, armor, accessories, items, enemies, visions, skills, "
            "espers, quests, locations, tablet shards, colosseum, "
            "chamber of arms, optional bosses"
        ),
        note=(
            "robots.txt carries only the Cloudflare content-signal preamble with "
            "no User-agent/Disallow directives and no signal values set, which "
            "its own clause (c) defines as neither granting nor restricting. "
            "Crawled politely with a descriptive agent and a delay."
        ),
    ),
    Source(
        key="ffrbuilds",
        host="ffrbuilds.com",
        base_url="https://ffrbuilds.com",
        status=Status.CRAWL_OK,
        covers="vision acquisition routes, missables, tier list, achievements",
        note="robots.txt is 'User-agent: * / Allow: /' plus a sitemap.",
    ),
    Source(
        key="powerpyx",
        host="www.powerpyx.com",
        base_url="https://www.powerpyx.com",
        status=Status.CRAWL_OK,
        covers="all 99 tablet shard locations, collectible walkthroughs",
        note=(
            "robots.txt is three lines: 'User-agent: * / Allow: /' plus a "
            "sitemap. No AI-crawler or data-mining restriction of any kind."
        ),
    ),
    Source(
        key="gamersocialclub",
        host="gamersocialclub.ca",
        base_url="https://gamersocialclub.ca",
        status=Status.CRAWL_OK,
        covers="tablet shard overview, used to cross-check conflicts",
        note="robots.txt is 'User-agent: * / Disallow:' -- an empty "
        "Disallow, which permits everything.",
    ),
    Source(
        key="gamingpromax",
        host="gamingpromax.com",
        base_url="https://gamingpromax.com",
        status=Status.CRAWL_OK,
        covers="tablet shard list, used to cross-check conflicts",
        note=(
            "robots.txt names ClaudeBot explicitly and allows it, disallowing "
            "only /wp-admin/, search and the translated subtrees. Its shard 99 "
            "placement disagrees with other sources, so it is used only as a "
            "second opinion, never as the sole source for a record."
        ),
    ),
    Source(
        key="fextralife",
        host="fextralife.com",
        base_url="https://finalfantasyresonance.wiki.fextralife.com",
        status=Status.ROBOTS_UNAVAILABLE,
        covers="tablet shards grouped by area",
        note=(
            "robots.txt returns HTTP 502 and the wiki subdomain does not "
            "resolve; RFC 9309 says assume complete disallow."
        ),
    ),
    Source(
        key="nightlygamingbinge",
        host="nightlygamingbinge.com",
        base_url="https://nightlygamingbinge.com",
        status=Status.BLOCKS_CLAUDE,
        covers="tablet shard guide",
        note="robots.txt sets 'User-agent: ClaudeBot / Disallow: /'.",
    ),
    Source(
        key="gamerant",
        host="gamerant.com",
        base_url="https://gamerant.com",
        status=Status.ROBOTS_UNAVAILABLE,
        covers="tablet shard locations article",
        note="robots.txt could not be retrieved; assume complete disallow.",
    ),
    Source(
        key="ign",
        host="www.ign.com",
        base_url="https://www.ign.com/wikis/final-fantasy-resonance",
        status=Status.FORBIDS_SCRAPING,
        covers="99 tablet shards, equipment locations, steal guide",
        note=(
            "Ziff Davis robots.txt header: automated scraping prohibited without "
            "written permission, and prohibited uses explicitly include "
            "'creating data sets containing our content or sharing it with "
            "others'. Contact licensing@ziffdavis.com for permission."
        ),
    ),
    Source(
        key="mapgenie",
        host="mapgenie.io",
        base_url="https://mapgenie.io/final-fantasy-resonance",
        status=Status.FORBIDS_SCRAPING,
        covers="esper map locations",
        note="Same Ziff Davis terms as IGN.",
    ),
    Source(
        key="ffresonance_wiki",
        host="ffresonance.wiki",
        base_url="https://ffresonance.wiki",
        status=Status.BLOCKS_CLAUDE,
        covers="visions, bestiary, espers, equipment, quests",
        note=(
            "robots.txt sets 'User-agent: ClaudeBot / Disallow: /' and the same "
            "for anthropic-ai. Search and some answer engines are welcomed, but "
            "Anthropic crawlers are named and refused."
        ),
    ),
    Source(
        key="meowdb",
        host="meowdb.com",
        base_url="https://meowdb.com/db/final-fantasy-resonance",
        status=Status.BLOCKS_CLAUDE,
        covers="chapter-by-chapter walkthrough, dungeon puzzles",
        note=(
            "robots.txt disallows ClaudeBot, Claude-Web and anthropic-ai by name."
        ),
    ),
    Source(
        key="rpgsite",
        host="www.rpgsite.net",
        base_url="https://www.rpgsite.net",
        status=Status.ROBOTS_UNAVAILABLE,
        covers="100% walkthrough, achievements",
        note=(
            "robots.txt returns HTTP 500. RFC 9309 section 2.3.1.3 says a crawler "
            "should assume complete disallow when robots.txt is unreachable."
        ),
    ),
    Source(
        key="se_press",
        host="press.es.square-enix.com",
        base_url="https://press.es.square-enix.com/FINAL-FANTASY-RESONANCE",
        status=Status.ASSETS_DISALLOWED,
        covers="official promotional art, logos, screenshots",
        note=(
            "robots.txt allows the site with Crawl-Delay 5 but disallows "
            "/Files/File and /Files/Download, which are the asset download "
            "paths. Separately, press assets are licensed for editorial "
            "coverage, not for redistribution inside an application."
        ),
    ),
)

BY_HOST = {s.host: s for s in SOURCES}
BY_KEY = {s.key: s for s in SOURCES}

CRAWLABLE_HOSTS = frozenset(s.host for s in SOURCES if s.crawlable)


def status_for(host: str) -> Source | None:
    """Return the registry entry for ``host``, matching parent domains too."""
    if host in BY_HOST:
        return BY_HOST[host]
    for known_host, source in BY_HOST.items():
        if host == known_host or host.endswith("." + known_host):
            return source
    return None


def report() -> str:
    width = max(len(s.key) for s in SOURCES)
    lines = ["Source permissions (read 2026-10-26):", ""]
    for source in sorted(SOURCES, key=lambda s: (not s.crawlable, s.key)):
        mark = "OK  " if source.crawlable else "NO  "
        lines.append(f"  {mark}{source.key.ljust(width)}  {source.status.value}")
        lines.append(f"      covers: {source.covers}")
        lines.append(f"      {source.note}")
        lines.append("")
    return "\n".join(lines)


if __name__ == "__main__":
    print(report())
    if "--recheck" in sys.argv:
        import requests

        for source in SOURCES:
            print(f"\n{'=' * 70}\n{source.host}/robots.txt\n{'=' * 70}")
            try:
                response = requests.get(
                    f"https://{source.host}/robots.txt", timeout=20
                )
                print(f"HTTP {response.status_code}")
                print(response.text[:2000])
            except Exception as exc:  # pragma: no cover - diagnostic path
                print(f"error: {exc}")

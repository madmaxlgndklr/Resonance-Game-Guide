"""Polite, cached HTTP fetching for the guide pipeline.

Every request is gated twice: once against :mod:`sources` (is this host one we
are allowed to crawl at all?) and once against the host's live robots.txt for
our own user agent. Responses are cached on disk so re-running the pipeline
costs the sources nothing.
"""

from __future__ import annotations

import hashlib
import time
import urllib.parse
import urllib.robotparser
from pathlib import Path

import requests

from sources import Status, status_for

#: Descriptive agent. Identifies the project so operators can see who we are
#: in their logs, and can block us if they would rather we did not fetch.
USER_AGENT = (
    "ResonanceGuideBot/0.1 "
    "(+https://github.com/madmaxlgndklr/Resonance-Game-Guide; "
    "offline fan guide; contact via repository issues)"
)

CACHE_DIR = Path(__file__).resolve().parent / ".cache"
DEFAULT_DELAY = 1.5


class NotPermitted(RuntimeError):
    """Raised when a fetch is refused for policy reasons, never for network ones."""


class Fetcher:
    def __init__(
        self,
        delay: float = DEFAULT_DELAY,
        cache_dir: Path = CACHE_DIR,
        offline: bool = False,
    ) -> None:
        self.delay = delay
        self.cache_dir = cache_dir
        self.offline = offline
        self.cache_dir.mkdir(parents=True, exist_ok=True)
        self._session = requests.Session()
        self._session.headers["User-Agent"] = USER_AGENT
        self._robots: dict[str, urllib.robotparser.RobotFileParser | None] = {}
        self._last_request: dict[str, float] = {}

    # -- policy ---------------------------------------------------------------

    def _check_registry(self, url: str) -> None:
        host = urllib.parse.urlparse(url).netloc
        source = status_for(host)
        if source is None:
            raise NotPermitted(
                f"{host} is not in the source registry. Add it to sources.py "
                f"with its robots.txt status before fetching from it."
            )
        if not source.crawlable:
            raise NotPermitted(
                f"{host} is marked {source.status.value}: {source.note}"
            )

    def _robots_for(self, host: str) -> urllib.robotparser.RobotFileParser | None:
        if host in self._robots:
            return self._robots[host]
        parser = urllib.robotparser.RobotFileParser()
        robots_url = f"https://{host}/robots.txt"
        try:
            response = self._session.get(robots_url, timeout=20)
        except requests.RequestException:
            # Unreachable robots.txt: treat as disallow (RFC 9309 2.3.1.3).
            self._robots[host] = None
            return None
        if response.status_code >= 500:
            self._robots[host] = None
            return None
        if response.status_code >= 400:
            # No robots.txt published: nothing is disallowed.
            parser.parse([])
        else:
            parser.parse(response.text.splitlines())
        self._robots[host] = parser
        return parser

    def _check_robots(self, url: str) -> None:
        host = urllib.parse.urlparse(url).netloc
        parser = self._robots_for(host)
        if parser is None:
            raise NotPermitted(
                f"{host}/robots.txt is unreachable; assuming complete disallow."
            )
        if not parser.can_fetch(USER_AGENT, url):
            raise NotPermitted(f"robots.txt disallows {url} for {USER_AGENT!r}")

    # -- fetching -------------------------------------------------------------

    def _cache_path(self, url: str) -> Path:
        digest = hashlib.sha256(url.encode("utf-8")).hexdigest()[:20]
        host = urllib.parse.urlparse(url).netloc.replace(":", "_")
        return self.cache_dir / host / f"{digest}.html"

    def _throttle(self, host: str) -> None:
        previous = self._last_request.get(host)
        if previous is not None:
            remaining = self.delay - (time.monotonic() - previous)
            if remaining > 0:
                time.sleep(remaining)
        self._last_request[host] = time.monotonic()

    def get(self, url: str, *, refresh: bool = False) -> str:
        """Return the body of ``url``, from cache when possible.

        Raises :class:`NotPermitted` if policy forbids the fetch, which callers
        should treat as a permanent answer rather than retrying.
        """
        cache_path = self._cache_path(url)
        if cache_path.is_file() and not refresh:
            return cache_path.read_text(encoding="utf-8")
        if self.offline:
            raise NotPermitted(f"offline mode and {url} is not cached")

        self._check_registry(url)
        self._check_robots(url)

        host = urllib.parse.urlparse(url).netloc
        self._throttle(host)
        response = self._session.get(url, timeout=40)
        response.raise_for_status()
        # Decode as UTF-8 rather than letting requests guess from headers:
        # these pages are UTF-8 but often omit the charset, and a latin-1
        # guess silently mangles names such as Y'shtola.
        if not response.encoding or response.encoding.lower() == "iso-8859-1":
            response.encoding = response.apparent_encoding or "utf-8"
        body = response.content.decode("utf-8", errors="replace")
        cache_path.parent.mkdir(parents=True, exist_ok=True)
        cache_path.write_text(body, encoding="utf-8")
        return body


__all__ = ["Fetcher", "NotPermitted", "USER_AGENT", "Status"]

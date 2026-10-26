# Guide data pipeline

Builds `app/src/main/assets/ff_resonance_guide.json` — the dataset the app
ships — from a hand-verified seed file plus the sub-tables we are permitted to
crawl.

```bash
python3 -m pip install -r requirements.txt

# Build from the seed (catalog pages only, ~1 min)
python3 build_guide.py --seed /path/to/ff_resonance_android_guide_data.json

# Also fetch each item/enemy page for acquisition, drops and steal tables
# (~1,100 requests at 1.5 s each; resumable — the cache makes a re-run cheap)
python3 build_guide.py --seed /path/to/seed.json --details

# Validate the result
python3 validate_json.py

# Regenerate the app's Kotlin models after changing schema.py
python3 gen_kotlin.py
```

## Files

| File | Role |
| --- | --- |
| `sources.py` | Every source and what it permits. **Read this first.** |
| `fetch.py` | Polite cached fetcher; refuses any host `sources.py` does not clear. |
| `parse_atma.py` | Catalog, table and detail-page parsers. |
| `schema.py` | Pydantic models — the contract for the shipped JSON. |
| `ids.py` | Deterministic snake_case id helpers. |
| `build_guide.py` | Assembles the dataset and records its own coverage gaps. |
| `validate_json.py` | UTF-8, schema, id uniqueness, foreign keys, counts. |
| `gen_kotlin.py` | Emits `data/model/GuideModels.kt` from `schema.py`. |

Responses are cached under `.cache/` (git-ignored), so re-running costs the
sources nothing and an interrupted `--details` run resumes where it stopped.

## Source permissions

`python3 sources.py` prints the current table; `--recheck` re-fetches each
robots.txt so you can review it yourself.

Of the sources originally planned for this dataset, most do not permit
automated access:

| Source | Covers | Status |
| --- | --- | --- |
| **atma.gg** | equipment, items, enemies, visions, skills, quests, colosseum, shard rewards | **crawled** — robots.txt sets no directives |
| **ffrbuilds.com** | vision routes, missables | **crawlable** — `Allow: /` |
| ign.com | all 99 shard locations, equipment locations, steal guide | refused — Ziff Davis terms prohibit automated retrieval *and* "creating data sets containing our content" |
| mapgenie.io | esper map locations | refused — same terms |
| ffresonance.wiki | visions, bestiary, equipment | refused — `User-agent: ClaudeBot → Disallow: /` |
| meowdb.com | chapter walkthroughs, puzzles | refused — disallows ClaudeBot, Claude-Web, anthropic-ai |
| rpgsite.net | 100% walkthrough | refused — robots.txt returns HTTP 500, which RFC 9309 says to treat as full disallow |
| press.es.square-enix.com | official art | asset paths `/Files/Download` and `/Files/File` disallowed |

The fetcher enforces this. A refused host raises `NotPermitted`, which is a
permanent answer, not something to retry or work around by changing the user
agent.

## Rules this pipeline follows

1. **Facts, not prose.** Numeric stats, item names, objective strings and
   mechanical effect text (game strings) are extracted. Editorial writing from
   community wikis is not copied; strategy summaries in the seed are original
   phrasing.
2. **No invented data.** A field with no confirmed source is `null`, and the
   shortfall is recorded in `meta.coverage.gaps`. `validate_json.py` treats an
   unrecorded shortfall as an error, so the dataset cannot be quietly
   incomplete.
3. **Stable ids.** Every trackable record has an immutable snake_case `id`
   derived only from its name and, where available, the game's own catalog
   number — never from list position. Checklist state keys off these, so an id
   must never change once shipped.
4. **Conflicts are recorded, not resolved.** Where sources disagree, both
   readings go in a `notes` field. For example the seed's equipment counts
   (82 weapons / 36 armor / 51 accessories, from IGN) disagree with atma.gg's
   catalog (250 / 108 / 96); the dataset carries atma's rows, because that is
   the source it could verify, and `meta.coverage` records both figures.
5. **No bundled artwork.** See below.

## Artwork

`assetsManifest` records art by URL, with a licence note, and
`bundled: false` on every entry. Nothing is downloaded into the APK.

Square Enix key art, press-kit files and in-game sprites are the publisher's
copyright. "Official" and "not watermarked" are not the same as "licensed for
redistribution": press material is provided for editorial coverage, and
bundling it inside a distributed app is redistribution. The cover art on
Wikipedia is non-free media used there under fair use, which does not extend to
this project.

If you want art in the app, the routes that actually work are: commission or
draw original assets; use the publisher's own licensing contact to ask for
explicit permission; or ship the app art-free and let it show placeholders.

package com.madmaxlgndklr.resonanceguide.data

import com.madmaxlgndklr.resonanceguide.data.model.Guide
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parses the shipped dataset with the generated models.
 *
 * This is a JVM test rather than an instrumented one so it runs on every
 * `./gradlew test`: it catches the case where `scripts/guide_pipeline` emits a
 * field shape the Kotlin models cannot read, which would otherwise only show
 * up as a crash on first launch.
 */
class GuideDatasetTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val guide: Guide by lazy {
        val asset = File("src/main/assets/ff_resonance_guide.json")
        assertTrue(
            "dataset not found at ${asset.absolutePath}; run scripts/guide_pipeline/build_guide.py",
            asset.isFile,
        )
        json.decodeFromString<Guide>(asset.readText())
    }

    @Test
    fun `dataset parses into the generated models`() {
        assertEquals("FINAL FANTASY RESONANCE", guide.meta.gameTitle)
        assertTrue(guide.meta.datasetVersion.isNotBlank())
    }

    @Test
    fun `expected collections are fully populated`() {
        assertEquals(9, guide.chapters.size)
        assertEquals(26, guide.visions.size)
        assertEquals(9, guide.espers.size)
        assertEquals(33, guide.sideQuests.size)
        assertEquals(15, guide.caveMasters.size)
        assertEquals(12, guide.chamberOfArms.size)
        assertEquals(8, guide.adamantiteLocations.size)
        assertEquals(99, guide.collectibles.tabletShards.size)
        assertEquals(20, guide.colosseum.sumOf { it.battles.size })
        assertTrue(guide.equipment.size > 400)
        assertTrue(guide.bestiary.size > 500)
    }

    @Test
    fun `every id is stable snake_case and unique`() {
        val pattern = Regex("^[a-z][a-z0-9_]*$")
        val ids = buildList {
            guide.chapters.forEach { chapter ->
                add(chapter.id)
                chapter.objectives.forEach { add(it.id) }
            }
            guide.visions.forEach { add(it.id) }
            guide.espers.forEach { add(it.id) }
            guide.sideQuests.forEach { add(it.id) }
            guide.caveMasters.forEach { add(it.id) }
            guide.equipment.forEach { add(it.id) }
            guide.items.forEach { add(it.id) }
            guide.bestiary.forEach { add(it.id) }
            guide.collectibles.tabletShards.forEach { add(it.id) }
            guide.collectibles.oblivionPalaceTiers.forEach { add(it.id) }
        }
        val malformed = ids.filterNot { pattern.matches(it) }
        assertEquals("ids must be stable snake_case", emptyList<String>(), malformed)

        val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertEquals("ids must be unique", emptySet<String>(), duplicates)
    }

    @Test
    fun `tablet shards are numbered 1 to 99 and all described`() {
        val shards = guide.collectibles.tabletShards
        assertEquals((1..99).toList(), shards.map { it.number }.sorted())
        assertEquals("shard_001", shards.first { it.number == 1 }.id)
        assertEquals("shard_099", shards.first { it.number == 99 }.id)
        val undescribed = shards.filter { it.exactLocationDescription.isNullOrBlank() }
        assertEquals(emptyList<Int>(), undescribed.map { it.number })
    }

    @Test
    fun `vision mastery ranks are odd values between 1 and 9`() {
        val ranks = guide.visions
            .flatMap { it.skills }
            .mapNotNull { it.masteryRank }
            .toSet()
        assertTrue("unexpected mastery ranks: $ranks", ranks.all { it in setOf(1, 3, 5, 7, 9) })
        assertTrue("no mastery ranks parsed", ranks.isNotEmpty())
    }

    @Test
    fun `detail crawl populated acquisition and enemy stats`() {
        assertTrue(guide.equipment.all { it.acquisitionMethod != null })
        assertTrue(guide.bestiary.all { it.hp != null })
        assertTrue(guide.bestiary.any { it.stealables.isNotEmpty() })
    }

    @Test
    fun `no artwork is bundled and every asset records its licence`() {
        guide.assetsManifest.entries.forEach { asset ->
            assertTrue("asset ${asset.id} is marked bundled", !asset.bundled)
            assertTrue(asset.licenseStatus.isNotBlank())
        }
        assertNotNull(guide.assetsManifest.licenceNote)
    }

    @Test
    fun `coverage records any shortfall against expectations`() {
        val coverage = guide.meta.coverage
        coverage.expected.forEach { (key, want) ->
            val have = coverage.actual[key]
            assertNotNull("coverage.actual missing $key", have)
            if (have != want) {
                assertTrue(
                    "$key is $have of $want but no gap mentions it",
                    coverage.gaps.any { it.contains(key, ignoreCase = true) },
                )
            }
        }
    }
}

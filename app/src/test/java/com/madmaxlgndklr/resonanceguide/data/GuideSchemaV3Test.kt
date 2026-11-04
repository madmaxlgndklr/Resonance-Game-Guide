package com.madmaxlgndklr.resonanceguide.data

import com.madmaxlgndklr.resonanceguide.data.local.GuideSchemaV3
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks `GuideSchemaV3.ADDED_STATEMENTS` against the exported schema.
 *
 * `MIGRATION_2_3` creates the version 3 tables from that list, and Room
 * validates the result against `3.json` on every upgrade. If the two drift,
 * the migration throws on a user's device. This keeps that a build failure.
 *
 * Version 3 is additive, so this checks only the tables version 2 did not
 * have; the rest are [GuideSchemaV2Test]'s business. A JVM test rather than an
 * instrumented one, so it runs on every `./gradlew test` without an emulator.
 */
class GuideSchemaV3Test {

    private val schemaDir = File("schemas/com.madmaxlgndklr.resonanceguide.data.local.GuideDatabase")

    /** Tables added in version 3, with their indices, in Room's own order. */
    private val addedTables = listOf("walkthrough_areas", "dialogue_choices")

    private fun statements(version: Int, tables: Collection<String>): List<String> {
        val file = File(schemaDir, "$version.json")
        assertTrue(
            "exported schema not found at ${file.absolutePath}; " +
                "build the app once to export it",
            file.isFile,
        )
        val database = Json.parseToJsonElement(file.readText())
            .jsonObject.getValue("database").jsonObject

        return buildList {
            database.getValue("entities").jsonArray.forEach { element ->
                val entity = element.jsonObject
                val table = entity.getValue("tableName").jsonPrimitive.content
                if (table !in tables) return@forEach
                fun substitute(sql: String) = sql.replace("\${TABLE_NAME}", table)

                add(substitute(entity.getValue("createSql").jsonPrimitive.content))
                entity["indices"]?.jsonArray?.forEach { index ->
                    add(
                        substitute(
                            index.jsonObject.getValue("createSql").jsonPrimitive.content,
                        ),
                    )
                }
            }
        }
    }

    private fun tableNames(version: Int): Set<String> {
        val file = File(schemaDir, "$version.json")
        assertTrue("exported schema not found at ${file.absolutePath}", file.isFile)
        return Json.parseToJsonElement(file.readText())
            .jsonObject.getValue("database").jsonObject
            .getValue("entities").jsonArray
            .map { it.jsonObject.getValue("tableName").jsonPrimitive.content }
            .toSet()
    }

    @Test
    fun `migration DDL matches the exported version 3 schema`() {
        assertEquals(
            "GuideSchemaV3.ADDED_STATEMENTS is out of step with 3.json. Copy it " +
                "from the exported schema; do not hand-edit.",
            statements(3, addedTables),
            GuideSchemaV3.ADDED_STATEMENTS,
        )
    }

    @Test
    fun `version 3 only adds tables`() {
        val v2 = tableNames(2)
        val v3 = tableNames(3)

        assertEquals(
            "version 3 removed a table, which MIGRATION_2_3 does not handle",
            emptySet<String>(),
            v2 - v3,
        )
        assertEquals(
            "a table was added in version 3 that MIGRATION_2_3 does not create",
            addedTables.toSet(),
            v3 - v2,
        )
    }

    @Test
    fun `the version 3 tables carried over from version 2 are unchanged`() {
        // MIGRATION_2_3 runs no ALTER statements, so every table version 2
        // already had must be byte-identical in 3.json. If one changed, the
        // migration would leave the database in a shape Room rejects.
        val shared = tableNames(2)
        assertEquals(
            "a version 2 table changed shape in version 3; MIGRATION_2_3 only " +
                "creates new tables and cannot migrate an altered one",
            statements(2, shared),
            statements(3, shared),
        )
    }

    @Test
    fun `progress is not touched by version 3`() {
        assertTrue(
            "MIGRATION_2_3 must not create or drop progress: it holds the " +
                "user's check-offs and has to survive the upgrade untouched",
            GuideSchemaV3.ADDED_STATEMENTS.none { it.contains("`progress`") },
        )
    }

    @Test
    fun `area and dialogue tables are keyed by the dataset ids`() {
        val areas = GuideSchemaV3.ADDED_STATEMENTS
            .single { it.startsWith("CREATE TABLE IF NOT EXISTS `walkthrough_areas`") }
        assertTrue(areas.contains("`id` TEXT NOT NULL"))
        assertTrue(areas.contains("PRIMARY KEY(`id`)"))
        // Progress rows key off these ids, so a rebuild must not move them.
        assertTrue(
            "walkthrough_areas must cascade from chapters",
            areas.contains("REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE"),
        )

        val dialogue = GuideSchemaV3.ADDED_STATEMENTS
            .single { it.startsWith("CREATE TABLE IF NOT EXISTS `dialogue_choices`") }
        assertTrue(dialogue.contains("`id` TEXT NOT NULL"))
        assertTrue(dialogue.contains("PRIMARY KEY(`id`)"))
    }
}

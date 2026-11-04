package com.madmaxlgndklr.resonanceguide.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.madmaxlgndklr.resonanceguide.data.local.GuideDatabase
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the schema migrations: the version 1 -> 2 move from integer primary
 * keys to the dataset's stable string ids, and the additive 2 -> 3 that adds
 * the walkthrough areas and dialogue choices.
 *
 * Run on a device or emulator: `./gradlew :app:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GuideDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2_dropsTheIntegerKeyedTables() {
        helper.createDatabase(DB_NAME, 1).apply {
            // A version 1 row, keyed the old way, to prove progress is rebuilt.
            execSQL(
                "INSERT INTO progress (type, entityId, completed, updatedAt) " +
                    "VALUES ('QUEST_STEP', 1001, 1, 0)",
            )
            close()
        }

        // The migration has to be passed in: MigrationTestHelper does not read
        // the database's own migration list, and without it Room reports that a
        // migration from 1 to 2 was required but not found. Room then validates
        // the resulting schema against schemas/2.json and fails the call if the
        // migration leaves the database in the wrong shape.
        val db = helper.runMigrationsAndValidate(
            DB_NAME,
            2,
            true,
            GuideDatabase.MIGRATION_1_2,
        )

        // progress is rebuilt empty: an integer id cannot be mapped onto a
        // string one, which is the documented cost of this migration.
        db.query("SELECT COUNT(*) FROM progress").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }

        // entityId now takes a string key without complaint.
        db.execSQL(
            "INSERT INTO progress (type, entityId, completed, updatedAt) " +
                "VALUES ('SHARD', 'shard_001', 1, 0)",
        )
        db.query("SELECT entityId FROM progress").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("shard_001", cursor.getString(0))
        }
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3_addsTheNewTablesAndKeepsProgress() {
        helper.createDatabase(DB_NAME, 2).apply {
            // A chapter to hang an area off, since walkthrough_areas has a
            // foreign key onto it, and a check-off that must survive.
            execSQL(
                "INSERT INTO chapters (id, chapterNumber, title, region, " +
                    "recommendedLevel, storyBosses, optionalFights, mapAsset) " +
                    "VALUES ('ch_01_x', 1, 'Chapter 1', NULL, NULL, '[]', '[]', NULL)",
            )
            execSQL(
                "INSERT INTO progress (type, entityId, completed, updatedAt) " +
                    "VALUES ('SHARD', 'shard_001', 1, 42)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            DB_NAME,
            3,
            true,
            GuideDatabase.MIGRATION_2_3,
        )

        // The point of an additive migration: nothing is lost.
        db.query("SELECT entityId, completed, updatedAt FROM progress").use { cursor ->
            assertTrue("progress row did not survive the upgrade", cursor.moveToFirst())
            assertEquals("shard_001", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
            assertEquals(42, cursor.getInt(2))
        }

        // Both new tables exist and accept a row.
        db.execSQL(
            "INSERT INTO walkthrough_areas (id, chapterId, sortOrder, name, kind, " +
                "summary, steps, chests, bosses, shardCount, sideQuests, notableSteals) " +
                "VALUES ('ch_01_x_area_earth_shrine', 'ch_01_x', 1, 'Earth Shrine', " +
                "'dungeon', NULL, '[]', '[]', '[]', 2, '[]', '[]')",
        )
        db.query("SELECT name, shardCount FROM walkthrough_areas").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Earth Shrine", cursor.getString(0))
            assertEquals(2, cursor.getInt(1))
        }

        db.execSQL(
            "INSERT INTO dialogue_choices (id, chapterNumber, speaker, prompt, " +
                "choice, bonus) VALUES ('dlg_x', 1, 'Rain', 'Earth Shrine', " +
                "'Cohesion is key.', 'Spirit +3')",
        )
        db.query("SELECT speaker, bonus FROM dialogue_choices").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Rain", cursor.getString(0))
            assertEquals("Spirit +3", cursor.getString(1))
        }

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To3_runsBothMigrationsInOrder() {
        helper.createDatabase(DB_NAME, 1).close()

        // A user on the never-released version 1 upgrades straight to 3, so
        // both migrations have to compose: 1 -> 2 recreates the schema, 2 -> 3
        // adds to it. Room validates the result against schemas/3.json.
        val db = helper.runMigrationsAndValidate(
            DB_NAME,
            3,
            true,
            GuideDatabase.MIGRATION_1_2,
            GuideDatabase.MIGRATION_2_3,
        )

        db.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND " +
                "name IN ('walkthrough_areas', 'dialogue_choices', 'progress')",
        ).use { cursor ->
            val tables = buildSet {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
            assertEquals(
                setOf("walkthrough_areas", "dialogue_choices", "progress"),
                tables,
            )
        }
        db.close()
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}

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
 * Covers the version 1 -> 2 move from integer primary keys to the dataset's
 * stable string ids.
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

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}

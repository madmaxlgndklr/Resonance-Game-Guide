package com.madmaxlgndklr.resonanceguide.data

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.madmaxlgndklr.resonanceguide.data.local.GuideDatabase
import com.madmaxlgndklr.resonanceguide.data.local.GuideSeeder
import com.madmaxlgndklr.resonanceguide.data.local.ProgressEntity
import com.madmaxlgndklr.resonanceguide.data.local.ProgressType
import com.madmaxlgndklr.resonanceguide.data.local.SeedResult
import com.madmaxlgndklr.resonanceguide.data.model.GuideDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Seeds the bundled dataset into an in-memory database.
 *
 * The second test is the point of the whole string-id migration: a content
 * refresh must not disturb the user's checklist.
 *
 * Run on a device or emulator: `./gradlew :app:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class GuideSeederTest {

    private lateinit var database: GuideDatabase
    private lateinit var seeder: GuideSeeder
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GuideDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefs = context.getSharedPreferences("seeder-test", Context.MODE_PRIVATE)
            .also { it.edit().clear().commit() }
        seeder = GuideSeeder(
            database = database,
            dataSource = GuideDataSource(context),
            prefs = prefs,
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun seedsEveryContentTable() = runTest {
        val result = seeder.seedIfNeeded()
        assertTrue("expected a fresh seed, got $result", result is SeedResult.Seeded)

        val dao = database.guideDao()
        assertEquals(9, dao.observeChapterSummaries().first().size)
        assertEquals(26, dao.observeVisions().first().size)
        assertEquals(99, dao.observeAllShards().first().size)
        assertEquals(33, dao.observeSideQuestsForChapter(1).first().size +
            (2..8).sumOf { dao.observeSideQuestsForChapter(it).first().size })
        assertTrue(dao.observeEquipment().first().size > 400)
        assertTrue(dao.observeEnemies().first().size > 500)
        // Cave Masters plus Espers.
        assertEquals(24, dao.observeOptionalBosses().first().size)
        assertEquals(25, dao.observeOblivionTiers().first().size)
    }

    @Test
    fun reseedingKeepsProgress() = runTest {
        seeder.seedIfNeeded()

        database.progressDao().upsert(
            ProgressEntity(ProgressType.SHARD, "shard_001", completed = true),
        )
        val before = database.guideDao().observeAllShards().first()
            .first { it.shard.id == "shard_001" }
        assertTrue(before.collected)

        // A second call with the version already recorded does nothing.
        assertTrue(
            "second call should be a no-op",
            seeder.seedIfNeeded() is SeedResult.AlreadyCurrent,
        )

        // Now forget the recorded version, which is what shipping a newer
        // dataset looks like to the seeder, and let it rewrite every content
        // table.
        prefs.edit().clear().commit()
        assertTrue(
            "clearing the recorded version should force a reseed",
            seeder.seedIfNeeded() is SeedResult.Seeded,
        )

        val after = database.guideDao().observeAllShards().first()
            .first { it.shard.id == "shard_001" }
        assertTrue("progress must survive a content refresh", after.collected)
    }
}

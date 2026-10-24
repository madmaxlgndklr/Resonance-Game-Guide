package com.madmaxlgndklr.resonanceguide.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.room.withTransaction
import com.madmaxlgndklr.resonanceguide.data.local.BossWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.ChapterEntity
import com.madmaxlgndklr.resonanceguide.data.local.CharacterBuildEntity
import com.madmaxlgndklr.resonanceguide.data.local.GuideDatabase
import com.madmaxlgndklr.resonanceguide.data.local.ItemWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.ProgressEntity
import com.madmaxlgndklr.resonanceguide.data.local.ProgressType
import com.madmaxlgndklr.resonanceguide.data.local.QuestEntity
import com.madmaxlgndklr.resonanceguide.data.local.QuestStepWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.QuestSummary
import com.madmaxlgndklr.resonanceguide.data.local.SeedData
import kotlinx.coroutines.flow.Flow

class GuideRepository(
    private val database: GuideDatabase,
    private val prefs: SharedPreferences,
) {
    private val guideDao = database.guideDao()
    private val progressDao = database.progressDao()

    fun chapters(): Flow<List<ChapterEntity>> = guideDao.observeChapters()
    fun chapter(chapterId: Int): Flow<ChapterEntity?> = guideDao.observeChapter(chapterId)
    fun questSummaries(): Flow<List<QuestSummary>> = guideDao.observeQuestSummaries()
    fun quest(questId: Int): Flow<QuestEntity?> = guideDao.observeQuest(questId)
    fun steps(questId: Int): Flow<List<QuestStepWithProgress>> = guideDao.observeSteps(questId)
    fun bosses(): Flow<List<BossWithProgress>> = guideDao.observeBosses()
    fun items(): Flow<List<ItemWithProgress>> = guideDao.observeItems()
    fun builds(): Flow<List<CharacterBuildEntity>> = guideDao.observeBuilds()

    suspend fun setStepCompleted(stepId: Int, completed: Boolean) =
        progressDao.upsert(ProgressEntity(ProgressType.QUEST_STEP, stepId, completed))

    suspend fun setQuestCompleted(questId: Int, completed: Boolean) {
        val now = System.currentTimeMillis()
        val rows = guideDao.stepIdsForQuest(questId)
            .map { ProgressEntity(ProgressType.QUEST_STEP, it, completed, now) }
        progressDao.upsertAll(rows)
    }

    suspend fun setBossDefeated(bossId: Int, defeated: Boolean) =
        progressDao.upsert(ProgressEntity(ProgressType.BOSS, bossId, defeated))

    suspend fun setItemCollected(itemId: Int, collected: Boolean) =
        progressDao.upsert(ProgressEntity(ProgressType.ITEM, itemId, collected))

    suspend fun resetProgress() = progressDao.clearAll()

    /** Writes [SeedData] when the installed content is older than the bundled version. */
    suspend fun seedIfNeeded() {
        if (prefs.getInt(KEY_SEED_VERSION, 0) >= SeedData.VERSION) return
        database.withTransaction {
            guideDao.upsertChapters(SeedData.chapters)
            guideDao.upsertQuests(SeedData.quests)
            guideDao.upsertSteps(SeedData.steps)
            guideDao.upsertBosses(SeedData.bosses)
            guideDao.upsertItems(SeedData.items)
            guideDao.upsertBuilds(SeedData.builds)
        }
        prefs.edit { putInt(KEY_SEED_VERSION, SeedData.VERSION) }
    }

    private companion object {
        const val KEY_SEED_VERSION = "seed_version"
    }
}

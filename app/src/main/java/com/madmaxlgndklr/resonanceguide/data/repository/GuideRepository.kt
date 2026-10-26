package com.madmaxlgndklr.resonanceguide.data.repository

import com.madmaxlgndklr.resonanceguide.data.local.ChapterEntity
import com.madmaxlgndklr.resonanceguide.data.local.ChapterSummary
import com.madmaxlgndklr.resonanceguide.data.local.EnemyWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.EquipmentWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.GuideDatabase
import com.madmaxlgndklr.resonanceguide.data.local.GuideSeeder
import com.madmaxlgndklr.resonanceguide.data.local.ItemWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.ObjectiveWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.OblivionTierEntity
import com.madmaxlgndklr.resonanceguide.data.local.OptionalBossWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.ProgressEntity
import com.madmaxlgndklr.resonanceguide.data.local.ProgressType
import com.madmaxlgndklr.resonanceguide.data.local.SeedResult
import com.madmaxlgndklr.resonanceguide.data.local.ShardWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.SideQuestWithProgress
import com.madmaxlgndklr.resonanceguide.data.local.VisionSkillEntity
import com.madmaxlgndklr.resonanceguide.data.local.VisionWithProgress
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth over the guide database.
 *
 * Every write goes through a `setX` call that upserts one [ProgressEntity]
 * keyed by the dataset's own id, so toggling a checkbox never rewrites content.
 */
class GuideRepository(
    private val database: GuideDatabase,
    private val seeder: GuideSeeder,
) {
    private val guideDao = database.guideDao()
    private val progressDao = database.progressDao()

    // -- content ------------------------------------------------------------

    fun chapterSummaries(): Flow<List<ChapterSummary>> = guideDao.observeChapterSummaries()
    fun chapter(chapterId: String): Flow<ChapterEntity?> = guideDao.observeChapter(chapterId)
    fun objectives(chapterId: String): Flow<List<ObjectiveWithProgress>> =
        guideDao.observeObjectives(chapterId)

    fun sideQuests(chapterNumber: Int): Flow<List<SideQuestWithProgress>> =
        guideDao.observeSideQuestsForChapter(chapterNumber)

    fun shards(chapterNumber: Int): Flow<List<ShardWithProgress>> =
        guideDao.observeShardsForChapter(chapterNumber)

    fun allShards(): Flow<List<ShardWithProgress>> = guideDao.observeAllShards()
    fun enemies(): Flow<List<EnemyWithProgress>> = guideDao.observeEnemies()
    fun optionalBosses(): Flow<List<OptionalBossWithProgress>> =
        guideDao.observeOptionalBosses()

    fun visions(): Flow<List<VisionWithProgress>> = guideDao.observeVisions()
    fun visionSkills(): Flow<List<VisionSkillEntity>> = guideDao.observeAllVisionSkills()
    fun equipment(): Flow<List<EquipmentWithProgress>> = guideDao.observeEquipment()
    fun items(): Flow<List<ItemWithProgress>> = guideDao.observeItems()
    fun oblivionTiers(): Flow<List<OblivionTierEntity>> = guideDao.observeOblivionTiers()

    // -- progress -----------------------------------------------------------

    suspend fun setObjectiveCompleted(objectiveId: String, completed: Boolean) =
        set(ProgressType.OBJECTIVE, objectiveId, completed)

    suspend fun setChapterObjectivesCompleted(chapterId: String, completed: Boolean) {
        val now = System.currentTimeMillis()
        progressDao.upsertAll(
            guideDao.objectiveIds(chapterId).map {
                ProgressEntity(ProgressType.OBJECTIVE, it, completed, now)
            },
        )
    }

    suspend fun setSideQuestCompleted(questId: String, completed: Boolean) =
        set(ProgressType.SIDE_QUEST, questId, completed)

    suspend fun setEnemyDefeated(enemyId: String, defeated: Boolean) =
        set(ProgressType.ENEMY, enemyId, defeated)

    suspend fun setOptionalBossDefeated(bossId: String, defeated: Boolean) =
        set(ProgressType.OPTIONAL_BOSS, bossId, defeated)

    suspend fun setVisionUnlocked(visionId: String, unlocked: Boolean) =
        set(ProgressType.VISION, visionId, unlocked)

    suspend fun setEquipmentCollected(equipmentId: String, collected: Boolean) =
        set(ProgressType.EQUIPMENT, equipmentId, collected)

    suspend fun setItemCollected(itemId: String, collected: Boolean) =
        set(ProgressType.ITEM, itemId, collected)

    suspend fun setShardCollected(shardId: String, collected: Boolean) =
        set(ProgressType.SHARD, shardId, collected)

    suspend fun resetProgress() = progressDao.clearAll()

    private suspend fun set(type: String, entityId: String, completed: Boolean) =
        progressDao.upsert(ProgressEntity(type, entityId, completed))

    // -- content loading ----------------------------------------------------

    /** Loads the bundled dataset into Room if the asset is newer than the DB. */
    suspend fun seedIfNeeded(): SeedResult = seeder.seedIfNeeded()
}

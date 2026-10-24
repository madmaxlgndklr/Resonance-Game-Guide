package com.madmaxlgndklr.resonanceguide.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GuideDao {

    @Query("SELECT * FROM chapters ORDER BY number")
    fun observeChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    fun observeChapter(chapterId: Int): Flow<ChapterEntity?>

    @Query(
        """
        SELECT q.*,
            (SELECT COUNT(*) FROM quest_steps s WHERE s.questId = q.id) AS totalSteps,
            (SELECT COUNT(*) FROM quest_steps s
                JOIN progress p ON p.type = 'QUEST_STEP' AND p.entityId = s.id AND p.completed = 1
                WHERE s.questId = q.id) AS completedSteps
        FROM quests q
        ORDER BY q.chapterId, q.sortOrder
        """,
    )
    fun observeQuestSummaries(): Flow<List<QuestSummary>>

    @Query("SELECT * FROM quests WHERE id = :questId")
    fun observeQuest(questId: Int): Flow<QuestEntity?>

    @Query(
        """
        SELECT s.*, COALESCE(p.completed, 0) AS completed
        FROM quest_steps s
        LEFT JOIN progress p ON p.type = 'QUEST_STEP' AND p.entityId = s.id
        WHERE s.questId = :questId
        ORDER BY s.stepNumber
        """,
    )
    fun observeSteps(questId: Int): Flow<List<QuestStepWithProgress>>

    @Query("SELECT id FROM quest_steps WHERE questId = :questId")
    suspend fun stepIdsForQuest(questId: Int): List<Int>

    @Query(
        """
        SELECT b.*, COALESCE(p.completed, 0) AS defeated
        FROM bosses b
        LEFT JOIN progress p ON p.type = 'BOSS' AND p.entityId = b.id
        ORDER BY COALESCE(b.chapterId, 9999), b.level
        """,
    )
    fun observeBosses(): Flow<List<BossWithProgress>>

    @Query(
        """
        SELECT i.*, COALESCE(p.completed, 0) AS collected
        FROM items i
        LEFT JOIN progress p ON p.type = 'ITEM' AND p.entityId = i.id
        ORDER BY i.category, i.name
        """,
    )
    fun observeItems(): Flow<List<ItemWithProgress>>

    @Query("SELECT * FROM character_builds ORDER BY characterName, buildName")
    fun observeBuilds(): Flow<List<CharacterBuildEntity>>

    // @Upsert updates rows in place rather than delete+insert, so refreshing a
    // chapter does not cascade-delete its quests and steps.
    @Upsert suspend fun upsertChapters(chapters: List<ChapterEntity>)
    @Upsert suspend fun upsertQuests(quests: List<QuestEntity>)
    @Upsert suspend fun upsertSteps(steps: List<QuestStepEntity>)
    @Upsert suspend fun upsertBosses(bosses: List<BossEntity>)
    @Upsert suspend fun upsertItems(items: List<ItemEntity>)
    @Upsert suspend fun upsertBuilds(builds: List<CharacterBuildEntity>)
}

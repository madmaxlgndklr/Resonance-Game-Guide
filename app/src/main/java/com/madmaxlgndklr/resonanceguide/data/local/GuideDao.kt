package com.madmaxlgndklr.resonanceguide.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GuideDao {

    @Query(
        """
        SELECT c.*,
            (SELECT COUNT(*) FROM objectives o WHERE o.chapterId = c.id) AS totalObjectives,
            (SELECT COUNT(*) FROM objectives o
                JOIN progress p ON p.type = 'OBJECTIVE' AND p.entityId = o.id AND p.completed = 1
                WHERE o.chapterId = c.id) AS completedObjectives,
            (SELECT COUNT(*) FROM tablet_shards s WHERE s.chapter = c.chapterNumber) AS totalShards,
            (SELECT COUNT(*) FROM tablet_shards s
                JOIN progress p ON p.type = 'SHARD' AND p.entityId = s.id AND p.completed = 1
                WHERE s.chapter = c.chapterNumber) AS collectedShards
        FROM chapters c
        ORDER BY c.chapterNumber
        """,
    )
    fun observeChapterSummaries(): Flow<List<ChapterSummary>>

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    fun observeChapter(chapterId: String): Flow<ChapterEntity?>

    @Query(
        """
        SELECT o.*, COALESCE(p.completed, 0) AS completed
        FROM objectives o
        LEFT JOIN progress p ON p.type = 'OBJECTIVE' AND p.entityId = o.id
        WHERE o.chapterId = :chapterId
        ORDER BY o.step
        """,
    )
    fun observeObjectives(chapterId: String): Flow<List<ObjectiveWithProgress>>

    @Query("SELECT id FROM objectives WHERE chapterId = :chapterId")
    suspend fun objectiveIds(chapterId: String): List<String>

    @Query(
        """
        SELECT q.*, COALESCE(p.completed, 0) AS completed
        FROM side_quests q
        LEFT JOIN progress p ON p.type = 'SIDE_QUEST' AND p.entityId = q.id
        WHERE q.chapterNumber = :chapterNumber
        ORDER BY q.title
        """,
    )
    fun observeSideQuestsForChapter(chapterNumber: Int): Flow<List<SideQuestWithProgress>>

    @Query(
        """
        SELECT s.*, COALESCE(p.completed, 0) AS collected
        FROM tablet_shards s
        LEFT JOIN progress p ON p.type = 'SHARD' AND p.entityId = s.id
        WHERE s.chapter = :chapterNumber
        ORDER BY s.number
        """,
    )
    fun observeShardsForChapter(chapterNumber: Int): Flow<List<ShardWithProgress>>

    @Query(
        """
        SELECT s.*, COALESCE(p.completed, 0) AS collected
        FROM tablet_shards s
        LEFT JOIN progress p ON p.type = 'SHARD' AND p.entityId = s.id
        ORDER BY s.number
        """,
    )
    fun observeAllShards(): Flow<List<ShardWithProgress>>

    @Query(
        """
        SELECT e.*, COALESCE(p.completed, 0) AS defeated
        FROM enemies e
        LEFT JOIN progress p ON p.type = 'ENEMY' AND p.entityId = e.id
        ORDER BY COALESCE(e.level, 9999), e.name
        """,
    )
    fun observeEnemies(): Flow<List<EnemyWithProgress>>

    @Query(
        """
        SELECT b.*, COALESCE(p.completed, 0) AS defeated
        FROM optional_bosses b
        LEFT JOIN progress p ON p.type = 'OPTIONAL_BOSS' AND p.entityId = b.id
        ORDER BY b.kind, COALESCE(b.level, 9999)
        """,
    )
    fun observeOptionalBosses(): Flow<List<OptionalBossWithProgress>>

    @Query(
        """
        SELECT v.*, COALESCE(p.completed, 0) AS unlocked
        FROM visions v
        LEFT JOIN progress p ON p.type = 'VISION' AND p.entityId = v.id
        ORDER BY COALESCE(v.chapter, 99), v.name
        """,
    )
    fun observeVisions(): Flow<List<VisionWithProgress>>

    @Query(
        """
        SELECT * FROM vision_skills
        WHERE visionId = :visionId
        ORDER BY COALESCE(masteryRank, 99), COALESCE(awakeningTier, 99), name
        """,
    )
    fun observeVisionSkills(visionId: String): Flow<List<VisionSkillEntity>>

    @Query("SELECT * FROM vision_skills ORDER BY visionId, COALESCE(masteryRank, 99)")
    fun observeAllVisionSkills(): Flow<List<VisionSkillEntity>>

    @Query(
        """
        SELECT e.*, COALESCE(p.completed, 0) AS collected
        FROM equipment e
        LEFT JOIN progress p ON p.type = 'EQUIPMENT' AND p.entityId = e.id
        ORDER BY e.slot, e.name
        """,
    )
    fun observeEquipment(): Flow<List<EquipmentWithProgress>>

    @Query(
        """
        SELECT i.*, COALESCE(p.completed, 0) AS collected
        FROM items i
        LEFT JOIN progress p ON p.type = 'ITEM' AND p.entityId = i.id
        ORDER BY i.category, i.name
        """,
    )
    fun observeItems(): Flow<List<ItemWithProgress>>

    @Query("SELECT * FROM oblivion_tiers ORDER BY totalShards")
    fun observeOblivionTiers(): Flow<List<OblivionTierEntity>>

    // Upserts update in place rather than delete-and-insert, so refreshing a
    // chapter does not cascade-delete its objectives.
    @Upsert suspend fun upsertChapters(rows: List<ChapterEntity>)
    @Upsert suspend fun upsertObjectives(rows: List<ObjectiveEntity>)
    @Upsert suspend fun upsertSideQuests(rows: List<SideQuestEntity>)
    @Upsert suspend fun upsertEnemies(rows: List<EnemyEntity>)
    @Upsert suspend fun upsertOptionalBosses(rows: List<OptionalBossEntity>)
    @Upsert suspend fun upsertVisions(rows: List<VisionEntity>)
    @Upsert suspend fun upsertVisionSkills(rows: List<VisionSkillEntity>)
    @Upsert suspend fun upsertEquipment(rows: List<EquipmentEntity>)
    @Upsert suspend fun upsertItems(rows: List<ItemEntity>)
    @Upsert suspend fun upsertShards(rows: List<TabletShardEntity>)
    @Upsert suspend fun upsertOblivionTiers(rows: List<OblivionTierEntity>)
}

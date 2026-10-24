package com.madmaxlgndklr.resonanceguide.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ChapterEntity::class,
        QuestEntity::class,
        QuestStepEntity::class,
        BossEntity::class,
        ItemEntity::class,
        CharacterBuildEntity::class,
        ProgressEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GuideDatabase : RoomDatabase() {
    abstract fun guideDao(): GuideDao
    abstract fun progressDao(): ProgressDao

    companion object {
        private const val NAME = "resonance_guide.db"

        fun build(context: Context): GuideDatabase =
            Room.databaseBuilder(context.applicationContext, GuideDatabase::class.java, NAME)
                // Add real Migrations when the schema changes: falling back to a
                // destructive migration would wipe the user's progress table.
                .build()
    }
}

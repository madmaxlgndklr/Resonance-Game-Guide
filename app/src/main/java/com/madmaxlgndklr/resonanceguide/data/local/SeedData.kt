package com.madmaxlgndklr.resonanceguide.data.local

/**
 * Bundled guide content written into Room on first launch.
 *
 * NOTE: everything here is placeholder sample content that exercises the
 * schema and UI. Replace it with verified guide data, then bump [VERSION] so
 * installed copies refresh their content tables (progress is kept).
 */
object SeedData {
    const val VERSION = 1

    val chapters = listOf(
        ChapterEntity(1, 0, "Prologue: The Silent Bell", "Wake in the ruined chapel and learn the basics of resonance combat.", "maps/chapter_00.webp"),
        ChapterEntity(2, 1, "Chapter 1: Glasswind Steppe", "Cross the open plains to reach the first crystal spire.", "maps/chapter_01.webp"),
        ChapterEntity(3, 2, "Chapter 2: The Drowned Archive", "Descend into the flooded library beneath the capital.", "maps/chapter_02.webp"),
    )

    val quests = listOf(
        QuestEntity(101, 1, "An Echo in the Dark", "Escape the collapsing chapel.", isMainStory = true, sortOrder = 0),
        QuestEntity(102, 1, "Lost Hymnal", "Recover the keeper's hymnal from the crypt.", isMainStory = false, sortOrder = 1),
        QuestEntity(201, 2, "Road to the Spire", "Travel across the steppe to the first spire.", isMainStory = true, sortOrder = 0),
        QuestEntity(202, 2, "Windmill Troubles", "Help the farmers restart the old windmills.", isMainStory = false, sortOrder = 1),
        QuestEntity(301, 3, "Into the Archive", "Find a way below the capital's great library.", isMainStory = true, sortOrder = 0),
        QuestEntity(302, 3, "Ink and Water", "Collect the water-damaged ledgers for the archivist.", isMainStory = false, sortOrder = 1),
    )

    val steps = listOf(
        QuestStepEntity(1001, 101, 1, "Examine the cracked bell in the nave.", "Interact twice to trigger the tutorial."),
        QuestStepEntity(1002, 101, 2, "Defeat the Hollow Sentinel."),
        QuestStepEntity(1003, 101, 3, "Climb the bell tower and exit through the roof."),
        QuestStepEntity(1004, 102, 1, "Take the side stairs down to the crypt."),
        QuestStepEntity(1005, 102, 2, "Light the three braziers to open the sealed door.", "Order: left, right, centre."),
        QuestStepEntity(1006, 102, 3, "Return the hymnal to the keeper."),
        QuestStepEntity(2001, 201, 1, "Leave the village by the north gate."),
        QuestStepEntity(2002, 201, 2, "Rest at the waystone camp."),
        QuestStepEntity(2003, 201, 3, "Defeat the Glasswind Roc at the spire's base."),
        QuestStepEntity(2004, 201, 4, "Attune to the first spire."),
        QuestStepEntity(2005, 202, 1, "Speak to the farmer by the eastern fields."),
        QuestStepEntity(2006, 202, 2, "Clear the pests from the three windmills."),
        QuestStepEntity(2007, 202, 3, "Report back for your reward."),
        QuestStepEntity(3001, 301, 1, "Obtain the archive key from the curator."),
        QuestStepEntity(3002, 301, 2, "Drain the first flooded hall using the sluice lever."),
        QuestStepEntity(3003, 301, 3, "Defeat the Tidebound Scribe."),
        QuestStepEntity(3004, 302, 1, "Find all five ledgers in the lower stacks.", "One is behind the collapsed shelf on the west side."),
        QuestStepEntity(3005, 302, 2, "Deliver the ledgers to the archivist."),
    )

    val bosses = listOf(
        BossEntity(1, "Hollow Sentinel", 1, "Ruined Chapel", 5, 4_200, listOf("Lightning"), listOf("Earth"), listOf("Sentinel Core"), "Dodge the overhead slam, then punish the stagger window.", "sprites/bosses/hollow_sentinel.webp"),
        BossEntity(2, "Glasswind Roc", 2, "Spire Base", 14, 28_500, listOf("Earth", "Pierce"), listOf("Wind"), listOf("Roc Plume", "Wind Shard x3"), "Ground it with pierce attacks when it begins to hover.", "sprites/bosses/glasswind_roc.webp"),
        BossEntity(3, "Steppe Marauder", 2, "Eastern Fields", 12, 16_000, listOf("Fire"), emptyList(), listOf("Marauder's Axe"), "Optional. Kill the two adds first.", "sprites/bosses/steppe_marauder.webp"),
        BossEntity(4, "Tidebound Scribe", 3, "Drowned Archive", 22, 61_000, listOf("Lightning", "Holy"), listOf("Water", "Ice"), listOf("Scribe's Quill", "Deep Pearl"), "Destroy the floating tomes to stop its heal.", "sprites/bosses/tidebound_scribe.webp"),
    )

    val items = listOf(
        ItemEntity(1, "Keeper's Hymnal", "Key Item", "A worn book of hymns.", "Chapel crypt, behind the sealed door", 1, null),
        ItemEntity(2, "Potion", "Consumable", "Restores a small amount of HP.", "Sold everywhere; chests throughout", null, null),
        ItemEntity(3, "Ether", "Consumable", "Restores a small amount of MP.", "Chapel nave chest", 1, null),
        ItemEntity(4, "Wind Shard", "Material", "Crafting material from wind-aligned foes.", "Dropped by Glasswind Roc", 2, null),
        ItemEntity(5, "Sentinel Core", "Material", "A humming core of old stone.", "Dropped by Hollow Sentinel", 1, null),
        ItemEntity(6, "Iron Longsword", "Weapon", "A dependable starter blade.", "Village smithy", 2, null),
        ItemEntity(7, "Marauder's Axe", "Weapon", "Heavy axe with high break damage.", "Dropped by Steppe Marauder", 2, null),
        ItemEntity(8, "Archivist's Robe", "Armor", "Boosts magic defence.", "Reward for 'Ink and Water'", 3, null),
        ItemEntity(9, "Deep Pearl", "Accessory", "Grants water resistance.", "Dropped by Tidebound Scribe", 3, null),
        ItemEntity(10, "Archive Key", "Key Item", "Opens the lower archive.", "Given by the curator", 3, null),
    )

    val builds = listOf(
        CharacterBuildEntity(1, "Aren", "Stagger Breaker", "Attacker", "Front-loads break damage to open stagger windows for the party.", listOf("Rending Strike", "Shatterpoint", "Second Wind"), listOf("Marauder's Axe", "Iron Gauntlets"), "sprites/characters/aren.webp"),
        CharacterBuildEntity(2, "Lyse", "Chorus Healer", "Support", "Keeps the party topped up and cleanses debuffs.", listOf("Mending Chime", "Purify", "Resonant Ward"), listOf("Oak Staff", "Archivist's Robe"), "sprites/characters/lyse.webp"),
        CharacterBuildEntity(3, "Lyse", "Storm Caller", "Caster", "Swaps healing for lightning damage against water-aligned bosses.", listOf("Arc Bolt", "Thunderbell", "Focus"), listOf("Oak Staff", "Deep Pearl"), "sprites/characters/lyse.webp"),
        CharacterBuildEntity(4, "Doran", "Bulwark", "Defender", "Draws aggro and mitigates heavy hits.", listOf("Taunt", "Iron Will", "Counterstance"), listOf("Tower Shield", "Sentinel Core charm"), "sprites/characters/doran.webp"),
    )
}

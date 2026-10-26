"""Pydantic models for the shipped guide dataset.

Every record the app can show is validated against these models before it is
written, so a parser regression fails the build instead of shipping a broken
asset. Fields that a source has not confirmed are ``None`` -- never a guess.
"""

from __future__ import annotations

import re
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator

ID_PATTERN = re.compile(r"^[a-z][a-z0-9_]*$")


class Base(BaseModel):
    model_config = ConfigDict(extra="forbid", populate_by_name=True)


class Identified(Base):
    """Anything the app can key checklist state by."""

    id: str

    @field_validator("id")
    @classmethod
    def _snake_case(cls, value: str) -> str:
        if not ID_PATTERN.match(value):
            raise ValueError(f"id {value!r} is not stable snake_case")
        return value


class SourceRef(Base):
    sourceUrl: str | None = None
    notes: str | None = None


class Objective(Identified, SourceRef):
    step: int
    text: str
    locationId: str | None = None
    bossId: str | None = None
    chestsInArea: list[str] = Field(default_factory=list)
    dungeonPuzzleSolution: str | None = None


class Chapter(Identified, SourceRef):
    chapterNumber: int
    title: str
    region: str | None = None
    recommendedLevel: str | None = None
    firstObjective: str | None = None
    pointOfNoReturn: str | None = None
    storyBosses: list[str] = Field(default_factory=list)
    optionalFights: list[str] = Field(default_factory=list)
    notableCollectibles: list[str] = Field(default_factory=list)
    objectives: list[Objective] = Field(default_factory=list)


class ForgeWeapon(Base):
    model_config = ConfigDict(extra="allow")


class Hero(Identified, SourceRef):
    name: str
    baseRole: str | None = None
    weaponType: str | None = None
    damageType: str | None = None
    innateElements: list[str] = Field(default_factory=list)
    recommendedVisions: list[str] = Field(default_factory=list)
    sameDamageTypeVisions: list[str] = Field(default_factory=list)
    characterSubQuest: str | None = None
    aileenForgeWeapon: dict[str, Any] | None = None
    legendaryArm: dict[str, Any] | None = None


class VisionSkill(Identified, SourceRef):
    name: str
    category: str | None = None
    skillType: str | None = None
    description: str | None = None
    masteryRank: int | None = Field(default=None, ge=1, le=9)
    apCost: int | None = Field(default=None, ge=0)
    awakeningTier: int | None = Field(default=None, ge=0, le=3)


class Vision(Identified, SourceRef):
    name: str
    origin: str | None = None
    role: str | None = None
    resonanceSkill: str | None = None
    damageType: str | None = None
    elements: list[str] = Field(default_factory=list)
    chapter: int | None = None
    method: str | None = None
    unlockRoute: str | None = None
    skills: list[VisionSkill] = Field(default_factory=list)


class Esper(Identified, SourceRef):
    name: str
    element: str | None = None
    location: str | None = None
    unlockMethod: str | None = None
    signatureMove: str | None = None
    trialLevel: int | None = None
    hpNormal: int | None = None
    staggerGauge: int | None = None
    weaknesses: list[str] = Field(default_factory=list)
    resistances: list[str] = Field(default_factory=list)


class SideQuest(Identified, SourceRef):
    title: str
    chapter: int | None = None
    requester: str | None = None
    location: str | None = None
    region: str | None = None
    prerequisites: str | None = None
    rewards: list[str] = Field(default_factory=list)
    objectives: list[str] = Field(default_factory=list)


class CaveMaster(Identified, SourceRef):
    name: str
    level: int | None = None
    hp: int | None = None
    staggerGauge: int | None = None
    weaknesses: list[str] = Field(default_factory=list)
    devastatingAttacks: list[str] = Field(default_factory=list)
    location: str | None = None
    rewards: list[str] = Field(default_factory=list)
    strategy: str | None = None


class ColosseumBattle(Identified, SourceRef):
    battle: int
    recLevel: int | None = None
    name: str | None = None
    enemies: str | None = None
    firstWinReward: str | None = None
    repeatReward: str | None = None


class ColosseumLeague(Identified, SourceRef):
    name: str
    recommendedLevels: str | None = None
    repeatReward: str | None = None
    clearCertificate: str | None = None
    battles: list[ColosseumBattle] = Field(default_factory=list)


class ChamberTrial(Identified, SourceRef):
    guardian: str
    trialName: str | None = None
    levels: str | None = None
    reward: str | None = None
    formsSummary: str | None = None


class Adamantite(Identified, SourceRef):
    source: str
    requirement: str | None = None


class LootEntry(Base):
    name: str
    dropRate: float | None = Field(default=None, ge=0, le=100)


class Equipment(Identified, SourceRef):
    name: str
    slot: Literal["weapon", "armor", "accessory"]
    equipType: str | None = None
    catalogNumber: int | None = None
    physAtk: int | None = None
    magAtk: int | None = None
    defense: int | None = Field(default=None, alias="def")
    magDef: int | None = None
    speed: int | None = None
    sanctity: int | None = None
    effect: str | None = None
    acquisitionMethod: str | None = None
    stealSource: str | None = None
    sellPrice: int | None = None


class Item(Identified, SourceRef):
    name: str
    category: str | None = None
    description: str | None = None
    acquisitionMethod: str | None = None
    sellPrice: int | None = None


class Enemy(Identified, SourceRef):
    name: str
    enemyType: str | None = None
    level: int | None = None
    hp: int | None = None
    staggerGauge: int | None = None
    weaknesses: list[str] = Field(default_factory=list)
    resistances: list[str] = Field(default_factory=list)
    drops: list[LootEntry] = Field(default_factory=list)
    stealables: list[LootEntry] = Field(default_factory=list)
    location: str | None = None
    exp: int | None = None
    gil: int | None = None
    traits: str | None = None
    stats: dict[str, int | None] = Field(default_factory=dict)


class TabletShard(Identified, SourceRef):
    number: int = Field(ge=1, le=99)
    chapter: int | None = None
    region: str | None = None
    subArea: str | None = None
    exactLocationDescription: str | None = None
    missableWarning: str | None = None


class OblivionTier(Identified, SourceRef):
    chamber: str
    totalShards: int
    rewardName: str
    rewardDescription: str | None = None
    quantity: int | None = None


class Collectibles(Base):
    tabletShards: list[TabletShard] = Field(default_factory=list)
    oblivionPalaceTiers: list[OblivionTier] = Field(default_factory=list)


class DialogueChoice(Identified, SourceRef):
    speaker: str
    prompt: str | None = None
    choice: str | None = None
    bonus: str | None = None
    chapter: int | None = None


class Mechanic(Identified, SourceRef):
    name: str
    summary: str


class Asset(Identified, Base):
    title: str
    url: str
    sourceUrl: str | None = None
    localPath: str | None = None
    thumbnailPath: str | None = None
    licenseStatus: str
    bundled: bool = False


class AssetsManifest(Base):
    pressKitUrl: str | None = None
    licenceNote: str | None = None
    entries: list[Asset] = Field(default_factory=list)


class Coverage(Base):
    """What this build actually contains, and what it could not reach."""

    expected: dict[str, int] = Field(default_factory=dict)
    actual: dict[str, int] = Field(default_factory=dict)
    gaps: list[str] = Field(default_factory=list)


class SourceRecord(Base):
    key: str
    host: str
    status: str
    covers: str
    note: str
    used: bool


class Meta(Base):
    model_config = ConfigDict(extra="allow")

    gameTitle: str
    schemaVersion: str
    datasetVersion: str
    lastUpdated: str
    generatedBy: str
    coverage: Coverage
    sources: list[SourceRecord] = Field(default_factory=list)


class Guide(Base):
    meta: Meta
    combatMechanics: list[Mechanic] = Field(default_factory=list)
    chapters: list[Chapter] = Field(default_factory=list)
    heroes: list[Hero] = Field(default_factory=list)
    visions: list[Vision] = Field(default_factory=list)
    espers: list[Esper] = Field(default_factory=list)
    sideQuests: list[SideQuest] = Field(default_factory=list)
    caveMasters: list[CaveMaster] = Field(default_factory=list)
    colosseum: list[ColosseumLeague] = Field(default_factory=list)
    chamberOfArms: list[ChamberTrial] = Field(default_factory=list)
    adamantiteLocations: list[Adamantite] = Field(default_factory=list)
    equipment: list[Equipment] = Field(default_factory=list)
    items: list[Item] = Field(default_factory=list)
    bestiary: list[Enemy] = Field(default_factory=list)
    skills: list[VisionSkill] = Field(default_factory=list)
    collectibles: Collectibles = Field(default_factory=Collectibles)
    dialogueChoices: list[DialogueChoice] = Field(default_factory=list)
    assetsManifest: AssetsManifest = Field(default_factory=AssetsManifest)

package com.dailytrials;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;

public final class ModContent {
    public static final String ID = "dailytrials";

    public static Identifier id(String path) { return new Identifier(ID, path); }

    public static final Block QUEST_BOARD = Registry.register(Registries.BLOCK, id("quest_board"),
            new QuestBoardBlock(FabricBlockSettings.create().mapColor(MapColor.OAK_TAN).strength(2.5f).sounds(BlockSoundGroup.WOOD)));

    public static final Item QUEST_BOARD_ITEM = Registry.register(Registries.ITEM, id("quest_board"),
            new BlockItem(QUEST_BOARD, new FabricItemSettings()));

    public static final EntityType<SombraEntity> SOMBRA = Registry.register(Registries.ENTITY_TYPE, id("sombra"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, SombraEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .fireImmune()
                    .build());

    public static final Item SOMBRA_EGG = Registry.register(Registries.ITEM, id("sombra_spawn_egg"),
            new SpawnEggItem(SOMBRA, 0x1b1b2f, 0x7f3fbf, new FabricItemSettings()));

    public static void init() {
        FabricDefaultAttributeRegistry.register(SOMBRA, SombraEntity.createAttributes());
        SpawnRestriction.register(SOMBRA, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, SOMBRA, 8, 1, 2);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(e -> e.add(QUEST_BOARD_ITEM));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(e -> e.add(SOMBRA_EGG));
    }
}

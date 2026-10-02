package com.untamedrealms.arsenal;

import com.untamedrealms.arsenal.block.Station;
import com.untamedrealms.arsenal.block.StationBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ArsenalBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(UntamedArsenal.MODID);

    public static final DeferredBlock<DropExperienceBlock> ORICHALCUM_ORE = BLOCKS.register("orichalcum_ore", () -> ore(3.0f, false));
    public static final DeferredBlock<DropExperienceBlock> MOONSTONE_ORE = BLOCKS.register("moonstone_ore", () -> ore(3.0f, false));
    public static final DeferredBlock<DropExperienceBlock> MALACHITE_ORE = BLOCKS.register("malachite_ore", () -> ore(3.5f, false));
    public static final DeferredBlock<DropExperienceBlock> EBONY_ORE = BLOCKS.register("ebony_ore", () -> ore(4.5f, false));
    // deepslate variants (vanilla deepslate ores are 1.5 harder)
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_ORICHALCUM_ORE = BLOCKS.register("deepslate_orichalcum_ore", () -> ore(4.5f, true));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_MOONSTONE_ORE = BLOCKS.register("deepslate_moonstone_ore", () -> ore(4.5f, true));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_MALACHITE_ORE = BLOCKS.register("deepslate_malachite_ore", () -> ore(5.0f, true));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_EBONY_ORE = BLOCKS.register("deepslate_ebony_ore", () -> ore(6.0f, true));

    public static final DeferredBlock<StationBlock> FORGE = BLOCKS.register("forge", () -> new StationBlock(Station.FORGE,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5f).sound(SoundType.STONE).requiresCorrectToolForDrops()
                    .lightLevel(s -> 11).noOcclusion()));
    public static final DeferredBlock<StationBlock> TANNING_RACK = BLOCKS.register("tanning_rack", () -> new StationBlock(Station.TANNING_RACK,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2f).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final DeferredBlock<StationBlock> WORKBENCH = BLOCKS.register("workbench", () -> new StationBlock(Station.WORKBENCH,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));

    private ArsenalBlocks() {}

    private static DropExperienceBlock ore(float strength, boolean deepslate) {
        return new DropExperienceBlock(UniformInt.of(1, 4), BlockBehaviour.Properties.of()
                .mapColor(deepslate ? MapColor.DEEPSLATE : MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(strength, 3.0f).sound(deepslate ? SoundType.DEEPSLATE : SoundType.STONE));
    }
}

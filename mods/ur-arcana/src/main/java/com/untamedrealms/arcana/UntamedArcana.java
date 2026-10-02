package com.untamedrealms.arcana;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.untamedrealms.arcana.block.ArcanaStationBlock;
import com.untamedrealms.arcana.data.AlchemyEffect;
import com.untamedrealms.arcana.data.ArcanaKnowledge;
import com.untamedrealms.arcana.data.IngredientDef;
import com.untamedrealms.arcana.effect.SoulTrappedEffect;
import com.untamedrealms.arcana.effect.VitalEffect;
import com.untamedrealms.arcana.item.SoulGemItem;
import com.untamedrealms.core.data.DataRegistry;
import com.untamedrealms.core.registry.CoreItems;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Untamed Realms: Arcana. Skyrim's alchemy - ingredients with four hidden effects, learned by tasting
 * and brewing at the alchemy lab - and enchanting - learn enchantments by disenchanting gear at the
 * arcane enchanter, then put them on your own gear with soul gems filled by Soul Trap.
 */
@Mod(UntamedArcana.MODID)
public final class UntamedArcana {
    public static final String MODID = "urarcana";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DataRegistry<AlchemyEffect> EFFECTS = DataRegistry.create(id("alchemy_effects"), "urarcana/alchemy_effects", AlchemyEffect.CODEC, true);
    public static final DataRegistry<IngredientDef> INGREDIENTS = DataRegistry.create(id("ingredients"), "urarcana/ingredients", IngredientDef.CODEC, true);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);
    public static final Supplier<AttachmentType<ArcanaKnowledge>> KNOWLEDGE = ATTACHMENTS.register("knowledge",
            () -> AttachmentType.serializable(ArcanaKnowledge::new).copyOnDeath().build());

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    /** The soul a soul gem holds: 0 empty, 1 petty .. 5 grand. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SOUL = COMPONENTS.registerComponentType("soul",
            b -> b.persistent(Codec.intRange(0, 5)).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MODID);
    public static final DeferredHolder<MobEffect, VitalEffect> RESTORE_MAGICKA = MOB_EFFECTS.register("restore_magicka", () -> new VitalEffect(true, true, 0x3C7FE0));
    public static final DeferredHolder<MobEffect, VitalEffect> RESTORE_STAMINA = MOB_EFFECTS.register("restore_stamina", () -> new VitalEffect(false, true, 0x4FC24F));
    public static final DeferredHolder<MobEffect, VitalEffect> RAVAGE_MAGICKA = MOB_EFFECTS.register("ravage_magicka", () -> new VitalEffect(true, false, 0x1D2F6B));
    public static final DeferredHolder<MobEffect, VitalEffect> RAVAGE_STAMINA = MOB_EFFECTS.register("ravage_stamina", () -> new VitalEffect(false, false, 0x2F5A1D));
    public static final DeferredHolder<MobEffect, SoulTrappedEffect> SOUL_TRAPPED = MOB_EFFECTS.register("soul_trapped", SoulTrappedEffect::new);

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredBlock<ArcanaStationBlock> ALCHEMY_LAB = BLOCKS.register("alchemy_lab", () -> new ArcanaStationBlock(false,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final DeferredBlock<ArcanaStationBlock> ARCANE_ENCHANTER = BLOCKS.register("arcane_enchanter", () -> new ArcanaStationBlock(true,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(5f, 1200f).sound(SoundType.STONE).requiresCorrectToolForDrops()
                    .lightLevel(s -> 7).noOcclusion()));

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredItem<BlockItem> ALCHEMY_LAB_ITEM = ITEMS.registerSimpleBlockItem(ALCHEMY_LAB);
    public static final DeferredItem<BlockItem> ARCANE_ENCHANTER_ITEM = ITEMS.registerSimpleBlockItem(ARCANE_ENCHANTER);
    /** Index 0 = petty .. 4 = grand. */
    public static final List<DeferredItem<SoulGemItem>> SOUL_GEMS = new ArrayList<>();
    /** Our own alchemy ingredients; most ingredients are vanilla items (see the ingredient data). */
    public static final DeferredItem<Item> FIRE_SALTS = ITEMS.registerSimpleItem("fire_salts");
    public static final DeferredItem<Item> FROST_SALTS = ITEMS.registerSimpleItem("frost_salts");
    public static final DeferredItem<Item> VOID_SALTS = ITEMS.registerSimpleItem("void_salts", new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> NIRNROOT = ITEMS.registerSimpleItem("nirnroot", new Item.Properties().rarity(Rarity.UNCOMMON));

    static {
        Rarity[] rarity = {Rarity.COMMON, Rarity.COMMON, Rarity.UNCOMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC};
        for (int size = 1; size <= 5; size++) {
            int capacity = size;
            SOUL_GEMS.add(ITEMS.register(SoulGemItem.SIZES[size] + "_soul_gem",
                    () -> new SoulGemItem(capacity, new Item.Properties().stacksTo(1).rarity(rarity[capacity]))));
        }
    }

    public UntamedArcana(IEventBus modBus, ModContainer container) {
        ATTACHMENTS.register(modBus);
        COMPONENTS.register(modBus);
        MOB_EFFECTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, event -> {
            if (event.getTabKey().equals(CoreItems.TAB_KEY)) ITEMS.getEntries().forEach(item -> event.accept(item.get()));
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}

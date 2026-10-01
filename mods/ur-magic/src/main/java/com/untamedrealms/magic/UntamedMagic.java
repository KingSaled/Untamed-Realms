package com.untamedrealms.magic;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.untamedrealms.core.data.DataRegistry;
import com.untamedrealms.core.registry.CoreItems;
import com.untamedrealms.magic.data.SpellBook;
import com.untamedrealms.magic.data.SpellDef;
import com.untamedrealms.magic.entity.SpellProjectile;
import com.untamedrealms.magic.item.BoundWeaponItem;
import com.untamedrealms.magic.item.SpellTomeItem;
import com.untamedrealms.magic.item.VitalPotionItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Untamed Realms: Magic. Spells are data ({@link SpellDef}) interpreted by a handful of coded kinds;
 * they are learned from tomes, readied into five quick slots and cast with Magicka. Casting trains
 * the matching school skill.
 */
@Mod(UntamedMagic.MODID)
public final class UntamedMagic {
    public static final String MODID = "urmagic";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DataRegistry<SpellDef> SPELLS = DataRegistry.create(id("spells"), "urmagic/spells", SpellDef.CODEC, true);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);
    public static final Supplier<AttachmentType<SpellBook>> SPELLBOOK = ATTACHMENTS.register("spellbook",
            () -> AttachmentType.serializable(SpellBook::new).copyOnDeath().build());

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> SPELL = COMPONENTS.registerComponentType("spell",
            b -> b.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> EXPIRES = COMPONENTS.registerComponentType("expires",
            b -> b.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredItem<SpellTomeItem> SPELL_TOME = ITEMS.register("spell_tome",
            () -> new SpellTomeItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<BoundWeaponItem> BOUND_SWORD = ITEMS.register("bound_sword",
            () -> new BoundWeaponItem(Tiers.DIAMOND, new Item.Properties().rarity(Rarity.RARE)
                    .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 4, -2.2f))));
    public static final DeferredItem<VitalPotionItem> MAGICKA_POTION = ITEMS.register("magicka_potion",
            () -> new VitalPotionItem(true, 60f, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<VitalPotionItem> STAMINA_POTION = ITEMS.register("stamina_potion",
            () -> new VitalPotionItem(false, 60f, new Item.Properties().stacksTo(16)));

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<SpellProjectile>> SPELL_PROJECTILE = ENTITIES.register("spell_projectile",
            () -> EntityType.Builder.<SpellProjectile>of(SpellProjectile::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(8).updateInterval(2).build("spell_projectile"));

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MODID);
    /** Illusion: much harder for monsters to notice you. */
    public static final DeferredHolder<MobEffect, MobEffect> MUFFLED = EFFECTS.register("muffled",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x5A7A9A) {});

    public UntamedMagic(IEventBus modBus, ModContainer container) {
        ATTACHMENTS.register(modBus);
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        EFFECTS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, MagicConfig.SPEC);
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, event -> {
            if (!event.getTabKey().equals(CoreItems.TAB_KEY)) return;
            event.accept(MAGICKA_POTION.get());
            event.accept(STAMINA_POTION.get());
            SPELLS.entries().keySet().stream().sorted().forEach(id -> event.accept(SpellTomeItem.of(id)));
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}

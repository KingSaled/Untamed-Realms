package com.untamedrealms.core.registry;

import com.untamedrealms.core.UntamedCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Player attributes added by the suite. Using real attributes (instead of plain numbers) means
 * any mod - perks, gear, potions, other mods' items - can modify them through attribute modifiers.
 */
@EventBusSubscriber(modid = UntamedCore.MODID)
public final class CoreAttributes {
    public static final DeferredRegister<Attribute> REGISTER = DeferredRegister.create(Registries.ATTRIBUTE, UntamedCore.MODID);

    public static final DeferredHolder<Attribute, Attribute> MAX_MAGICKA = REGISTER.register("max_magicka",
            () -> new RangedAttribute("attribute.urcore.max_magicka", 100.0, 0.0, 100_000.0).setSyncable(true));
    public static final DeferredHolder<Attribute, Attribute> MAX_STAMINA = REGISTER.register("max_stamina",
            () -> new RangedAttribute("attribute.urcore.max_stamina", 100.0, 0.0, 100_000.0).setSyncable(true));
    /** Multiplier on magicka regeneration (1.0 = normal). */
    public static final DeferredHolder<Attribute, Attribute> MAGICKA_REGEN = REGISTER.register("magicka_regen",
            () -> new RangedAttribute("attribute.urcore.magicka_regen", 1.0, 0.0, 100.0).setSyncable(true));
    /** Multiplier on stamina regeneration (1.0 = normal). */
    public static final DeferredHolder<Attribute, Attribute> STAMINA_REGEN = REGISTER.register("stamina_regen",
            () -> new RangedAttribute("attribute.urcore.stamina_regen", 1.0, 0.0, 100.0).setSyncable(true));
    /** Multiplier on how much stamina actions cost (1.0 = normal). */
    public static final DeferredHolder<Attribute, Attribute> STAMINA_COST = REGISTER.register("stamina_cost",
            () -> new RangedAttribute("attribute.urcore.stamina_cost", 1.0, 0.0, 10.0).setSyncable(true));

    @SubscribeEvent
    public static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, MAX_MAGICKA);
        event.add(EntityType.PLAYER, MAX_STAMINA);
        event.add(EntityType.PLAYER, MAGICKA_REGEN);
        event.add(EntityType.PLAYER, STAMINA_REGEN);
        event.add(EntityType.PLAYER, STAMINA_COST);
    }

    private CoreAttributes() {}
}

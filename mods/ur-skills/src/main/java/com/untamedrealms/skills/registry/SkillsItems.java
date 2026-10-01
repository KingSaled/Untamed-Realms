package com.untamedrealms.skills.registry;

import com.untamedrealms.core.registry.CoreItems;
import com.untamedrealms.skills.UntamedSkills;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.item.SkillBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = UntamedSkills.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class SkillsItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(UntamedSkills.MODID);

    public static final DeferredItem<SkillBookItem> SKILL_BOOK = REGISTER.register("skill_book",
            () -> new SkillBookItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    @SubscribeEvent
    public static void onBuildTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CoreItems.TAB_KEY)) {
            for (Skill skill : Skill.VALUES) event.accept(SkillBookItem.of(SKILL_BOOK.get(), skill));
        }
    }

    private SkillsItems() {}
}

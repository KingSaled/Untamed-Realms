package com.untamedrealms.skills.client;

import com.untamedrealms.skills.SkillsClientConfig;
import com.untamedrealms.skills.UntamedSkills;
import com.untamedrealms.skills.api.Gear;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.data.RequirementSet;
import com.untamedrealms.skills.data.SkillsData;
import com.untamedrealms.skills.registry.SkillsComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

public final class SkillsClient {
    private SkillsClient() {}

    @EventBusSubscriber(modid = UntamedSkills.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(SkillsKeys.OPEN_SKILLS);
        }

        @SubscribeEvent
        public static void registerLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(UntamedSkills.id("xp_drops"), XpDropHud::render);
        }
    }

    @EventBusSubscriber(modid = UntamedSkills.MODID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            ClientSkills.applyToPlayer();
            XpDropHud.tick();
            Minecraft mc = Minecraft.getInstance();
            while (SkillsKeys.OPEN_SKILLS.consumeClick()) {
                if (mc.player != null && mc.screen == null) mc.setScreen(new SkillsScreen());
            }
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientSkills.reset();
            XpDropHud.clear();
        }

        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            List<Component> tooltip = event.getToolTip();

            Integer quality = stack.get(SkillsComponents.QUALITY.get());
            if (quality != null && quality > 0) {
                tooltip.add(1, Component.translatable("quality.urskills." + quality).withStyle(ChatFormatting.AQUA));
            }

            if (event.getEntity() == null || !SkillsClientConfig.SHOW_REQUIREMENT_TOOLTIPS.get()) return;
            boolean any = false;
            for (RequirementSet.Kind kind : new RequirementSet.Kind[]{RequirementSet.Kind.WEAPON, RequirementSet.Kind.TOOL, RequirementSet.Kind.ARMOR}) {
                for (RequirementSet.Requirement req : SkillsData.requirements(kind, stack)) {
                    boolean met = ClientSkills.data().level(req.skill()) >= req.level();
                    tooltip.add(Component.translatable("tooltip.urskills.requires." + kind.getSerializedName(), req.skill().displayName(), req.level())
                            .withStyle(met ? ChatFormatting.DARK_GREEN : ChatFormatting.RED));
                    any = true;
                }
            }
            if (!any) {
                Skill weapon = Gear.weaponSkill(stack);
                Skill armor = Gear.armorSkill(stack);
                Skill shown = weapon != null ? weapon : armor;
                if (shown != null) {
                    tooltip.add(Component.translatable("tooltip.urskills.skill", shown.displayName()).withStyle(ChatFormatting.DARK_GRAY));
                }
            } else {
                Skill armor = Gear.armorSkill(stack);
                if (armor != null) tooltip.add(Component.translatable("tooltip.urskills.skill", armor.displayName()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}

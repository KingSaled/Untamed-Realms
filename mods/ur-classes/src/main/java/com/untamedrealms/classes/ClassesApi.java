package com.untamedrealms.classes;

import com.untamedrealms.classes.data.Birthsign;
import com.untamedrealms.classes.data.ClassData;
import com.untamedrealms.classes.data.ClassDef;
import com.untamedrealms.classes.data.ClassesData;
import com.untamedrealms.classes.network.ClassesNetwork;
import com.untamedrealms.core.api.UR;
import com.untamedrealms.core.economy.WalletApi;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillsApi;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

import java.util.Map;

public final class ClassesApi {
    private ClassesApi() {}

    public static ClassData data(Player player) {
        return player.getData(UntamedClasses.CLASS_DATA);
    }

    public static boolean hasChosen(Player player) {
        return data(player).chosen();
    }

    /** Applies a class + birthsign choice. Returns an error message or null on success. */
    public static Component choose(ServerPlayer player, ResourceLocation classId, ResourceLocation birthsignId) {
        if (hasChosen(player)) return Component.translatable("message.urclasses.already_chosen");
        ClassDef def = ClassesData.CLASSES.getOrNull(classId);
        Birthsign sign = ClassesData.BIRTHSIGNS.getOrNull(birthsignId);
        if (def == null) return Component.translatable("message.urclasses.unknown_class");
        if (sign == null && !ClassesData.BIRTHSIGNS.entries().isEmpty()) return Component.translatable("message.urclasses.unknown_birthsign");

        data(player).set(classId, sign == null ? null : birthsignId);

        for (Map.Entry<Skill, Integer> entry : def.skills().entrySet()) {
            if (SkillsApi.level(player, entry.getKey()) < entry.getValue()) {
                SkillsApi.setLevel(player, entry.getKey(), entry.getValue());
            }
        }
        for (ItemStack stack : def.loadout().resolve(player.registryAccess())) {
            give(player, stack);
        }
        WalletApi.deposit(player, def.coins());
        SkillsApi.refresh(player);
        player.setHealth(player.getMaxHealth());

        UR.banner(player, Component.translatable("banner.urclasses.chosen", def.name()).withStyle(ChatFormatting.GOLD),
                sign == null ? Component.empty() : Component.translatable("banner.urclasses.born_under", sign.name()));
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.0f);
        ClassesNetwork.sync(player);
        return null;
    }

    /** Equips armor / shields into empty slots, everything else into the inventory (or drops it). */
    private static void give(ServerPlayer player, ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem armor) {
            EquipmentSlot slot = armor.getEquipmentSlot();
            if (player.getItemBySlot(slot).isEmpty()) {
                player.setItemSlot(slot, stack);
                return;
            }
        }
        if (stack.getItem() instanceof ShieldItem && player.getOffhandItem().isEmpty()) {
            player.setItemSlot(EquipmentSlot.OFFHAND, stack);
            return;
        }
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    /** Clears the choice so the player picks again (admin). Skills and items are kept. */
    public static void reset(ServerPlayer player) {
        data(player).set(null, null);
        SkillsApi.refresh(player);
        ClassesNetwork.sync(player);
        ClassesNetwork.openSelection(player);
    }
}

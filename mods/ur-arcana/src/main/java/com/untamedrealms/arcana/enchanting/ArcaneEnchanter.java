package com.untamedrealms.arcana.enchanting;

import com.untamedrealms.arcana.ArcanaApi;
import com.untamedrealms.arcana.UntamedArcana;
import com.untamedrealms.arcana.data.ArcanaKnowledge;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillsApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.Optional;

/** Server side of the arcane enchanter. Slots are player inventory indices (0-39). */
public final class ArcaneEnchanter {
    private ArcaneEnchanter() {}

    public static Optional<Holder.Reference<Enchantment>> enchantment(Player player, ResourceLocation id) {
        return player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(ResourceKey.create(Registries.ENCHANTMENT, id));
    }

    private static ItemStack slot(Inventory inventory, int slot) {
        return slot >= 0 && slot < 40 ? inventory.getItem(slot) : ItemStack.EMPTY;
    }

    /** Destroys the item in {@code slot} and learns its enchantments. Returns how many were new. */
    public static int disenchant(ServerPlayer player, BlockPos pos, int slot) {
        if (!ArcanaApi.atStation(player, pos, true)) return 0;
        return disenchant(player, slot);
    }

    public static int disenchant(Player player, int slot) {
        ItemStack stack = slot(player.getInventory(), slot);
        List<Holder<Enchantment>> learnable = Enchanting.learnable(stack);
        if (learnable.isEmpty()) return 0;
        ArcanaKnowledge knowledge = ArcanaApi.knowledge(player);
        int learned = 0;
        for (Holder<Enchantment> e : learnable) {
            ResourceLocation id = e.unwrapKey().map(ResourceKey::location).orElse(null);
            if (id != null && knowledge.enchantments().add(id)) learned++;
        }
        if (learned == 0) {
            ArcanaApi.warn(player, Component.translatable("message.urarcana.already_known"));
            return 0;
        }
        stack.shrink(stack.getCount());
        ArcanaApi.xp(player, Skill.ENCHANTING, Enchanting.disenchantXp(learned));
        ArcanaApi.subtle(player, Component.translatable("message.urarcana.learned_enchantments", learned));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8f, 0.7f);
        ArcanaApi.sync(player);
        return learned;
    }

    /** Puts a known enchantment on the gear in {@code itemSlot}, emptying the soul gem in {@code gemSlot}. */
    public static boolean enchant(ServerPlayer player, BlockPos pos, int itemSlot, ResourceLocation enchantmentId, int gemSlot) {
        if (!ArcanaApi.atStation(player, pos, true)) return false;
        return enchant(player, itemSlot, enchantmentId, gemSlot);
    }

    public static boolean enchant(Player player, int itemSlot, ResourceLocation enchantmentId, int gemSlot) {
        Inventory inventory = player.getInventory();
        ItemStack gear = slot(inventory, itemSlot);
        ItemStack gem = slot(inventory, gemSlot);
        int soul = Enchanting.soul(gem);
        Holder<Enchantment> enchantment = enchantment(player, enchantmentId).orElse(null);
        int slots = Enchanting.slots(SkillsApi.effect(player, Enchanting.ENCHANT_SLOTS, ""));
        if (enchantment == null || soul <= 0 || itemSlot == gemSlot) return false;
        if (!ArcanaApi.knowledge(player).enchantments().contains(enchantmentId)) {
            ArcanaApi.warn(player, Component.translatable("message.urarcana.unknown_enchantment"));
            return false;
        }
        if (!Enchanting.canApply(gear, enchantment, slots)) {
            ArcanaApi.warn(player, Component.translatable("message.urarcana.cannot_enchant"));
            return false;
        }
        int level = Enchanting.level(enchantment, soul, SkillsApi.level(player, Skill.ENCHANTING),
                SkillsApi.effect(player, Enchanting.ENCHANT_POWER, ""));
        gear.enchant(enchantment, level);
        gem.remove(UntamedArcana.SOUL.get());
        ArcanaApi.xp(player, Skill.ENCHANTING, Enchanting.enchantXp(soul));
        ArcanaApi.subtle(player, Component.translatable("message.urarcana.enchanted", gear.getHoverName(), Enchantment.getFullname(enchantment, level)));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1f, 1.2f);
        return true;
    }
}

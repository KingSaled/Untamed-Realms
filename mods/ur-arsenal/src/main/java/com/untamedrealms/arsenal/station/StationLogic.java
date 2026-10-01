package com.untamedrealms.arsenal.station;

import com.untamedrealms.arsenal.UntamedArsenal;
import com.untamedrealms.arsenal.block.Station;
import com.untamedrealms.arsenal.block.StationBlock;
import com.untamedrealms.arsenal.data.StationRecipe;
import com.untamedrealms.core.api.UR;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillsApi;
import com.untamedrealms.skills.events.SmithingQuality;
import com.untamedrealms.skills.registry.SkillsComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Server-side crafting at the forge / tanning rack and tempering at the workbench. */
public final class StationLogic {
    /** Highest quality tempering reaches: Fine at Smithing 1, up to Flawless (4) at 60. */
    public static int maxTemper(int smithing) {
        return Math.min(4, 1 + smithing / 20);
    }

    private StationLogic() {}

    private static boolean atStation(ServerPlayer player, BlockPos pos, Station station) {
        return player.distanceToSqr(pos.getCenter()) <= 64
                && player.level().getBlockState(pos).getBlock() instanceof StationBlock block && block.station == station;
    }

    /** Number of items in the main inventory matching an input. */
    public static int count(Inventory inventory, StationRecipe.Input input) {
        int n = 0;
        for (ItemStack stack : inventory.items) if (input.matches(stack)) n += stack.getCount();
        return n;
    }

    private static void take(Inventory inventory, StationRecipe.Input input) {
        int left = input.count();
        for (ItemStack stack : inventory.items) {
            if (left <= 0) return;
            if (input.matches(stack)) {
                int n = Math.min(left, stack.getCount());
                stack.shrink(n);
                left -= n;
            }
        }
    }

    public static void craft(ServerPlayer player, BlockPos pos, ResourceLocation id, int times) {
        StationRecipe recipe = UntamedArsenal.RECIPES.getOrNull(id);
        if (recipe == null || !atStation(player, pos, recipe.station())) return;
        if (SkillsApi.level(player, recipe.skill()) < recipe.level()) {
            UR.warn(player, Component.translatable("message.urarsenal.needs_level", recipe.skill().displayName(), recipe.level()));
            return;
        }
        int made = 0;
        for (int i = 0; i < Math.max(1, Math.min(64, times)); i++) {
            boolean ok = recipe.inputs().stream().allMatch(in -> count(player.getInventory(), in) >= in.count());
            if (!ok) break;
            recipe.inputs().forEach(in -> take(player.getInventory(), in));
            ItemStack result = recipe.resultStack();
            SmithingQuality.roll(player, result);
            if (!player.getInventory().add(result)) player.drop(result, false);
            SkillsApi.addXp(player, recipe.skill(), recipe.xp());
            made++;
        }
        if (made == 0) {
            UR.warn(player, Component.translatable("message.urarsenal.missing"));
            return;
        }
        var sound = recipe.station() == Station.FORGE ? SoundEvents.ANVIL_USE : SoundEvents.ARMOR_EQUIP_LEATHER.value();
        player.level().playSound(null, pos, sound, SoundSource.BLOCKS, 0.7f, 1.0f);
    }

    /** Raises a piece of gear's quality by one, for one unit of its repair material. */
    public static void temper(ServerPlayer player, BlockPos pos, int slot) {
        if (!atStation(player, pos, Station.WORKBENCH) || slot < 0 || slot >= 40) return;
        Inventory inventory = player.getInventory();
        ItemStack gear = inventory.getItem(slot);
        if (!SmithingQuality.isGear(gear)) return;
        int quality = gear.getOrDefault(SkillsComponents.QUALITY.get(), 0);
        int max = maxTemper(SkillsApi.level(player, Skill.SMITHING));
        if (quality >= max) {
            UR.warn(player, Component.translatable("message.urarsenal.temper_max"));
            return;
        }
        ItemStack material = ItemStack.EMPTY;
        for (ItemStack stack : inventory.items) {
            if (stack != gear && !stack.isEmpty() && gear.getItem().isValidRepairItem(gear, stack)) {
                material = stack;
                break;
            }
        }
        if (material.isEmpty()) {
            UR.warn(player, Component.translatable("message.urarsenal.temper_material"));
            return;
        }
        material.shrink(1);
        gear.set(SkillsComponents.QUALITY.get(), quality + 1);
        SkillsApi.addXp(player, Skill.SMITHING, 20f * (quality + 1));
        player.level().playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.7f, 1.2f);
    }
}

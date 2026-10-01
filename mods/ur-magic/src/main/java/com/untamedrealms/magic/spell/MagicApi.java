package com.untamedrealms.magic.spell;

import com.untamedrealms.core.api.ProgressEvent;
import com.untamedrealms.core.api.UR;
import com.untamedrealms.core.vitals.VitalsApi;
import com.untamedrealms.magic.MagicConfig;
import com.untamedrealms.magic.UntamedMagic;
import com.untamedrealms.magic.data.SpellBook;
import com.untamedrealms.magic.data.SpellDef;
import com.untamedrealms.magic.network.MagicNetwork;
import com.untamedrealms.skills.api.SkillsApi;
import com.untamedrealms.skills.effect.EffectTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

/** Public API of the magic system. */
public final class MagicApi {
    private MagicApi() {}

    public static SpellBook book(Player player) {
        return player.getData(UntamedMagic.SPELLBOOK);
    }

    public static boolean learn(ServerPlayer player, ResourceLocation spell) {
        SpellDef def = UntamedMagic.SPELLS.getOrNull(spell);
        if (def == null || !book(player).learn(spell)) return false;
        UR.banner(player, Component.translatable("banner.urmagic.learned", def.name()).withStyle(ChatFormatting.AQUA),
                Component.translatable("banner.urmagic.learned.sub", Component.keybind("key.urmagic.wheel"), Component.keybind("key.urmagic.cast")));
        MagicNetwork.sync(player);
        return true;
    }

    /** Effective magicka cost after school level and perks. */
    public static float cost(ServerPlayer player, SpellDef def) {
        float reduction = (float) (SkillsApi.level(player, def.school()) * MagicConfig.COST_REDUCTION_PER_LEVEL.get())
                + SkillsApi.effect(player, EffectTypes.COST_REDUCTION, def.school().id());
        return Math.max(1f, def.cost() * (1f - Math.min(0.8f, reduction)));
    }

    /** Magnitude multiplier from school level and perks. */
    public static float power(ServerPlayer player, SpellDef def) {
        return 1f + (float) (SkillsApi.level(player, def.school()) * MagicConfig.POWER_PER_LEVEL.get())
                + SkillsApi.effect(player, EffectTypes.SPELL_POWER, def.school());
    }

    /** Casts the spell in the given quick slot (or the selected one when slot < 0). */
    public static void castSlot(ServerPlayer player, int slot) {
        SpellBook book = book(player);
        if (slot >= 0) book.select(slot);
        ResourceLocation id = book.selectedSpell();
        if (id == null) {
            UR.warn(player, Component.translatable("message.urmagic.no_spell", Component.keybind("key.urmagic.wheel"), Component.keybind("key.urmagic.spellbook")));
            return;
        }
        cast(player, id);
    }

    public static void cast(ServerPlayer player, ResourceLocation id) {
        SpellBook book = book(player);
        SpellDef def = UntamedMagic.SPELLS.getOrNull(id);
        if (def == null || !book.knows(id) || player.isSpectator()) return;
        long now = player.serverLevel().getGameTime();
        if (now < book.readyAt(id)) return;
        float cost = cost(player, def);
        if (VitalsApi.magicka(player) < cost && !player.isCreative()) {
            UR.warn(player, Component.translatable("message.urmagic.no_magicka"));
            return;
        }
        if (!SpellEffects.cast(player, id, def, power(player, def))) return;
        VitalsApi.tryConsumeMagicka(player, cost);
        book.setReadyAt(id, now + def.cooldown());
        SkillsApi.addXp(player, def.school(), def.xp() + cost * 0.25f);
        NeoForge.EVENT_BUS.post(new ProgressEvent(player, "cast", id, 1));
        MagicNetwork.sync(player);
    }
}

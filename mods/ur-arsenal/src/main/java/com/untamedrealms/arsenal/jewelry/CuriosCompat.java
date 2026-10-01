package com.untamedrealms.arsenal.jewelry;

import com.untamedrealms.arsenal.ArsenalComponents;
import com.untamedrealms.arsenal.UntamedArsenal;
import com.untamedrealms.skills.api.SkillsApi;
import com.untamedrealms.skills.effect.EffectManager;
import com.untamedrealms.skills.effect.SkillEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Only loaded when Curios is installed: worn jewelry feeds its bonuses into the skills effect system,
 * and the player's effects are rebuilt whenever what they wear changes.
 */
public final class CuriosCompat {
    private static final Map<UUID, Integer> WORN = new ConcurrentHashMap<>();

    private CuriosCompat() {}

    public static void init() {
        EffectManager.registerProvider(UntamedArsenal.id("jewelry"), (player, out) -> {
            for (ItemStack stack : worn(player)) {
                List<SkillEffect> effects = stack.get(ArsenalComponents.JEWEL_EFFECTS.get());
                if (effects != null) effects.forEach(out);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, event -> {
            if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
            int hash = 1;
            for (ItemStack stack : worn(player)) hash = hash * 31 + BuiltInRegistries.ITEM.getKey(stack.getItem()).hashCode();
            Integer before = WORN.put(player.getUUID(), hash);
            if (before != null && before != hash) SkillsApi.refresh(player);
        });
    }

    private static List<ItemStack> worn(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.findCurios(stack -> stack.has(ArsenalComponents.JEWEL_EFFECTS.get())).stream().map(SlotResult::stack).toList())
                .orElse(List.of());
    }
}

package com.untamedrealms.npcs.shop;

import com.mojang.serialization.Dynamic;
import com.untamedrealms.core.api.UR;
import com.untamedrealms.core.economy.WalletApi;
import com.untamedrealms.npcs.data.NpcsData;
import com.untamedrealms.npcs.data.ShopDef;
import com.untamedrealms.npcs.entity.NpcEntity;
import com.untamedrealms.npcs.network.NpcsNetwork;
import com.untamedrealms.quests.engine.Targets;
import com.untamedrealms.skills.api.Skill;
import com.untamedrealms.skills.api.SkillsApi;
import com.untamedrealms.skills.effect.EffectTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Merchant buying and selling, priced by Speech. All validation is server-side. */
public final class ShopManager {
    public record Offer(ItemStack stack, int price) {}

    private record Session(int entityId, ResourceLocation shop) {}

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private ShopManager() {}

    /** Price multipliers from Speech level and price perks. */
    static double buyMultiplier(ServerPlayer player) {
        double bonus = SkillsApi.level(player, Skill.SPEECH) * 0.003 + SkillsApi.effect(player, EffectTypes.PRICE_BONUS, "");
        return Math.max(0.6, 1.3 - bonus);
    }

    static double sellMultiplier(ServerPlayer player) {
        double bonus = SkillsApi.level(player, Skill.SPEECH) * 0.002 + SkillsApi.effect(player, EffectTypes.PRICE_BONUS, "");
        return Math.min(0.95, 0.6 + bonus);
    }

    public static List<Offer> sales(ServerPlayer player, ShopDef shop) {
        List<Offer> out = new ArrayList<>();
        double m = buyMultiplier(player);
        for (ShopDef.Sale sale : shop.sells()) {
            ItemStack stack = decode(player, sale.item());
            if (!stack.isEmpty()) out.add(new Offer(stack, Math.max(1, (int) Math.ceil(sale.price() * m))));
        }
        return out;
    }

    public static List<Offer> purchases(ServerPlayer player, ShopDef shop) {
        List<Offer> out = new ArrayList<>();
        double m = sellMultiplier(player);
        for (ShopDef.Purchase p : shop.buys()) {
            String shown = p.display().isEmpty() ? p.match().split("\\|")[0].replace("#", "") : p.display();
            ResourceLocation id = ResourceLocation.tryParse(shown);
            ItemStack display = id == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(id));
            out.add(new Offer(display, Math.max(1, (int) Math.floor(p.price() * m))));
        }
        return out;
    }

    private static <T> ItemStack decode(ServerPlayer player, Dynamic<T> raw) {
        return ItemStack.CODEC.parse(RegistryOps.create(raw.getOps(), player.registryAccess()), raw.getValue()).result().orElse(ItemStack.EMPTY);
    }

    public static void open(ServerPlayer player, NpcEntity npc, ResourceLocation shopId) {
        ShopDef shop = NpcsData.SHOPS.getOrNull(shopId);
        if (shop == null) return;
        SESSIONS.put(player.getUUID(), new Session(npc.getId(), shopId));
        NpcsNetwork.openShop(player, npc, shop.name(), sales(player, shop), purchases(player, shop));
    }

    private static ShopDef validate(ServerPlayer player, int entityId) {
        Session s = SESSIONS.get(player.getUUID());
        if (s == null || s.entityId() != entityId) return null;
        if (!(player.level().getEntity(entityId) instanceof NpcEntity npc) || npc.distanceToSqr(player) > 100) return null;
        return NpcsData.SHOPS.getOrNull(s.shop());
    }

    public static void buy(ServerPlayer player, int entityId, int index, int quantity) {
        ShopDef shop = validate(player, entityId);
        if (shop == null) return;
        List<Offer> sales = sales(player, shop);
        if (index < 0 || index >= sales.size()) return;
        Offer offer = sales.get(index);
        quantity = Math.max(1, Math.min(quantity, 64));
        long total = (long) offer.price() * quantity;
        if (!WalletApi.tryWithdraw(player, total)) {
            UR.warn(player, Component.translatable("message.urnpcs.cannot_afford", total));
            return;
        }
        for (int i = 0; i < quantity; i++) {
            ItemStack stack = offer.stack().copy();
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
        SkillsApi.addXp(player, Skill.SPEECH, Math.max(1, total / 6.0));
        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_TRADE, SoundSource.NEUTRAL, 0.6f, 1f);
    }

    public static void sell(ServerPlayer player, int entityId, int index, boolean all) {
        ShopDef shop = validate(player, entityId);
        if (shop == null || index < 0 || index >= shop.buys().size()) return;
        ShopDef.Purchase purchase = shop.buys().get(index);
        int price = purchases(player, shop).get(index).price();
        int sold = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!Targets.item(purchase.match(), stack)) continue;
            int n = all ? stack.getCount() : 1;
            stack.shrink(n);
            sold += n;
            if (!all) break;
        }
        if (sold == 0) {
            UR.warn(player, Component.translatable("message.urnpcs.nothing_to_sell"));
            return;
        }
        WalletApi.deposit(player, (long) price * sold);
        SkillsApi.addXp(player, Skill.SPEECH, Math.max(1, price * sold / 6.0));
        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_TRADE, SoundSource.NEUTRAL, 0.6f, 1.2f);
    }

    public static void forget(UUID player) {
        SESSIONS.remove(player);
    }
}

package com.untamedrealms.npcs.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.List;

/**
 * A merchant's stock, from {@code data/<ns>/urnpcs/shops/<id>.json}. {@code sells}: what the merchant
 * sells to players (item + base price). {@code buys}: what they buy (item matcher + base price).
 * Speech level and price perks shift both sides.
 */
public record ShopDef(Component name, List<Sale> sells, List<Purchase> buys) {
    public static final Codec<ShopDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(ShopDef::name),
            Sale.CODEC.listOf().optionalFieldOf("sells", List.of()).forGetter(ShopDef::sells),
            Purchase.CODEC.listOf().optionalFieldOf("buys", List.of()).forGetter(ShopDef::buys)
    ).apply(inst, ShopDef::new));

    public record Sale(Dynamic<?> item, int price) {
        public static final Codec<Sale> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.PASSTHROUGH.fieldOf("item").forGetter(Sale::item),
                Codec.intRange(1, 1_000_000).fieldOf("price").forGetter(Sale::price)
        ).apply(inst, Sale::new));
    }

    public record Purchase(String match, int price, String display) {
        public static final Codec<Purchase> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("match").forGetter(Purchase::match),
                Codec.intRange(1, 1_000_000).fieldOf("price").forGetter(Purchase::price),
                Codec.STRING.optionalFieldOf("display", "").forGetter(Purchase::display)
        ).apply(inst, Purchase::new));
    }
}

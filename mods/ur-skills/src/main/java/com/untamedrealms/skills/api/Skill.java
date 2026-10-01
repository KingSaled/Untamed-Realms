package com.untamedrealms.skills.api;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * The 24 skills of Untamed Realms: Skyrim's 18 plus six RuneScape-style gathering/artisan skills.
 * Skills improve by doing; each has its own XP sources, passive per-level bonus and perk tree.
 */
public enum Skill implements StringRepresentable {
    // Combat (the Warrior)
    ONE_HANDED(Category.COMBAT, () -> Items.IRON_SWORD),
    TWO_HANDED(Category.COMBAT, () -> Items.MACE),
    ARCHERY(Category.COMBAT, () -> Items.BOW),
    BLOCK(Category.COMBAT, () -> Items.SHIELD),
    HEAVY_ARMOR(Category.COMBAT, () -> Items.IRON_CHESTPLATE),
    SMITHING(Category.COMBAT, () -> Items.ANVIL),
    // Magic (the Mage)
    DESTRUCTION(Category.MAGIC, () -> Items.FIRE_CHARGE),
    RESTORATION(Category.MAGIC, () -> Items.GOLDEN_APPLE),
    ALTERATION(Category.MAGIC, () -> Items.AMETHYST_SHARD),
    CONJURATION(Category.MAGIC, () -> Items.SOUL_LANTERN),
    ILLUSION(Category.MAGIC, () -> Items.ENDER_EYE),
    ENCHANTING(Category.MAGIC, () -> Items.ENCHANTING_TABLE),
    // Stealth (the Thief)
    LIGHT_ARMOR(Category.STEALTH, () -> Items.LEATHER_CHESTPLATE),
    SNEAK(Category.STEALTH, () -> Items.LEATHER_BOOTS),
    LOCKPICKING(Category.STEALTH, () -> Items.TRIPWIRE_HOOK),
    PICKPOCKET(Category.STEALTH, () -> Items.GOLD_NUGGET),
    SPEECH(Category.STEALTH, () -> Items.WRITABLE_BOOK),
    ALCHEMY(Category.STEALTH, () -> Items.BREWING_STAND),
    // Gathering & artisan (the Artisan)
    MINING(Category.GATHERING, () -> Items.IRON_PICKAXE),
    WOODCUTTING(Category.GATHERING, () -> Items.OAK_LOG),
    FISHING(Category.GATHERING, () -> Items.FISHING_ROD),
    FARMING(Category.GATHERING, () -> Items.WHEAT),
    COOKING(Category.GATHERING, () -> Items.COOKED_BEEF),
    AGILITY(Category.GATHERING, () -> Items.FEATHER);

    public static final Codec<Skill> CODEC = StringRepresentable.fromEnum(Skill::values);
    public static final List<Skill> VALUES = List.of(values());

    private final Category category;
    private final Supplier<Item> icon;
    private final String id;

    Skill(Category category, Supplier<Item> icon) {
        this.category = category;
        this.icon = icon;
        this.id = name().toLowerCase(Locale.ROOT);
    }

    public Category category() { return category; }
    public Item icon() { return icon.get(); }
    public String id() { return id; }

    public Component displayName() {
        return Component.translatable("skill.urskills." + id);
    }

    public Component description() {
        return Component.translatable("skill.urskills." + id + ".desc");
    }

    @Override
    public String getSerializedName() { return id; }

    public static Skill byId(String id) {
        for (Skill s : VALUES) if (s.id.equals(id)) return s;
        return null;
    }

    public static List<Skill> inCategory(Category category) {
        return Arrays.stream(values()).filter(s -> s.category == category).toList();
    }

    public enum Category implements StringRepresentable {
        COMBAT, MAGIC, STEALTH, GATHERING;

        public static final Codec<Category> CODEC = StringRepresentable.fromEnum(Category::values);

        public String id() { return name().toLowerCase(Locale.ROOT); }

        public Component displayName() {
            return Component.translatable("skill_category.urskills." + id());
        }

        @Override
        public String getSerializedName() { return id(); }
    }
}

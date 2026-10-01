package com.untamedrealms.core.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.List;

/**
 * Rolls an extra loot table into every loot table whose path starts with a prefix (by default
 * {@code chests/}), with a chance. This reaches structure chests from every worldgen mod that follows
 * the {@code chests/...} naming convention - which is nearly all of them - without listing them.
 * <pre>{@code
 * { "type": "urcore:inject_chest_loot", "conditions": [], "table": "urmagic:inject/spell_tomes",
 *   "chance": 0.15, "prefixes": ["chests/"] }
 * }</pre>
 */
public class InjectChestLootModifier extends LootModifier {
    public static final MapCodec<InjectChestLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).and(inst.group(
            ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("table").forGetter(m -> m.table),
            Codec.floatRange(0, 1).optionalFieldOf("chance", 1f).forGetter(m -> m.chance),
            Codec.STRING.listOf().optionalFieldOf("prefixes", List.of("chests/")).forGetter(m -> m.prefixes),
            Codec.STRING.listOf().optionalFieldOf("exclude_namespaces", List.of()).forGetter(m -> m.excludeNamespaces)
    )).apply(inst, InjectChestLootModifier::new));

    private final ResourceKey<LootTable> table;
    private final float chance;
    private final List<String> prefixes;
    private final List<String> excludeNamespaces;

    public InjectChestLootModifier(LootItemCondition[] conditions, ResourceKey<LootTable> table, float chance,
                                   List<String> prefixes, List<String> excludeNamespaces) {
        super(conditions);
        this.table = table;
        this.chance = chance;
        this.prefixes = prefixes;
        this.excludeNamespaces = excludeNamespaces;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ResourceLocation queried = context.getQueriedLootTableId();
        if (queried == null || queried.equals(table.location())) return generatedLoot;
        if (excludeNamespaces.contains(queried.getNamespace())) return generatedLoot;
        String path = queried.getPath();
        boolean matches = false;
        for (String prefix : prefixes) {
            if (path.startsWith(prefix)) { matches = true; break; }
        }
        if (!matches || context.getRandom().nextFloat() >= chance) return generatedLoot;

        context.getResolver().get(Registries.LOOT_TABLE, table).ifPresent(holder ->
                holder.value().getRandomItemsRaw(context, LootTable.createStackSplitter(context.getLevel(), generatedLoot::add)));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}

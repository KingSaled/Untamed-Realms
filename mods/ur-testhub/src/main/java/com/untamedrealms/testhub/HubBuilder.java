package com.untamedrealms.testhub;

import com.untamedrealms.arsenal.ArsenalArmor;
import com.untamedrealms.arsenal.ArsenalBlocks;
import com.untamedrealms.arsenal.ArsenalItems;
import com.untamedrealms.arsenal.ArsenalTier;
import com.untamedrealms.arsenal.WeaponType;
import com.untamedrealms.arsenal.jewelry.ArsenalJewelry;
import com.untamedrealms.npcs.UntamedNpcs;
import com.untamedrealms.npcs.data.NpcDef;
import com.untamedrealms.npcs.data.NpcsData;
import com.untamedrealms.npcs.entity.NpcEntity;
import com.untamedrealms.quests.UntamedQuests;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Builds the test hub: an 81x81 floating platform above the starting village, in four quarters.
 * <pre>
 *            north (-z)
 *   FORGE YARD    |   MAGIC
 *   stations,     |   tomes, skill books,
 *   materials,    |   enchanting, training
 *   weapons/armor |   dummies
 *  ---------------+---------------  spawn in the middle
 *   TOWN SQUARE   |   WILDS
 *   every NPC,    |   trees, farm, pond,
 *   notice boards,|   ore wall, tools,
 *   quest items   |   mob arena
 *            south (+z)
 * </pre>
 * Everything is placed in code, so {@code /ur test rebuild} restores the hub (restocks chests, regrows
 * trees and ores, respawns NPCs).
 */
public final class HubBuilder {
    /** Half the hub's width. */
    public static final int R = 40;
    private static final int CLEAR_BELOW = 3, CLEAR_ABOVE = 22;

    // the mob arena (inclusive bounds, relative to the centre)
    private static final int ARENA_X1 = 8, ARENA_X2 = 34, ARENA_Z1 = 30, ARENA_Z2 = 38;
    private static final int[][] DUMMIES = {{14, -30}, {20, -30}, {26, -30}};

    private final ServerLevel level;
    private final BlockPos c;
    private final int y0;

    private HubBuilder(ServerLevel level, BlockPos center) {
        this.level = level;
        this.c = center;
        this.y0 = center.getY();
    }

    // ---------------------------------------------------------------- entry points

    /** Builds (or rebuilds) the hub above the starting village and puts the world spawn on it. */
    public static BlockPos buildAndRemember(ServerLevel overworld) {
        HubData data = HubData.get(overworld);
        BlockPos ground = data.village != null ? data.village : overworld.getSharedSpawnPos();
        BlockPos center = data.center != null ? data.center : new BlockPos(ground.getX(), floorHeight(overworld, ground), ground.getZ());
        long t0 = System.currentTimeMillis();
        build(overworld, center);
        data.center = center;
        data.village = ground;
        data.setDirty();
        overworld.setDefaultSpawnPos(center.above(), 0f);
        UntamedTestHub.LOGGER.info("Test hub built at {} in {} ms (village below at {})", center, System.currentTimeMillis() - t0, ground);
        return center;
    }

    /** Builds the hub with its floor centred on {@code center}. Used directly by the game test. */
    public static void build(ServerLevel level, BlockPos center) {
        HubBuilder b = new HubBuilder(level, center);
        // keep the hub loaded, so its NPCs, dummies and crops are always there
        for (int cx = (center.getX() - R) >> 4; cx <= (center.getX() + R) >> 4; cx++) {
            for (int cz = (center.getZ() - R) >> 4; cz <= (center.getZ() + R) >> 4; cz++) level.setChunkForced(cx, cz, true);
        }
        b.clear();
        b.floor();
        b.plaza();
        b.forgeYard();
        b.magic();
        b.town();
        b.wilds();
    }

    public static void respawnDummies(ServerLevel level, BlockPos center) {
        new HubBuilder(level, center).dummies();
    }

    /** High enough to clear the terrain under the whole hub, and at least 32 blocks above the village. */
    private static int floorHeight(ServerLevel level, BlockPos ground) {
        int top = ground.getY();
        for (int x = -R; x <= R; x += 8) {
            for (int z = -R; z <= R; z += 8) {
                top = Math.max(top, level.getHeight(Heightmap.Types.MOTION_BLOCKING, ground.getX() + x, ground.getZ() + z));
            }
        }
        int y = Math.max(top + 16, ground.getY() + 32);
        return Math.min(y, level.getMaxBuildHeight() - CLEAR_ABOVE - 2);
    }

    // ---------------------------------------------------------------- the build

    private void clear() {
        AABB box = new AABB(at(-R - 2, -CLEAR_BELOW, -R - 2).getCenter(), at(R + 2, CLEAR_ABOVE, R + 2).getCenter());
        for (Entity e : level.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof Player))) e.discard();
        fill(-R, -CLEAR_BELOW, -R, R, CLEAR_ABOVE, R, Blocks.AIR.defaultBlockState());
    }

    private void floor() {
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                boolean edge = Math.abs(x) == R || Math.abs(z) == R;
                boolean path = Math.abs(x) <= 2 || Math.abs(z) <= 2;
                boolean light = Math.floorMod(x + R, 8) == 4 && Math.floorMod(z + R, 8) == 4 && !path;
                set(x, 0, z, (edge ? Blocks.STONE_BRICKS : path ? Blocks.POLISHED_ANDESITE : light ? Blocks.SEA_LANTERN : Blocks.SMOOTH_STONE).defaultBlockState());
            }
        }
        // a knee-high wall all round, with lanterns
        for (int i = -R; i <= R; i++) {
            for (int[] p : new int[][]{{i, -R}, {i, R}, {-R, i}, {R, i}}) {
                shaped(p[0], 1, p[1], Blocks.STONE_BRICK_WALL.defaultBlockState());
                if (Math.floorMod(i, 8) == 0) set(p[0], 2, p[1], Blocks.LANTERN.defaultBlockState());
            }
        }
    }

    private void plaza() {
        sign(-3, 1, -3, 14, null, title("Forge Yard"), line("stations, materials,"), line("weapons & armor"));
        sign(3, 1, -3, 2, null, title("Magic"), line("tomes, skill books,"), line("enchanting, dummies"));
        sign(-3, 1, 3, 10, null, title("Town Square"), line("every NPC, notice"), line("boards, quest items"));
        sign(3, 1, 3, 6, null, title("Wilds"), line("trees, farm, pond,"), line("ores, mob arena"));
        sign(3, 1, 4, 8, "/ur test village", title("Village below"), line("click to go down"), line("(come back with"), line("/ur test hub)"));
        sign(-3, 1, 4, 8, "/ur test book", title("Control book"), line("click for a new"), line("copy of the book"));
    }

    private void forgeYard() {
        // stations, facing the path
        List<Block> stations = List.of(ArsenalBlocks.FORGE.get(), ArsenalBlocks.TANNING_RACK.get(), ArsenalBlocks.WORKBENCH.get(),
                Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.ANVIL, Blocks.GRINDSTONE,
                Blocks.SMITHING_TABLE, Blocks.STONECUTTER, Blocks.LOOM);
        for (int i = 0; i < stations.size(); i++) set(-6 - 2 * i, 1, -6, placed(stations.get(i).defaultBlockState(), Direction.SOUTH));

        // materials: vanilla basics, then every Arsenal material
        List<ItemStack> materials = new ArrayList<>(List.of(
                full(Items.IRON_INGOT), full(Items.GOLD_INGOT), full(Items.COPPER_INGOT), full(Items.COAL), full(Items.CHARCOAL),
                full(Items.DIAMOND), full(Items.LEATHER), full(Items.RABBIT_HIDE), full(Items.STRING), full(Items.STICK),
                full(Items.OAK_PLANKS), full(Items.COBBLESTONE), full(Items.FLINT), full(Items.FEATHER), full(Items.ARROW),
                full(Items.LAPIS_LAZULI), full(Items.BOOK), full(Items.RAW_IRON), full(Items.RAW_GOLD)));
        Set<Item> gear = new java.util.HashSet<>();
        ArsenalItems.WEAPONS.values().forEach(w -> gear.add(w.get()));
        ArsenalArmor.ITEMS.values().forEach(a -> gear.add(a.get()));
        ArsenalJewelry.ITEMS.values().forEach(j -> gear.add(j.get()));
        materials.addAll(namespace("urarsenal", item -> !gear.contains(item)));
        chestRow(-6, -10, -2, Direction.SOUTH, "Materials", materials);

        // one chest of weapons per tier
        for (int t = 0; t < ArsenalTier.values().length; t++) {
            ArsenalTier tier = ArsenalTier.values()[t];
            List<ItemStack> weapons = new ArrayList<>();
            for (WeaponType type : WeaponType.values()) weapons.add(new ItemStack(ArsenalItems.weapon(tier, type)));
            chestRow(-6 - 2 * t, -14, -2, Direction.SOUTH, cap(tier.id) + " weapons", weapons);
        }

        // an armor stand wearing each set, then chests with every piece and the jewelry
        ArsenalArmor.Set[] sets = ArsenalArmor.Set.values();
        List<ItemStack> armor = new ArrayList<>();
        for (int i = 0; i < sets.length; i++) {
            ArsenalArmor.Set set = sets[i];
            ArmorStand stand = new ArmorStand(level, 0, 0, 0);
            BlockPos p = at(-6 - 2 * i, 1, -19);
            stand.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0f, 0f);
            for (ArmorItem.Type piece : ArsenalArmor.PIECES) {
                Item item = ArsenalArmor.ITEMS.get(set.id + "_" + piece.getName()).get();
                stand.setItemSlot(piece.getSlot(), new ItemStack(item));
                armor.add(new ItemStack(item));
            }
            stand.setCustomName(Component.literal(cap(set.id)).withStyle(ChatFormatting.GOLD));
            stand.setCustomNameVisible(true);
            level.addFreshEntity(stand);
        }
        int next = chestRow(-6, -24, -2, Direction.SOUTH, "Armor", armor);
        List<ItemStack> jewelry = new ArrayList<>();
        ArsenalJewelry.ITEMS.values().forEach(j -> jewelry.add(new ItemStack(j.get())));
        chestRow(next, -24, -2, Direction.SOUTH, "Rings & amulets", jewelry);
    }

    private void magic() {
        int x = chestRow(6, -6, 2, Direction.SOUTH, "Magic", namespace("urmagic", item -> true));
        chestRow(x, -6, 2, Direction.SOUTH, "Skill books", namespace("urskills", item -> true));

        // vanilla enchanting until ur-arcana replaces it
        set(26, 1, -12, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean ring = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                if (!ring || (dz == 2 && dx == 0)) continue;   // keep the south side open
                set(26 + dx, 1, -12 + dz, Blocks.BOOKSHELF.defaultBlockState());
                set(26 + dx, 2, -12 + dz, Blocks.BOOKSHELF.defaultBlockState());
            }
        }
        chestRow(22, -8, 2, Direction.SOUTH, "Enchanting", List.of(full(Items.LAPIS_LAZULI), full(Items.BOOK),
                new ItemStack(Items.ENCHANTED_BOOK, 4), full(Items.EXPERIENCE_BOTTLE)));

        // training range: dummies in front of a backstop
        for (int bx = 10; bx <= 30; bx++) {
            for (int dy = 1; dy <= 3; dy++) set(bx, dy, -33, Blocks.TARGET.defaultBlockState());
        }
        dummies();
        sign(20, 1, -22, 8, "/ur test dummies", title("Training range"), line("click to respawn"), line("the dummies"));
    }

    private void dummies() {
        AABB range = new AABB(at(8, 0, -32).getCenter(), at(32, 4, -28).getCenter());
        level.getEntitiesOfClass(Husk.class, range).forEach(Entity::discard);
        for (int[] d : DUMMIES) {
            Husk husk = EntityType.HUSK.create(level);
            if (husk == null) continue;
            BlockPos p = at(d[0], 1, d[1]);
            husk.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0f, 0f);
            husk.setNoAi(true);
            husk.setSilent(true);
            husk.setPersistenceRequired();
            husk.setCustomName(Component.literal("Training dummy"));
            level.addFreshEntity(husk);
        }
    }

    private void town() {
        // every NPC archetype in its own pen, three rows of five
        List<ResourceLocation> npcs = new ArrayList<>(NpcsData.NPCS.entries().keySet());
        npcs.sort(null);
        for (int i = 0; i < npcs.size(); i++) {
            int x = -7 - 5 * (i % 5), z = 8 + 6 * (i / 5);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) if (dx != 0 || dz != 0) shaped(x + dx, 1, z + dz, Blocks.OAK_FENCE.defaultBlockState());
            }
            spawnNpc(npcs.get(i), x, z);
        }

        // notice boards (two, so there are more bounties to pick from)
        set(-8, 1, 30, placed(UntamedQuests.NOTICE_BOARD.get().defaultBlockState(), Direction.NORTH));
        set(-16, 1, 30, placed(UntamedQuests.NOTICE_BOARD.get().defaultBlockState(), Direction.NORTH));

        List<ItemStack> quest = List.of(stack(Items.BREAD, 16), stack(Items.OAK_LOG, 32), stack(Items.EMERALD, 16), stack(Items.GOLD_INGOT, 16),
                stack(Items.SPIDER_EYE, 16), stack(Items.SUGAR, 16), stack(Items.GLISTERING_MELON_SLICE, 8), stack(Items.GOLDEN_CARROT, 8),
                stack(Items.WHEAT, 32), stack(Items.COD, 16), stack(Items.SALMON, 16), stack(Items.LEATHER, 16), stack(Items.IRON_INGOT, 32),
                stack(Items.ENCHANTED_BOOK, 2), stack(Items.ENDER_PEARL, 16), stack(Items.RAW_IRON, 32), stack(Items.COOKED_BEEF, 16),
                stack(Items.COOKED_PORKCHOP, 16), stack(Items.WHITE_WOOL, 32), stack(Items.TRIAL_KEY, 2), stack(Items.OMINOUS_TRIAL_KEY, 1),
                stack(Items.BONE, 16), stack(Items.GUNPOWDER, 16), stack(Items.ROTTEN_FLESH, 16), stack(Items.COAL, 16));
        int x = chestRow(-6, 35, -2, Direction.NORTH, "Quest items", quest);

        List<ItemStack> food = new ArrayList<>(List.of(stack(Items.APPLE, 16), stack(Items.BREAD, 16), stack(Items.COOKED_BEEF, 16),
                stack(Items.COOKED_CHICKEN, 16), stack(Items.BAKED_POTATO, 16), stack(Items.CARROT, 16), stack(Items.GOLDEN_APPLE, 4),
                stack(Items.PUMPKIN_PIE, 8), stack(Items.COOKIE, 16), stack(Items.MUSHROOM_STEW, 1), stack(Items.HONEY_BOTTLE, 8),
                stack(Items.SWEET_BERRIES, 16), stack(Items.MELON_SLICE, 16), stack(Items.ROTTEN_FLESH, 8), stack(Items.PUFFERFISH, 2)));
        food.addAll(namespace("farmersdelight", item -> new ItemStack(item).has(DataComponents.FOOD)));
        x = chestRow(x, 35, -2, Direction.NORTH, "Food", food);

        List<ItemStack> misc = new ArrayList<>();
        for (String ns : List.of("urcore", "urquests", "urnpcs", "urclasses")) misc.addAll(namespace(ns, item -> true));
        if (!misc.isEmpty()) x = chestRow(x, 35, -2, Direction.NORTH, "Coins & misc", misc);

        // Farmer's Delight kitchen, when it is installed
        Block stove = block("farmersdelight:stove");
        if (stove != null) {
            set(-26, 1, 30, placed(stove.defaultBlockState(), Direction.NORTH));
            Block pot = block("farmersdelight:cooking_pot");
            if (pot != null) set(-26, 2, 30, placed(pot.defaultBlockState(), Direction.NORTH));
            Block board = block("farmersdelight:cutting_board");
            if (board != null) set(-28, 1, 30, placed(board.defaultBlockState(), Direction.NORTH));
            Block skillet = block("farmersdelight:skillet");
            set(-30, 1, 30, placed(stove.defaultBlockState(), Direction.NORTH));
            if (skillet != null) set(-30, 2, 30, placed(skillet.defaultBlockState(), Direction.NORTH));
            List<ItemStack> tools = namespace("farmersdelight", item -> !new ItemStack(item).has(DataComponents.FOOD));
            chestRow(x, 35, -2, Direction.NORTH, "Farmer's Delight", tools);
        }
    }

    private void spawnNpc(ResourceLocation id, int x, int z) {
        NpcDef def = NpcsData.NPCS.getOrNull(id);
        NpcEntity npc = UntamedNpcs.NPC.get().create(level);
        if (def == null || npc == null) return;
        BlockPos p = at(x, 1, z);
        npc.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 180f, 0f);
        npc.setup(id, def);
        npc.setHome(p, 0);
        npc.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.COMMAND, null);
        level.addFreshEntity(npc);
    }

    private void wilds() {
        // a grove of real (generated) trees for Woodcutting and tree felling
        List<ResourceKey<ConfiguredFeature<?, ?>>> trees = List.of(TreeFeatures.OAK, TreeFeatures.BIRCH, TreeFeatures.SPRUCE,
                TreeFeatures.ACACIA, TreeFeatures.CHERRY);
        var registry = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        for (int i = 0; i < trees.size(); i++) {
            int x = 8 + 6 * i, z = 8;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) set(x + dx, 0, z + dz, Blocks.GRASS_BLOCK.defaultBlockState());
            int tx = x;
            registry.getHolder(trees.get(i)).ifPresent(tree ->
                    tree.value().place(level, level.getChunkSource().getGenerator(), level.random, at(tx, 1, z)));
        }

        // a farm with ripe crops around a water channel
        Block[] crops = {Blocks.WHEAT, Blocks.CARROTS, null, Blocks.POTATOES, Blocks.BEETROOTS};
        for (int x = 8; x <= 18; x++) {
            for (int i = 0; i < crops.length; i++) {
                int z = 14 + i;
                if (crops[i] == null) {
                    set(x, 0, z, Blocks.WATER.defaultBlockState());
                    continue;
                }
                set(x, 0, z, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
                CropBlock crop = (CropBlock) crops[i];
                set(x, 1, z, crop.getStateForAge(crop.getMaxAge()));
            }
        }

        // a fishing pond, two deep
        for (int x = 22; x <= 32; x++) {
            for (int z = 12; z <= 20; z++) {
                boolean rim = x == 22 || x == 32 || z == 12 || z == 20;
                if (rim) {
                    set(x, -1, z, Blocks.STONE.defaultBlockState());
                } else {
                    set(x, 0, z, Blocks.WATER.defaultBlockState());
                    set(x, -1, z, Blocks.WATER.defaultBlockState());
                    set(x, -2, z, Blocks.SAND.defaultBlockState());
                }
            }
        }

        // an ore wall: vanilla ores and the four Arsenal ores, two of each
        List<Block> ores = List.of(Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.COPPER_ORE, Blocks.GOLD_ORE, Blocks.REDSTONE_ORE,
                Blocks.LAPIS_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
                ArsenalBlocks.ORICHALCUM_ORE.get(), ArsenalBlocks.MOONSTONE_ORE.get(), ArsenalBlocks.MALACHITE_ORE.get(), ArsenalBlocks.EBONY_ORE.get());
        for (int i = 0; i < ores.size(); i++) {
            int x = 8 + 2 * i;
            set(x, 1, 26, ores.get(i).defaultBlockState());
            set(x, 2, 26, ores.get(i).defaultBlockState());
            set(x + 1, 1, 26, Blocks.STONE.defaultBlockState());
            set(x + 1, 2, 26, Blocks.DEEPSLATE.defaultBlockState());
            for (int dy = 1; dy <= 3; dy++) {
                set(x, dy, 27, Blocks.STONE_BRICKS.defaultBlockState());
                set(x + 1, dy, 27, Blocks.STONE_BRICKS.defaultBlockState());
            }
        }

        List<ItemStack> tools = List.of(new ItemStack(Items.WOODEN_PICKAXE), new ItemStack(Items.STONE_PICKAXE), new ItemStack(Items.IRON_PICKAXE),
                new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.NETHERITE_PICKAXE), new ItemStack(Items.IRON_AXE),
                new ItemStack(Items.DIAMOND_AXE), new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.IRON_HOE),
                new ItemStack(Items.FISHING_ROD), new ItemStack(Items.FISHING_ROD), new ItemStack(Items.SHEARS),
                new ItemStack(Items.BUCKET), new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.BOW), new ItemStack(Items.CROSSBOW),
                full(Items.ARROW), new ItemStack(Items.SHIELD), full(Items.TORCH), full(Items.BONE_MEAL), full(Items.WHEAT_SEEDS),
                full(Items.CARROT), full(Items.POTATO), full(Items.BEETROOT_SEEDS), stack(Items.OAK_SAPLING, 16),
                stack(Items.BIRCH_SAPLING, 16), stack(Items.SPRUCE_SAPLING, 16), full(Items.DIRT));
        for (int i = 0, n = (tools.size() + 26) / 27; i < n; i++) {
            chest(37, 1, 10 + 2 * i, Direction.WEST, "Tools & seeds" + (n > 1 ? " " + (i + 1) : ""), tools.subList(27 * i, Math.min(tools.size(), 27 * (i + 1))), 4);
        }

        arena();
    }

    private void arena() {
        for (int x = ARENA_X1; x <= ARENA_X2; x++) {
            for (int z = ARENA_Z1; z <= ARENA_Z2; z++) {
                boolean wall = x == ARENA_X1 || x == ARENA_X2 || z == ARENA_Z1 || z == ARENA_Z2;
                if (wall) for (int dy = 1; dy <= 4; dy++) set(x, dy, z, Blocks.STONE_BRICKS.defaultBlockState());
                set(x, 5, z, Blocks.SPRUCE_PLANKS.defaultBlockState());   // a roof, so undead don't burn
            }
        }
        // the way in: a gate mobs can't open
        int gate = (ARENA_X1 + ARENA_X2) / 2;
        set(gate, 2, ARENA_Z1, Blocks.AIR.defaultBlockState());
        set(gate, 1, ARENA_Z1, placed(Blocks.OAK_FENCE_GATE.defaultBlockState(), Direction.NORTH));

        BlockPos mid = at(gate, 1, (ARENA_Z1 + ARENA_Z2) / 2);
        String[] mobs = {"zombie", "skeleton", "spider", "creeper", "witch", "pillager", "husk", "drowned", "cave_spider",
                "vindicator", "stray", "enderman"};
        int x = ARENA_X1 + 2;
        for (String mob : mobs) {
            if (x == gate) x++;
            wallSign(x, 2, ARENA_Z1 - 1, Direction.NORTH, "/summon minecraft:" + mob + " " + mid.getX() + " " + mid.getY() + " " + mid.getZ(),
                    line("Spawn a"), title(cap(mob.replace('_', ' '))));
            x += 2;
        }
        BlockPos lo = at(ARENA_X1 + 1, 1, ARENA_Z1 + 1);
        wallSign(gate, 3, ARENA_Z1 - 1, Direction.NORTH, String.format("/kill @e[x=%d,y=%d,z=%d,dx=%d,dy=3,dz=%d,type=!minecraft:player]",
                lo.getX(), lo.getY(), lo.getZ(), ARENA_X2 - ARENA_X1 - 2, ARENA_Z2 - ARENA_Z1 - 2), title("Clear arena"));
    }

    // ---------------------------------------------------------------- helpers

    private BlockPos at(int x, int dy, int z) {
        return new BlockPos(c.getX() + x, y0 + dy, c.getZ() + z);
    }

    private void set(int x, int dy, int z, BlockState state) {
        level.setBlock(at(x, dy, z), state, Block.UPDATE_CLIENTS);
    }

    /** Fences and walls: connect to what is already around them. */
    private void shaped(int x, int dy, int z, BlockState state) {
        BlockPos p = at(x, dy, z);
        level.setBlock(p, Block.updateFromNeighbourShapes(state, level, p), Block.UPDATE_CLIENTS);
    }

    private void fill(int x1, int dy1, int z1, int x2, int dy2, int z2, BlockState state) {
        for (int x = x1; x <= x2; x++) for (int dy = dy1; dy <= dy2; dy++) for (int z = z1; z <= z2; z++) set(x, dy, z, state);
    }

    private static BlockState placed(BlockState state, Direction facing) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        if (state.hasProperty(BlockStateProperties.ATTACH_FACE)) state = state.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
        return state;
    }

    /** As many chests as the items need, one every {@code step} blocks along x. Returns the next free x. */
    private int chestRow(int x, int z, int step, Direction facing, String title, List<ItemStack> items) {
        int n = Math.max(1, (items.size() + 26) / 27);
        int rotation = switch (facing) { case NORTH -> 8; case EAST -> 12; case WEST -> 4; default -> 0; };
        for (int i = 0; i < n; i++) {
            List<ItemStack> part = items.subList(Math.min(items.size(), 27 * i), Math.min(items.size(), 27 * (i + 1)));
            chest(x + step * i, 1, z, facing, n > 1 ? title + " " + (i + 1) : title, part, rotation);
        }
        return x + step * n;
    }

    private void chest(int x, int dy, int z, Direction facing, String title, List<ItemStack> items, int signRotation) {
        BlockPos p = at(x, dy, z);
        level.setBlock(p, placed(Blocks.CHEST.defaultBlockState(), facing), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(p) instanceof ChestBlockEntity chest) {
            // name first: applying components also applies an (empty) container component, clearing the chest
            chest.applyComponents(DataComponentMap.builder().set(DataComponents.CUSTOM_NAME, Component.literal(title)).build(), DataComponentPatch.EMPTY);
            for (int i = 0; i < Math.min(items.size(), chest.getContainerSize()); i++) chest.setItem(i, items.get(i).copy());
            chest.setChanged();
        }
        sign(x, dy + 1, z, signRotation, null, title(title));
    }

    private void sign(int x, int dy, int z, int rotation, @Nullable String command, Component... lines) {
        BlockPos p = at(x, dy, z);
        BlockState state = Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation);
        level.setBlock(p, state, Block.UPDATE_CLIENTS);
        writeSign(p, state, command, lines);
    }

    private void wallSign(int x, int dy, int z, Direction facing, @Nullable String command, Component... lines) {
        BlockPos p = at(x, dy, z);
        BlockState state = Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing);
        level.setBlock(p, state, Block.UPDATE_CLIENTS);
        writeSign(p, state, command, lines);
    }

    private void writeSign(BlockPos p, BlockState state, @Nullable String command, Component[] lines) {
        if (!(level.getBlockEntity(p) instanceof SignBlockEntity sign)) return;
        SignText text = new SignText();
        for (int i = 0; i < 4 && i < lines.length; i++) {
            Component line = lines[i];
            // one click event only: the sign runs every line's command
            if (i == 0 && command != null) line = line.copy().withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
            text = text.setMessage(i, line);
        }
        sign.setText(text, true);
        sign.setWaxed(true);
        sign.setChanged();
        level.sendBlockUpdated(p, state, state, Block.UPDATE_ALL);
    }

    private static Component title(String text) {
        return Component.literal(text).withStyle(ChatFormatting.BOLD);
    }

    private static Component line(String text) {
        return Component.literal(text);
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    private static ItemStack full(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.setCount(stack.getMaxStackSize());
        return stack;
    }

    private static @Nullable Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id)).orElse(null);
    }

    /** Every item of a mod, a full stack each, in registration order. */
    public static List<ItemStack> namespace(String ns, Predicate<Item> filter) {
        List<ItemStack> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR || !BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(ns) || !filter.test(item)) continue;
            out.add(full(item));
        }
        return out;
    }

    /** For the game test: how many NPCs the hub holds. */
    public static int npcCount() {
        return NpcsData.NPCS.entries().size();
    }

    public static int dummyCount() {
        return DUMMIES.length;
    }
}

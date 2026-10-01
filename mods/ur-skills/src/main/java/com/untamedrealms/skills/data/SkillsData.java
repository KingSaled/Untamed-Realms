package com.untamedrealms.skills.data;

import com.untamedrealms.core.data.DataRegistry;
import com.untamedrealms.skills.UntamedSkills;
import com.untamedrealms.skills.api.Skill;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Data registries owned by the skills module plus derived lookup indexes. */
public final class SkillsData {
    public static final DataRegistry<PerkTree> PERK_TREES =
            DataRegistry.create(UntamedSkills.id("perks"), "urskills/perks", PerkTree.CODEC, true);
    public static final DataRegistry<XpSource> XP_SOURCES =
            DataRegistry.create(UntamedSkills.id("xp_sources"), "urskills/xp_sources", XpSource.CODEC, false);
    public static final DataRegistry<RequirementSet> REQUIREMENTS =
            DataRegistry.create(UntamedSkills.id("requirements"), "urskills/requirements", RequirementSet.CODEC, true);

    /** A perk with its resolved global id and owning tree. */
    public record PerkRef(ResourceLocation id, Skill skill, PerkTree.Perk perk, List<ResourceLocation> requires) {}

    private static volatile Map<ResourceLocation, PerkRef> perks = Map.of();
    private static volatile Map<Skill, List<PerkRef>> perksBySkill = Map.of();
    private static volatile Map<XpSource.Trigger, List<XpSource>> xpByTrigger = Map.of();
    private static volatile Map<RequirementSet.Kind, List<RequirementSet.Requirement>> requirementsByKind = Map.of();

    static {
        PERK_TREES.onReload(registry -> {
            Map<ResourceLocation, PerkRef> index = new HashMap<>();
            Map<Skill, List<PerkRef>> bySkill = new EnumMap<>(Skill.class);
            registry.entries().forEach((fileId, tree) -> {
                for (PerkTree.Perk perk : tree.perks()) {
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), perk.id());
                    List<ResourceLocation> requires = perk.requires().stream()
                            .map(r -> r.contains(":") ? ResourceLocation.parse(r) : ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), r))
                            .toList();
                    PerkRef ref = new PerkRef(id, tree.skill(), perk, requires);
                    index.put(id, ref);
                    bySkill.computeIfAbsent(tree.skill(), s -> new ArrayList<>()).add(ref);
                }
            });
            bySkill.values().forEach(list -> list.sort((a, b) -> Integer.compare(a.perk().level(), b.perk().level())));
            perks = index;
            perksBySkill = bySkill;
        });
        XP_SOURCES.onReload(registry -> {
            Map<XpSource.Trigger, List<XpSource>> map = new EnumMap<>(XpSource.Trigger.class);
            for (XpSource source : registry.values()) {
                map.computeIfAbsent(source.trigger(), t -> new ArrayList<>()).add(source);
            }
            xpByTrigger = map;
        });
        REQUIREMENTS.onReload(registry -> {
            Map<RequirementSet.Kind, List<RequirementSet.Requirement>> map = new EnumMap<>(RequirementSet.Kind.class);
            for (RequirementSet set : registry.values()) {
                for (RequirementSet.Requirement req : set.requirements()) {
                    map.computeIfAbsent(req.kind(), k -> new ArrayList<>()).add(req);
                }
            }
            requirementsByKind = map;
        });
    }

    private SkillsData() {}

    /** Forces class loading (and therefore registry creation) during mod construction. */
    public static void init() {}

    public static PerkRef perk(ResourceLocation id) {
        return perks.get(id);
    }

    public static List<PerkRef> perks(Skill skill) {
        return perksBySkill.getOrDefault(skill, Collections.emptyList());
    }

    public static List<XpSource> xpSources(XpSource.Trigger trigger) {
        return xpByTrigger.getOrDefault(trigger, Collections.emptyList());
    }

    /** Best (most specific) XP entry for a block in one table, or null. */
    public static XpSource.Entry bestEntry(XpSource source, BlockState state) {
        XpSource.Entry best = null;
        for (XpSource.Entry entry : source.entries()) {
            if ((best == null || entry.match().specificity() > best.match().specificity()) && entry.match().matches(state)) {
                best = entry;
            }
        }
        return best;
    }

    public static XpSource.Entry bestEntry(XpSource source, ItemStack stack) {
        XpSource.Entry best = null;
        for (XpSource.Entry entry : source.entries()) {
            if ((best == null || entry.match().specificity() > best.match().specificity()) && entry.match().matches(stack)) {
                best = entry;
            }
        }
        return best;
    }

    public static List<RequirementSet.Requirement> requirements(RequirementSet.Kind kind, ItemStack stack) {
        List<RequirementSet.Requirement> out = new ArrayList<>(1);
        for (RequirementSet.Requirement req : requirementsByKind.getOrDefault(kind, Collections.emptyList())) {
            if (req.match().matches(stack)) out.add(req);
        }
        return out;
    }

    public static List<RequirementSet.Requirement> requirements(BlockState state) {
        List<RequirementSet.Requirement> out = new ArrayList<>(1);
        for (RequirementSet.Requirement req : requirementsByKind.getOrDefault(RequirementSet.Kind.BLOCK, Collections.emptyList())) {
            if (req.match().matches(state)) out.add(req);
        }
        return out;
    }

    /** Whether any break_block XP table cares about this block (used by the anti-exploit tracker). */
    public static boolean grantsBreakXp(BlockState state) {
        for (XpSource source : xpSources(XpSource.Trigger.BREAK_BLOCK)) {
            XpSource.Entry entry = bestEntry(source, state);
            if (entry != null && !entry.matureOnly()) return true;
        }
        return false;
    }
}

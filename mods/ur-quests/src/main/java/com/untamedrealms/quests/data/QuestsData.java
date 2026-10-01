package com.untamedrealms.quests.data;

import com.untamedrealms.core.data.DataRegistry;
import com.untamedrealms.quests.UntamedQuests;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public final class QuestsData {
    public static final DataRegistry<QuestDef> QUESTS =
            DataRegistry.create(UntamedQuests.id("quests"), "urquests/quests", QuestDef.CODEC, true);

    private QuestsData() {}

    public static void init() {}

    public static QuestDef get(ResourceLocation id) {
        return QUESTS.getOrNull(id);
    }

    public static List<ResourceLocation> inPool(String pool) {
        return QUESTS.entries().entrySet().stream()
                .filter(e -> e.getValue().pool().map(pool::equals).orElse(false))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }
}

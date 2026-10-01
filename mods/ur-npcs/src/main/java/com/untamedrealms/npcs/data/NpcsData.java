package com.untamedrealms.npcs.data;

import com.untamedrealms.core.data.DataRegistry;
import com.untamedrealms.npcs.UntamedNpcs;

public final class NpcsData {
    public static final DataRegistry<NpcDef> NPCS = DataRegistry.create(UntamedNpcs.id("npcs"), "urnpcs/npcs", NpcDef.CODEC, true);
    public static final DataRegistry<DialogueDef> DIALOGUES = DataRegistry.create(UntamedNpcs.id("dialogues"), "urnpcs/dialogues", DialogueDef.CODEC, false);
    public static final DataRegistry<ShopDef> SHOPS = DataRegistry.create(UntamedNpcs.id("shops"), "urnpcs/shops", ShopDef.CODEC, false);
    public static final DataRegistry<SettlementDef> SETTLEMENTS = DataRegistry.create(UntamedNpcs.id("settlements"), "urnpcs/settlements", SettlementDef.CODEC, false);

    private NpcsData() {}

    public static void init() {}
}

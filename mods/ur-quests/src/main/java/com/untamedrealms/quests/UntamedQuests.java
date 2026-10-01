package com.untamedrealms.quests;

import com.mojang.logging.LogUtils;
import com.untamedrealms.core.registry.CoreItems;
import com.untamedrealms.quests.block.NoticeBoardBlock;
import com.untamedrealms.quests.data.QuestLog;
import com.untamedrealms.quests.data.QuestsData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Untamed Realms: Quests. Data-driven main story, side quests and radiant bounties; a journal (J),
 * an on-screen tracker and town notice boards. Exposes {@link com.untamedrealms.quests.engine.QuestApi}.
 */
@Mod(UntamedQuests.MODID)
public final class UntamedQuests {
    public static final String MODID = "urquests";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);
    public static final Supplier<AttachmentType<QuestLog>> QUEST_LOG = ATTACHMENTS.register("quest_log",
            () -> AttachmentType.serializable(QuestLog::new).copyOnDeath().build());

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredBlock<NoticeBoardBlock> NOTICE_BOARD = BLOCKS.register("notice_board",
            () -> new NoticeBoardBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final DeferredItem<BlockItem> NOTICE_BOARD_ITEM = ITEMS.registerSimpleBlockItem("notice_board", NOTICE_BOARD);
    public static final DeferredItem<Item> QUEST_SCROLL = ITEMS.registerSimpleItem("quest_scroll");
    public static final DeferredItem<Item> BOUNTY_NOTICE = ITEMS.registerSimpleItem("bounty_notice");

    public UntamedQuests(IEventBus modBus, ModContainer container) {
        ATTACHMENTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        QuestsData.init();
        container.registerConfig(ModConfig.Type.SERVER, QuestsConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, QuestsClientConfig.SPEC);
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, event -> {
            if (event.getTabKey().equals(CoreItems.TAB_KEY)) event.accept(NOTICE_BOARD_ITEM.get());
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}

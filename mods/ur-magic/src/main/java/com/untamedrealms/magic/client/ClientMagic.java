package com.untamedrealms.magic.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.untamedrealms.magic.UntamedMagic;
import com.untamedrealms.magic.data.SpellBook;
import com.untamedrealms.magic.data.SpellDef;
import com.untamedrealms.magic.network.MagicNetwork;
import com.untamedrealms.skills.api.Skill;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public final class ClientMagic {
    public static final KeyMapping CAST = new KeyMapping("key.urmagic.cast", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.untamedrealms");
    public static final KeyMapping NEXT = new KeyMapping("key.urmagic.next", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.untamedrealms");
    public static final KeyMapping WHEEL = new KeyMapping("key.urmagic.wheel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, "key.categories.untamedrealms");
    public static final KeyMapping SPELLBOOK = new KeyMapping("key.urmagic.spellbook", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.untamedrealms");

    static final SpellBook BOOK = new SpellBook();
    /** Client game time at which each spell becomes ready. */
    static final Map<ResourceLocation, Long> READY = new HashMap<>();

    private ClientMagic() {}

    public static void onSync(CompoundTag book, CompoundTag cooldowns, HolderLookup.Provider registries) {
        BOOK.deserializeNBT(registries, book);
        long now = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
        READY.clear();
        for (String key : cooldowns.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) READY.put(id, now + cooldowns.getInt(key));
        }
    }

    public static ResourceLocation icon(ResourceLocation spell) {
        return ResourceLocation.fromNamespaceAndPath(spell.getNamespace(), "textures/gui/spell/" + spell.getPath() + ".png");
    }

    public static int schoolColor(Skill school) {
        return switch (school) {
            case DESTRUCTION -> 0xFFF78A2A;
            case RESTORATION -> 0xFFF8E874;
            case ALTERATION -> 0xFF8C7AE2;
            case CONJURATION -> 0xFFA35EC8;
            case ILLUSION -> 0xFF36A8C4;
            default -> 0xFFE6E1D3;
        };
    }

    @EventBusSubscriber(modid = UntamedMagic.MODID, value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(CAST);
            event.register(NEXT);
            event.register(SPELLBOOK);
            event.register(WHEEL);
        }

        @SubscribeEvent
        public static void registerLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(UntamedMagic.id("spell_hud"), SpellHud::render);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(UntamedMagic.SPELL_PROJECTILE.get(), NoopRenderer::new);
        }

        /** Spell tomes pick their cover colour from the spell's school. */
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ItemProperties.register(UntamedMagic.SPELL_TOME.get(), UntamedMagic.id("school"), (stack, level, entity, seed) -> {
                SpellDef def = UntamedMagic.SPELLS.getOrNull(stack.get(UntamedMagic.SPELL.get()));
                if (def == null) return 0f;
                return switch (def.school()) {
                    case RESTORATION -> 0.2f;
                    case ALTERATION -> 0.3f;
                    case CONJURATION -> 0.4f;
                    case ILLUSION -> 0.5f;
                    default -> 0.1f;
                };
            }));
        }

        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            while (CAST.consumeClick()) if (mc.screen == null) PacketDistributor.sendToServer(new MagicNetwork.Cast(-1));
            while (NEXT.consumeClick()) if (mc.screen == null) PacketDistributor.sendToServer(new MagicNetwork.Select(1, true));
            while (WHEEL.consumeClick()) if (mc.screen == null) mc.setScreen(new SpellWheelScreen(WHEEL));
            while (SPELLBOOK.consumeClick()) if (mc.screen == null) mc.setScreen(new SpellbookScreen());
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            BOOK.deserializeNBT(null, new CompoundTag());
            READY.clear();
        }
    }
}

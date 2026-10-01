package com.untamedrealms.skills.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class SkillsKeys {
    public static final String CATEGORY = "key.categories.untamedrealms";
    public static final KeyMapping OPEN_SKILLS = new KeyMapping("key.urskills.open_skills",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);

    private SkillsKeys() {}
}

package com.untamedrealms.classes.gametest;

import com.untamedrealms.classes.UntamedClasses;
import com.untamedrealms.classes.data.ClassDef;
import com.untamedrealms.classes.data.ClassesData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

@GameTestHolder(UntamedClasses.MODID)
@PrefixGameTestTemplate(false)
public class ClassesGameTests {
    @GameTest(template = "arena")
    public static void classesAndBirthsignsLoad(GameTestHelper helper) {
        helper.assertTrue(ClassesData.CLASSES.entries().size() >= 10, "expected the default classes, got " + ClassesData.CLASSES.entries().size());
        helper.assertTrue(ClassesData.BIRTHSIGNS.entries().size() >= 13, "expected the default birthsigns");
        for (Map.Entry<ResourceLocation, ClassDef> e : ClassesData.CLASSES.entries().entrySet()) {
            helper.assertTrue(!e.getValue().skills().isEmpty(), e.getKey() + " has no starting skills");
            helper.assertTrue(!e.getValue().loadout().resolve(helper.getLevel().registryAccess()).isEmpty(), e.getKey() + " has an empty loadout");
        }
        helper.succeed();
    }
}

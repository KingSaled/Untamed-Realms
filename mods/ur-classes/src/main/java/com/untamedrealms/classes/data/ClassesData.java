package com.untamedrealms.classes.data;

import com.untamedrealms.classes.UntamedClasses;
import com.untamedrealms.core.data.DataRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ClassesData {
    public static final DataRegistry<ClassDef> CLASSES =
            DataRegistry.create(UntamedClasses.id("classes"), "urclasses/classes", ClassDef.CODEC, true);
    public static final DataRegistry<Birthsign> BIRTHSIGNS =
            DataRegistry.create(UntamedClasses.id("birthsigns"), "urclasses/birthsigns", Birthsign.CODEC, true);

    private ClassesData() {}

    public static void init() {}

    public static List<Map.Entry<ResourceLocation, ClassDef>> sortedClasses() {
        return CLASSES.entries().entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<ResourceLocation, ClassDef> e) -> e.getValue().order()).thenComparing(Map.Entry::getKey))
                .toList();
    }

    public static List<Map.Entry<ResourceLocation, Birthsign>> sortedBirthsigns() {
        return BIRTHSIGNS.entries().entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<ResourceLocation, Birthsign> e) -> e.getValue().order()).thenComparing(Map.Entry::getKey))
                .toList();
    }
}

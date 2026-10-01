package com.untamedrealms.npcs.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A dialogue tree, from {@code data/<ns>/urnpcs/dialogues/<id>.json}.
 * <ul>
 *   <li>{@code entry}: candidates for the opening node; the first whose conditions pass is used.</li>
 *   <li>{@code nodes}: id -> what the NPC says plus the player's response options.</li>
 * </ul>
 * Options can carry conditions, actions, a Speech check and a {@code next} node (absent = end).
 */
public record DialogueDef(List<Entry> entry, Map<String, Node> nodes) {
    public static final Codec<DialogueDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Entry.CODEC.listOf().fieldOf("entry").forGetter(DialogueDef::entry),
            Codec.unboundedMap(Codec.STRING, Node.CODEC).fieldOf("nodes").forGetter(DialogueDef::nodes)
    ).apply(inst, DialogueDef::new));

    public record Entry(List<Condition> conditions, String node) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Condition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(Entry::conditions),
                Codec.STRING.fieldOf("node").forGetter(Entry::node)
        ).apply(inst, Entry::new));
    }

    public record Node(Component text, List<Option> options, List<Action> actions) {
        public static final Codec<Node> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ComponentSerialization.CODEC.fieldOf("text").forGetter(Node::text),
                Option.CODEC.listOf().optionalFieldOf("options", List.of()).forGetter(Node::options),
                Action.CODEC.listOf().optionalFieldOf("actions", List.of()).forGetter(Node::actions)
        ).apply(inst, Node::new));
    }

    public record Option(Component text, List<Condition> conditions, List<Action> actions, Optional<String> next,
                         Optional<Check> check) {
        public static final Codec<Option> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ComponentSerialization.CODEC.fieldOf("text").forGetter(Option::text),
                Condition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(Option::conditions),
                Action.CODEC.listOf().optionalFieldOf("actions", List.of()).forGetter(Option::actions),
                Codec.STRING.optionalFieldOf("next").forGetter(Option::next),
                Check.CODEC.optionalFieldOf("check").forGetter(Option::check)
        ).apply(inst, Option::new));
    }

    /** A skill check (usually Speech): on success go to {@code next}, otherwise to {@code fail}. */
    public record Check(String skill, int level, String fail) {
        public static final Codec<Check> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.optionalFieldOf("skill", "speech").forGetter(Check::skill),
                Codec.INT.fieldOf("level").forGetter(Check::level),
                Codec.STRING.fieldOf("fail").forGetter(Check::fail)
        ).apply(inst, Check::new));
    }

    /**
     * Condition: quest_active / quest_completed / quest_not_started / quest_available / quest_stage
     * (quest + value), skill (target skill + value level), level, has_item (target + value count),
     * coins (value), flag / not_flag (target). Set "negate": true to invert.
     */
    public record Condition(String type, String target, int value, boolean negate) {
        public static final Codec<Condition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("type").forGetter(Condition::type),
                Codec.STRING.optionalFieldOf("target", "").forGetter(Condition::target),
                Codec.INT.optionalFieldOf("value", 0).forGetter(Condition::value),
                Codec.BOOL.optionalFieldOf("negate", false).forGetter(Condition::negate)
        ).apply(inst, Condition::new));
    }

    /**
     * Action: start_quest (target), give_item (item), take_item (target + value), give_coins / take_coins
     * (value), open_shop, train, set_flag / clear_flag (target), give_xp (target skill + value).
     */
    public record Action(String type, String target, int value, Optional<Dynamic<?>> item) {
        public static final Codec<Action> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("type").forGetter(Action::type),
                Codec.STRING.optionalFieldOf("target", "").forGetter(Action::target),
                Codec.INT.optionalFieldOf("value", 0).forGetter(Action::value),
                Codec.PASSTHROUGH.optionalFieldOf("item").forGetter(Action::item)
        ).apply(inst, Action::new));
    }
}

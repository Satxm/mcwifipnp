package io.github.satxm.mcwifipnp.commands;

import java.net.StandardProtocolFamily;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * The "Proper" Enum argument implementation (like the one in Forge) requires
 * registration. Here is a simple workaround.
 * <p>
 * For simple string literals that don't have a enum backend, use
 * {@link EnumArgument#literals} and
 * {@link EnumArgument#getArgValue(CommandContext, int)}.
 * <p>
 * Otherwise, call the constructors to create a helper class for the enum, then
 * use {@link EnumArgument#appendTo(LiteralArgumentBuilder, UnaryOperator)} and
 * {@link EnumArgument#valueOf(CommandContext, int)}
 * <p>
 * The only drawback is that you need to find out the literal index manually.
 *
 * @param <T> the Enum type
 */
public class EnumArgument<T extends Enum<T>> {
	public final static EnumArgument<StandardProtocolFamily> IP_FAMILY =
		new EnumArgument<>(StandardProtocolFamily.class);

	private final Function<T, String> namingFunction;
	private final Map<String, T> mapping = new HashMap<>();

	/**
	 * Use all lower case and replace _ with - for naming.
	 *
	 * @param enumClass
	 */
	public EnumArgument(Class<T> enumClass) {
		this(enumClass, (t) -> t.name().toLowerCase().replace('_', '-'));
	}

	/**
	 * @param enumClass
	 * @param namingFunction how to map each of the enum to a string argument. An
	 *                       error will be thrown if multiple enum have the same
	 *                       mapped name.
	 */
	public EnumArgument(Class<T> enumClass, Function<T, String> namingFunction) {
		this.namingFunction = namingFunction;

		for (T value: enumClass.getEnumConstants()) {
			String name = namingFunction.apply(value);
			T existing = this.mapping.get(name);

			if (existing != null) {
				throw new RuntimeException(existing + " and " + value + " has the same mapped key: " + name);
			}

			this.mapping.put(name, value);
		}
	}

	public String nameOf(T value) {
		return this.namingFunction.apply(value);
	}

	public T valueOf(String argVal) {
		return this.mapping.get(argVal);
	}

	public T valueOf(CommandContext<CommandSourceStack> context, int index) {
		return this.mapping.get(getArgValue(context, index));
	}

	public LiteralArgumentBuilder<CommandSourceStack> appendTo(
		LiteralArgumentBuilder<CommandSourceStack> parent,
		UnaryOperator<LiteralArgumentBuilder<CommandSourceStack>> operation) {

		return literals(parent, this.mapping.keySet().toArray(new String[0])).apply(operation);
	}

	/**
	 * <pre>
	 *   EnumArgument.literals(parent, "enum1", "enum2").apply(enumOption -> enumOption.THE_OPERATION())
	 * </pre>
	 * <p> is equivalent to:</p>
	 * <pre>
	 *   parent
	 *       .then(Commands.literal("enum1").THE_OPERATION())
	 *       .then(Commands.literal("enum2").THE_OPERATION())
	 * </pre>
	 *
	 * @param parent
	 * @param values
	 * @return a builder function that accepts an operation (THE_OPERATION) to be performed on each enum literals
	 */
	public static Function<UnaryOperator<LiteralArgumentBuilder<CommandSourceStack>>, LiteralArgumentBuilder<CommandSourceStack>> literals(
		LiteralArgumentBuilder<CommandSourceStack> parent, String... values
	) {

		return (operation) -> {
			LiteralArgumentBuilder<CommandSourceStack> result = parent;
			for (String value: values) {
				result = result.then(operation.apply(Commands.literal(value)));
			}
			return result;
		};
	}

	/**
	 * Get the literal string value from a command context.
	 *
	 * @param context
	 * @param index The index of the literal, from right to left, starting from 0.
	 * @return the literal string value, null if not applicable.
	 */
	public static String getArgValue(CommandContext<CommandSourceStack> context, int index) {
		List<ParsedCommandNode<CommandSourceStack>> nodes = context.getNodes();
		ParsedCommandNode<CommandSourceStack> node = nodes.get(nodes.size() - 1 - index);
		if (node.getNode() instanceof LiteralCommandNode literal) {
			return literal.getLiteral();
		} else {
			return null;
		}
	}
}
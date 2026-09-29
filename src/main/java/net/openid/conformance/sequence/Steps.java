package net.openid.conformance.sequence;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.ExecutionContext;
import net.openid.conformance.testmodule.IterateEnvironmentArray;
import net.openid.conformance.testmodule.TestExecutionUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Factories for composing test steps as values rather than {@link ConditionSequence} subclasses.
 *
 * <p>Every unit returned here is immutable, so it can be stored in a field, reused, or returned from a profile
 * behaviour method. Units compose with the existing ones: condition calls, {@code exec()} commands and
 * {@link ConditionSequence}s can all be children, and these units can be passed to {@code call()} in test modules
 * and sequences.
 *
 * <pre>{@code
 * TestExecutionUnit tokenSteps = Steps.of("CallTokenEndpoint",
 *     condition(CreateTokenEndpointRequest.class),
 *     Steps.when("client uses DPoP", env -> env.getObject("client_dpop_key") != null,
 *         condition(AddDpopHeaderForTokenEndpointRequest.class)),
 *     condition(CallTokenEndpoint.class));
 *
 * call(Steps.modify(tokenSteps)
 *     .replace(CallTokenEndpoint.class, condition(CallTokenEndpointAllowingErrors.class))
 *     .build());
 * }</pre>
 *
 * <p>Choices that depend on the selected variants should be ordinary Java {@code if}s while building the steps,
 * so the steps are fixed per variant. {@link #when} is for choices that depend on the environment at run time.
 */
public final class Steps {

	private static final Logger logger = LoggerFactory.getLogger(Steps.class);

	private Steps() {
	}

	/**
	 * A named sequence of units, run in order. The name is only used for server logging and may be null.
	 *
	 * <p>{@link ConditionSequence#replace} and the other legacy modifiers don't look inside these units; use
	 * {@link #modify} instead, which also reaches into legacy sequences.
	 */
	public static Sequence of(String name, TestExecutionUnit... units) {
		return new Sequence(name, Arrays.asList(units));
	}

	/**
	 * A named sequence of units, run in order. The name is only used for server logging.
	 */
	public static Sequence of(String name, List<? extends TestExecutionUnit> units) {
		return new Sequence(name, units);
	}

	/**
	 * Run {@code then} if the predicate holds when this unit is reached. Otherwise an entry is written to the test
	 * log saying the steps were skipped, so the log shows why they did not run.
	 *
	 * <p>This must not be used to turn a check that is mandatory under the selected profile into a silent skip.
	 *
	 * @param description what the predicate checks, e.g. "client uses DPoP"; used in the test log
	 */
	public static When when(String description, Predicate<Environment> predicate, TestExecutionUnit then) {
		return new When(description, predicate, then, null);
	}

	/**
	 * Run {@code then} if the predicate holds when this unit is reached, {@code otherwise} if not.
	 *
	 * @param description what the predicate checks, e.g. "client uses DPoP"; used in the test log
	 */
	public static When when(String description, Predicate<Environment> predicate, TestExecutionUnit then, TestExecutionUnit otherwise) {
		return new When(description, predicate, then, Objects.requireNonNull(otherwise, "otherwise"));
	}

	/**
	 * Change the environment without writing to the test log, like {@code exec()} commands. The name makes the
	 * action addressable by {@link Modifier}; use a condition created with {@code check()} for anything that should
	 * be visible in the test log.
	 */
	public static Action action(String name, Consumer<Environment> action) {
		return new Action(name, action);
	}

	/**
	 * Run the units inside a block of the test log.
	 */
	public static Block block(String label, TestExecutionUnit... units) {
		return new Block(label, of(label, units));
	}

	/**
	 * Run {@code body} once for every element of the array at {@code sourceObject.sourcePath}. Configure how the
	 * current element is exposed with the returned builder, e.g. {@code .currentString("credential")}.
	 */
	public static IterateEnvironmentArray forEach(String sourceObject, String sourcePath, TestExecutionUnit body) {
		return new IterateEnvironmentArray(sourceObject, sourcePath, body);
	}

	/**
	 * Start modifying a unit. The modifications apply to condition calls anywhere below the unit, including inside
	 * nested sequences, {@link #when} branches, blocks and {@link #forEach} bodies. They are applied when the
	 * modified unit runs; a modification whose target is not found stops the test, as it does for
	 * {@link ConditionSequence}.
	 */
	public static Modifier modify(TestExecutionUnit unit) {
		return new Modifier(unit);
	}

	public static final class Sequence implements TestExecutionUnit {

		private final String name;
		private final List<TestExecutionUnit> units;
		private TestExecutionUnit finalStep;

		private Sequence(String name, List<? extends TestExecutionUnit> units) {
			this.name = name;
			this.units = List.copyOf(units);
		}

		public String getName() {
			return name;
		}

		public List<TestExecutionUnit> getUnits() {
			return units;
		}

		@Override
		public void run(ExecutionContext context) {
			try {
				if (name == null) {
					units.forEach(context::run);
					return;
				}
				logger.info(context.getTestId() + ":   Starting steps " + name);
				units.forEach(context::run);
				logger.info(context.getTestId() + ":   End of steps " + name);
			} finally {
				if (finalStep != null) {
						context.run(finalStep);
					}
			}

		}

		@Override
		public TestExecutionUnit transform(ExecutionContext context, UnaryOperator<TestExecutionUnit> leafMapper) {
			List<TestExecutionUnit> transformed = new ArrayList<>(units.size());
			for (TestExecutionUnit unit : units) {
				transformed.add(unit.transform(context, leafMapper));
			}
			return new Sequence(name, transformed);
		}

		public TestExecutionUnit doFinal(TestExecutionUnit unregisteringClient) {
			this.finalStep = unregisteringClient;
			return this;
		}

	}

	public static final class When implements TestExecutionUnit, DataUtils {

		private final String description;
		private final Predicate<Environment> predicate;
		private final TestExecutionUnit then;
		private final TestExecutionUnit otherwise;

		private When(String description, Predicate<Environment> predicate, TestExecutionUnit then, TestExecutionUnit otherwise) {
			this.description = Objects.requireNonNull(description, "description");
			this.predicate = Objects.requireNonNull(predicate, "predicate");
			this.then = Objects.requireNonNull(then, "then");
			this.otherwise = otherwise;
		}

		@Override
		public void run(ExecutionContext context) {
			if (predicate.test(context.getEnv())) {
				context.run(then);
			} else if (otherwise != null) {
				context.run(otherwise);
			} else {
				context.getEventLog().log(description, args(
					"msg", "Skipped because '" + description + "' does not hold"));
			}
		}

		@Override
		public TestExecutionUnit transform(ExecutionContext context, UnaryOperator<TestExecutionUnit> leafMapper) {
			return new When(description, predicate, then.transform(context, leafMapper),
				otherwise == null ? null : otherwise.transform(context, leafMapper));
		}
	}

	public static final class Action implements TestExecutionUnit {

		private final String name;
		private final Consumer<Environment> effect;

		private Action(String name, Consumer<Environment> effect) {
			this.name = Objects.requireNonNull(name, "name");
			this.effect = Objects.requireNonNull(effect, "effect");
		}

		@Override
		public Object key() {
			return name;
		}

		@Override
		public void run(ExecutionContext context) {
			effect.accept(context.getEnv());
		}
	}

	public static final class Block implements TestExecutionUnit {

		private final String label;
		private final TestExecutionUnit body;

		private Block(String label, TestExecutionUnit body) {
			this.label = Objects.requireNonNull(label, "label");
			this.body = body;
		}

		@Override
		public void run(ExecutionContext context) {
			context.getEventLog().startBlock(label);
			try {
				context.run(body);
			} finally {
				context.getEventLog().endBlock();
			}
		}

		@Override
		public TestExecutionUnit transform(ExecutionContext context, UnaryOperator<TestExecutionUnit> leafMapper) {
			return new Block(label, body.transform(context, leafMapper));
		}
	}

	/**
	 * Collects replace/skip/insert modifications for a unit; {@link #build()} returns the modified unit, leaving the
	 * original unchanged. Targets are condition classes, or names for functional conditions and actions.
	 */
	public static final class Modifier {

		private final TestExecutionUnit target;
		private final Map<Object, TestExecutionUnit> replacements = new LinkedHashMap<>();
		private final Map<Object, String> skips = new LinkedHashMap<>();
		private final Map<Object, TestExecutionUnit> insertBefore = new LinkedHashMap<>();
		private final Map<Object, TestExecutionUnit> insertAfter = new LinkedHashMap<>();
		private final List<TestExecutionUnit> before = new ArrayList<>();
		private final List<TestExecutionUnit> after = new ArrayList<>();

		private Modifier(TestExecutionUnit target) {
			this.target = Objects.requireNonNull(target, "target");
		}

		public Modifier replace(Class<? extends Condition> condition, TestExecutionUnit replacement) {
			replacements.put(condition, replacement);
			return this;
		}

		public Modifier replace(String name, TestExecutionUnit replacement) {
			replacements.put(name, replacement);
			return this;
		}

		public Modifier skip(Class<? extends Condition> condition, String message) {
			skips.put(condition, message);
			return this;
		}

		public Modifier skip(String name, String message) {
			skips.put(name, message);
			return this;
		}

		public Modifier insertBefore(Class<? extends Condition> condition, TestExecutionUnit unit) {
			insertBefore.put(condition, unit);
			return this;
		}

		public Modifier insertBefore(String name, TestExecutionUnit unit) {
			insertBefore.put(name, unit);
			return this;
		}

		public Modifier insertAfter(Class<? extends Condition> condition, TestExecutionUnit unit) {
			insertAfter.put(condition, unit);
			return this;
		}

		public Modifier insertAfter(String name, TestExecutionUnit unit) {
			insertAfter.put(name, unit);
			return this;
		}

		public Modifier butFirst(TestExecutionUnit... units) {
			before.addAll(Arrays.asList(units));
			return this;
		}

		public Modifier then(TestExecutionUnit... units) {
			after.addAll(Arrays.asList(units));
			return this;
		}

		public Modified build() {
			return new Modified(target, replacements, skips, insertBefore, insertAfter, before, after);
		}
	}

	public static final class Modified implements TestExecutionUnit {

		private final TestExecutionUnit target;
		private final Map<Object, TestExecutionUnit> replacements;
		private final Map<Object, String> skips;
		private final Map<Object, TestExecutionUnit> insertBefore;
		private final Map<Object, TestExecutionUnit> insertAfter;
		private final List<TestExecutionUnit> before;
		private final List<TestExecutionUnit> after;

		private Modified(TestExecutionUnit target,
			Map<Object, TestExecutionUnit> replacements, Map<Object, String> skips,
			Map<Object, TestExecutionUnit> insertBefore, Map<Object, TestExecutionUnit> insertAfter,
			List<TestExecutionUnit> before, List<TestExecutionUnit> after) {
			this.target = target;
			this.replacements = Collections.unmodifiableMap(new LinkedHashMap<>(replacements));
			this.skips = Collections.unmodifiableMap(new LinkedHashMap<>(skips));
			this.insertBefore = Collections.unmodifiableMap(new LinkedHashMap<>(insertBefore));
			this.insertAfter = Collections.unmodifiableMap(new LinkedHashMap<>(insertAfter));
			this.before = List.copyOf(before);
			this.after = List.copyOf(after);
		}

		@Override
		public void run(ExecutionContext context) {
			context.run(resolve(context));
		}

		@Override
		public TestExecutionUnit transform(ExecutionContext context, UnaryOperator<TestExecutionUnit> leafMapper) {
			return resolve(context).transform(context, leafMapper);
		}

		/**
		 * Apply the modifications to the target.
		 *
		 * @return the modified steps
		 * @throws IllegalStateException if a modification's target is not found
		 */
		public Sequence resolve(ExecutionContext context) {
			Set<Object> matched = new HashSet<>();
			TestExecutionUnit modified = target.transform(context, unit -> {
				Object key = unit.key();
				if (key == null) {
					return unit;
				}
				TestExecutionUnit result = unit;
				if (replacements.containsKey(key)) {
					result = replacements.get(key);
					matched.add(key);
				}
				if (skips.containsKey(key)) {
					result = new SkippedCondition(keyName(key), skips.get(key));
					matched.add(key);
				}
				if (insertBefore.containsKey(key)) {
					result = of(null, insertBefore.get(key), result);
					matched.add(key);
				}
				if (insertAfter.containsKey(key)) {
					result = of(null, result, insertAfter.get(key));
					matched.add(key);
				}
				return result;
			});

			checkTargetsFound(replacements.keySet(), matched, "replacement");
			checkTargetsFound(skips.keySet(), matched, "skip");
			checkTargetsFound(insertBefore.keySet(), matched, "insertion");
			checkTargetsFound(insertAfter.keySet(), matched, "insertion");

			List<TestExecutionUnit> units = new ArrayList<>(before);
			units.add(modified);
			units.addAll(after);
			return of(null, units);
		}

		private static void checkTargetsFound(Set<Object> targets, Set<Object> matched, String modification) {
			for (Object target : targets) {
				if (!matched.contains(target)) {
					throw new IllegalStateException("%s requested for missing condition: %s".formatted(modification, keyName(target)));
				}
			}
		}
	}

	private static String keyName(Object key) {
		if (key instanceof Class<?> clazz) {
			return clazz.getSimpleName();
		}
		return String.valueOf(key);
	}
}

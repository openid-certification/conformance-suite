package net.openid.conformance.sequence;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.Command;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import net.openid.conformance.testmodule.ConditionSequenceCallBuilder;
import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.TestExecutionUnit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class AbstractConditionSequence implements ConditionSequence, DataUtils {

	private List<TestExecutionUnit> callables = new ArrayList<>();
	private Map<Class<? extends Condition>, TestExecutionUnit> replacements = new HashMap<>();
	private Map<Class<? extends Condition>, String> skips = new HashMap<>();
	private Map<Class<? extends Condition>, TestExecutionUnit> insertBefore = new HashMap<>();
	private Map<Class<? extends Condition>, TestExecutionUnit> insertAfter = new HashMap<>();
	private List<TestExecutionUnit> before = new ArrayList<>();
	private List<TestExecutionUnit> after = new ArrayList<>();

	/**
	 * Add the builder to the list of calls to be made when this sequence is executed
	 *
	 * @param builder
	 */
	protected void call(TestExecutionUnit builder) {
		callables.add(builder);
	}

	/**
	 * Add the list of builders to the list of calls to be made when this sequence is executed
	 */
	protected void call(List<TestExecutionUnit> builders) {
		callables.addAll(builders);
	}

	/**
	 * Create a call to a new condition
	 */
	protected ConditionCallBuilder condition(Class<? extends Condition> conditionClass) {
		return new ConditionCallBuilder(conditionClass);
	}

	/**
	 * Create a call to a set of execution parameters
	 * @return
	 */
	protected Command exec() {
		return new Command();
	}

	protected ConditionSequenceCallBuilder sequence(Class<? extends ConditionSequence> conditionSequenceClass) {
		return new ConditionSequenceCallBuilder(conditionSequenceClass);
	}

	protected ConditionSequenceCallBuilder sequence(Supplier<? extends ConditionSequence> conditionSequenceConstructor) {
		return new ConditionSequenceCallBuilder(conditionSequenceConstructor);
	}

	protected ConditionSequence sequenceOf(TestExecutionUnit... units) {
		return new AbstractConditionSequence() {

			@Override
			public void evaluate() {
				call(Arrays.asList(units));
			}
		};
	}

	@Override
	public List<TestExecutionUnit> getTestExecutionUnits() {

		Function<TestExecutionUnit, Class<? extends Condition>> actionToConditionClass = action -> {
			if (action instanceof ConditionCallBuilder) {
				return ((ConditionCallBuilder) action).getConditionClass();
			} else if (action instanceof Condition) {
				return action.getClass().asSubclass(Condition.class);
			} else {
				return null;
			}
		};

		// First check that all modifications refer to a condition in this sequence
		Set<Class<? extends Condition>> conditionClasses = this.callables.stream()
				.map(actionToConditionClass)
				.filter(c -> c != null)
				.collect(Collectors.toSet());
		replacements.keySet().forEach(conditionClass -> {
			if (!conditionClasses.contains(conditionClass)) {
				throw new RuntimeException(String.format("%s: replacement requested for missing condition: %s",
						this.getClass().getSimpleName(), conditionClass.getSimpleName()));
			}
		});
		skips.keySet().forEach(conditionClass -> {
			if (!conditionClasses.contains(conditionClass)) {
				throw new RuntimeException(String.format("%s: skip requested for missing condition: %s",
						this.getClass().getSimpleName(), conditionClass.getSimpleName()));
			}
		});
		insertBefore.keySet().forEach(conditionClass -> {
			if (!conditionClasses.contains(conditionClass)) {
				throw new RuntimeException(String.format("%s: insertion requested for missing condition: %s",
						this.getClass().getSimpleName(), conditionClass.getSimpleName()));
			}
		});
		insertAfter.keySet().forEach(conditionClass -> {
			if (!conditionClasses.contains(conditionClass)) {
				throw new RuntimeException(String.format("%s: insertion requested for missing condition: %s",
						this.getClass().getSimpleName(), conditionClass.getSimpleName()));
			}
		});

		List<TestExecutionUnit> units = new ArrayList<>();
		units.addAll(before);
		units.addAll(this.callables.stream()
				.map((action) -> {
					Class<? extends Condition> conditionClass = actionToConditionClass.apply(action);
					if (conditionClass != null) {
						if (replacements.containsKey(conditionClass)) {
							action = replacements.get(conditionClass);
						}
						if (skips.containsKey(conditionClass)) {
							action = new SkippedCondition(conditionClass.getSimpleName(), skips.get(conditionClass));
						}
						if (insertBefore.containsKey(conditionClass)) {
							action = sequenceOf(insertBefore.get(conditionClass), action);
						}
						if (insertAfter.containsKey(conditionClass)) {
							action = sequenceOf(action, insertAfter.get(conditionClass));
						}
					}
					// otherwise pass through
					return action;
				})
				.collect(Collectors.toList()));
		units.addAll(after);

		return units;
	}

	@Override
	public ConditionSequence replace(Class<? extends Condition> conditionToReplace, TestExecutionUnit builder) {
		this.replacements.put(conditionToReplace, builder);
		return this;
	}

	@Override
	public ConditionSequence skip(Class<? extends Condition> conditionToSkip, String message) {
		this.skips.put(conditionToSkip, message);
		return this;
	}

	@Override
	public ConditionSequence insertBefore(Class<? extends Condition> conditionToInsertAt, TestExecutionUnit builder) {
		this.insertBefore.put(conditionToInsertAt, builder);
		return this;
	}

	@Override
	public ConditionSequence insertAfter(Class<? extends Condition> conditionToInsertAfter, TestExecutionUnit builder) {
		this.insertAfter.put(conditionToInsertAfter, builder);
		return this;
	}

	@Override
	public ConditionSequence then(TestExecutionUnit... builders) {
		this.after.addAll(Arrays.asList(builders));
		return this;
	}

	@Override
	public ConditionSequence butFirst(TestExecutionUnit... builders) {
		this.before.addAll(Arrays.asList(builders));
		return this;
	}

	/**
	 * Create and evaluate a Condition in the current environment. Throw a @TestFailureException if the Condition fails.
	 *
	 * onFail is set to FAILURE
	 *
	 */
	protected void callAndStopOnFailure(Class<? extends Condition> conditionClass, String... requirements) {
		call(condition(conditionClass)
			.onFail(Condition.ConditionResult.FAILURE)
			.requirements(requirements));
	}

	/**
	 * Create and evaluate a Condition in the current environment. Throw a @TestFailureException if the Condition fails.
	 */
	protected void callAndStopOnFailure(Class<? extends Condition> conditionClass, Condition.ConditionResult onFail, String... requirements) {
		call(condition(conditionClass)
			.requirements(requirements)
			.onFail(onFail));
	}

	/**
	 * Create and evaluate a Condition in the current environment. Log but ignore if the Condition fails.
	 *
	 * onFail is set to INFO if requirements is null or empty, WARNING if requirements are specified
	 *
	 */
	protected void callAndContinueOnFailure(Class<? extends Condition> conditionClass, String... requirements) {
		call(condition(conditionClass)
			.onFail((requirements == null || requirements.length == 0) ? Condition.ConditionResult.INFO : Condition.ConditionResult.WARNING)
			.requirements(requirements)
			.dontStopOnFailure());
	}

	/**
	 * Create and evaluate a Condition in the current environment. Log but ignore if the Condition fails.
	 *
	 */
	protected void callAndContinueOnFailure(Class<? extends Condition> conditionClass, Condition.ConditionResult onFail, String... requirements) {
		call(condition(conditionClass)
			.requirements(requirements)
			.onFail(onFail)
			.dontStopOnFailure());
	}

}

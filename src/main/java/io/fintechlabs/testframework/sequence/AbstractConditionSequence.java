package io.fintechlabs.testframework.sequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.testmodule.ConditionCallBuilder;
import io.fintechlabs.testframework.testmodule.DataUtils;
import io.fintechlabs.testframework.testmodule.TestExecutionBuilder;
import io.fintechlabs.testframework.testmodule.TestExecutionUnit;

public abstract class AbstractConditionSequence extends TestExecutionUnit implements ConditionSequence, DataUtils {

	private List<TestExecutionUnit> callables = new ArrayList<>();
	private ListMultimap<String, TestExecutionUnit> accessories = LinkedListMultimap.create();
	private Map<Class<? extends Condition>, TestExecutionUnit> replacements = new HashMap<>();
	private Map<Class<? extends Condition>, TestExecutionUnit> insertBefore = new HashMap<>();
	private Map<Class<? extends Condition>, TestExecutionUnit> insertAfter = new HashMap<>();
	private List<TestExecutionUnit> before = new ArrayList<>();
	private List<TestExecutionUnit> after = new ArrayList<>();

	/**
	 * Add the builder to the list of calls to be made when this series is executed
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
	protected TestExecutionBuilder exec() {
		return new TestExecutionBuilder();
	}

	protected AbstractConditionSequence sequenceOf(TestExecutionUnit... units) {
		return new AbstractConditionSequence() {

			@Override
			public void evaluate() {
				call(Arrays.asList(units));
			}
		};
	}

	@Override
	public List<TestExecutionUnit> getTestExecutionUnits() {

		List<TestExecutionUnit> units = new ArrayList<>();
		units.addAll(before);
		units.addAll(this.callables.stream()
			.map((action) -> {
				if (action instanceof ConditionCallBuilder) {
					ConditionCallBuilder builder = (ConditionCallBuilder) action;
					if (replacements.containsKey(builder.getConditionClass())) {
						// if we know to replace the class on the way in, do it here
						return replacements.get(builder.getConditionClass());
					} else if (insertBefore.containsKey(builder.getConditionClass())) {
						return sequenceOf(insertBefore.get(builder.getConditionClass()), action);
					} else if (insertAfter.containsKey(builder.getConditionClass())) {
						return sequenceOf(action, insertAfter.get(builder.getConditionClass()));
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
	public ConditionSequence with(String key, TestExecutionUnit... builders) {

		for (TestExecutionUnit builder : builders) {
			this.accessories.put(key, builder);
		}

		return this;
	}

	@Override
	public ConditionSequence replace(Class<? extends Condition> conditionToReplace, TestExecutionUnit builder) {
		this.replacements.put(conditionToReplace, builder);

		return this;
	}


	@Override
	public ConditionSequence insertBefore(Class<? extends Condition> conditionToInsertAt, TestExecutionUnit builder) {
		this.insertBefore.put(conditionToInsertAt, builder);
		return this;
	}
	@Override
	public ConditionSequence insertAfter(Class<? extends Condition> conditionToInsertAt, TestExecutionUnit builder) {
		this.insertAfter.put(conditionToInsertAt, builder);
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

	protected boolean hasAccessory(String key) {
		return this.accessories.containsKey(key);
	}

	protected List<TestExecutionUnit> getAccessories(String key) {
		return this.accessories.get(key);
	}

	protected void runAccessory(String key, TestExecutionUnit... defaults) {
		if (hasAccessory(key)) {
			call(getAccessories(key));
		} else {
			Arrays.asList(defaults);
		}
	}

}

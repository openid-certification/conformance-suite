package io.fintechlabs.testframework.sequence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.testmodule.ConditionCallBuilder;
import io.fintechlabs.testframework.testmodule.DataUtils;
import io.fintechlabs.testframework.testmodule.TestExecutionBuilder;
import io.fintechlabs.testframework.testmodule.TestExecutionUnit;

public abstract class AbstractConditionSequence extends TestExecutionUnit implements ConditionSequence, DataUtils {

	private List<TestExecutionUnit> callables = new ArrayList<>();
	private Map<String, TestExecutionUnit> accessories = new HashMap<>();

	/**
	 * Add the builder to the list of calls to be made when this series is executed
	 *
	 * @param builder
	 */
	protected void call(TestExecutionUnit builder) {
		callables.add(builder);
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

	@Override
	public List<TestExecutionUnit> getTestExecutionUnits() {
		return this.callables;
	}

	@Override
	public ConditionSequence with(String key, TestExecutionUnit builder) {
		this.accessories.put(key, builder);

		return this;
	}

	protected boolean hasAccessory(String key) {
		return this.accessories.containsKey(key);
	}

	protected TestExecutionUnit getAccessory(String key) {
		return this.accessories.get(key);
	}

}

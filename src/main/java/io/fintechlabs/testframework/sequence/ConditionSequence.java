package io.fintechlabs.testframework.sequence;

import java.util.List;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.testmodule.TestExecutionUnit;

public interface ConditionSequence {

	void evaluate();

	List<TestExecutionUnit> getTestExecutionUnits();

	ConditionSequence with(String key, TestExecutionUnit builder);

	ConditionSequence replace(Class<? extends Condition> conditionToReplace, TestExecutionUnit builder);

}

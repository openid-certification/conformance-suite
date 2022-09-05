package net.openid.conformance.sequence;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.TestExecutionUnit;

import java.util.List;

public interface ConditionSequence extends TestExecutionUnit {

	void evaluate();

	List<TestExecutionUnit> getTestExecutionUnits();

	ConditionSequence replace(Class<? extends Condition> conditionToReplace, TestExecutionUnit builder);

	ConditionSequence skip(Class<? extends Condition> conditionToSkip, String message);

	ConditionSequence butFirst(TestExecutionUnit... builders);

	ConditionSequence then(TestExecutionUnit... builders);

	ConditionSequence insertAfter(Class<? extends Condition> conditionToInsertAt, TestExecutionUnit builder);

	ConditionSequence insertBefore(Class<? extends Condition> conditionToInsertAt, TestExecutionUnit builder);

}

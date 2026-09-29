package net.openid.conformance.sequence;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.ExecutionContext;
import net.openid.conformance.testmodule.TestExecutionUnit;
import org.slf4j.LoggerFactory;

import java.util.List;

public interface ConditionSequence extends TestExecutionUnit {

	void evaluate();

	List<TestExecutionUnit> getTestExecutionUnits();

	@Override
	default void run(ExecutionContext context) {
		LoggerFactory.getLogger(ConditionSequence.class).info(context.getTestId() + ":   Starting sequence " + getClass().getSimpleName());

		evaluate();
		getTestExecutionUnits().forEach(context::run);

		LoggerFactory.getLogger(ConditionSequence.class).info(context.getTestId() + ":   End of sequence " + getClass().getSimpleName());
	}

	ConditionSequence replace(Class<? extends Condition> conditionToReplace, TestExecutionUnit builder);

	ConditionSequence skip(Class<? extends Condition> conditionToSkip, String message);

	ConditionSequence butFirst(TestExecutionUnit... builders);

	ConditionSequence then(TestExecutionUnit... builders);

	ConditionSequence insertAfter(Class<? extends Condition> conditionToInsertAt, TestExecutionUnit builder);

	ConditionSequence insertBefore(Class<? extends Condition> conditionToInsertAt, TestExecutionUnit builder);

	/*
	 * The String overloads address functional conditions (see FunctionalCondition) by their name.
	 */

	ConditionSequence replace(String conditionName, TestExecutionUnit builder);

	ConditionSequence skip(String conditionName, String message);

	ConditionSequence insertAfter(String conditionName, TestExecutionUnit builder);

	ConditionSequence insertBefore(String conditionName, TestExecutionUnit builder);

}

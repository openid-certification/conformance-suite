package io.fintechlabs.testframework.sequence;

import java.util.List;

import io.fintechlabs.testframework.testmodule.TestExecutionUnit;

public interface ConditionSequence extends TestExecutionUnit {

	void evaluate();

	List<TestExecutionUnit> getTestExecutionUnits();

}

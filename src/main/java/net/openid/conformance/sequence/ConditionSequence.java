package net.openid.conformance.sequence;

import java.util.List;

import net.openid.conformance.testmodule.TestExecutionUnit;

public interface ConditionSequence extends TestExecutionUnit {

	void evaluate();

	List<TestExecutionUnit> getTestExecutionUnits();

}

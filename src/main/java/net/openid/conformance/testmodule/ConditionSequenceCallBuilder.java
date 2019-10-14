package net.openid.conformance.testmodule;

import java.util.function.Supplier;

import net.openid.conformance.sequence.ConditionSequence;

public class ConditionSequenceCallBuilder implements TestExecutionUnit {

	private Class<? extends ConditionSequence> conditionSequenceClass = null;
	private Supplier<? extends ConditionSequence> conditionSequenceConstructor = null;

	public ConditionSequenceCallBuilder(Class<? extends ConditionSequence> conditionSequenceClass) {
		assert conditionSequenceClass != null;
		this.conditionSequenceClass = conditionSequenceClass;
	}

	public ConditionSequenceCallBuilder(Supplier<? extends ConditionSequence> conditionSequenceConstructor) {
		assert conditionSequenceConstructor != null;
		this.conditionSequenceConstructor = conditionSequenceConstructor;
	}

	public Class<? extends ConditionSequence> getConditionSequenceClass() {
		return conditionSequenceClass;
	}

	public Supplier<? extends ConditionSequence> getConditionSequenceConstructor() {
		return conditionSequenceConstructor;
	}
}

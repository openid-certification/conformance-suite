package net.openid.conformance.testmodule;

import net.openid.conformance.sequence.ConditionSequence;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

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

	/**
	 * Create a new instance of the sequence to call.
	 */
	public ConditionSequence createSequence(ExecutionContext context) {
		if (conditionSequenceConstructor != null) {
			return conditionSequenceConstructor.get();
		}
		try {
			return conditionSequenceClass.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
			throw context.fatalError("Fatal failure from condition sequence: " + conditionSequenceClass.getSimpleName(), e);
		}
	}

	@Override
	public void run(ExecutionContext context) {
		context.run(createSequence(context));
	}

	@Override
	public TestExecutionUnit transform(ExecutionContext context, UnaryOperator<TestExecutionUnit> leafMapper) {
		return createSequence(context).transform(context, leafMapper);
	}
}

package net.openid.conformance.openid.ssf.conditions.as;

import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import net.openid.conformance.testmodule.TestExecutionUnit;

import java.lang.reflect.InvocationTargetException;

/**
 * Re-issues every step of a delegate sequence as a stop-on-failure call, keeping each step's
 * condition, severity and requirement tags. The shared client-authentication sequences let every
 * step continue on failure, which suits a module that tests the client and wants every defect
 * listed; an emulated authorization server must instead refuse the token as soon as the
 * authentication is found invalid, so the caller can catch the failure and answer
 * {@code invalid_client}.
 */
public class OIDSSFStopOnFirstFailureSequence extends AbstractConditionSequence {

	private final Class<? extends ConditionSequence> delegate;

	public OIDSSFStopOnFirstFailureSequence(Class<? extends ConditionSequence> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void evaluate() {
		ConditionSequence sequence;
		try {
			sequence = delegate.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new IllegalStateException("Couldn't create condition sequence: " + delegate.getSimpleName(), e);
		}
		sequence.evaluate();

		for (TestExecutionUnit unit : sequence.getTestExecutionUnits()) {
			if (!(unit instanceof ConditionCallBuilder step)) {
				call(unit);
				continue;
			}
			ConditionCallBuilder stopping = step.getCondition() != null ? condition(step.getCondition()) : condition(step.getConditionClass());
			stopping.onFail(step.getOnFail()).onSkip(step.getOnSkip()).requirements(step.getRequirements());
			stopping.skipIfObjectsMissing(step.getSkipIfObjectsMissing().toArray(new String[0]));
			stopping.skipIfStringsMissing(step.getSkipIfStringsMissing().toArray(new String[0]));
			stopping.skipIfStringsPresent(step.getSkipIfStringsPresent().toArray(new String[0]));
			stopping.skipIfLongsMissing(step.getSkipIfLongsMissing().toArray(new String[0]));
			step.getSkipIfElementsMissing().forEach(pair -> stopping.skipIfElementMissing(pair.getLeft(), pair.getRight()));
			step.getSkipIfElementsPresent().forEach(pair -> stopping.skipIfElementPresent(pair.getLeft(), pair.getRight()));
			call(stopping);
		}
	}
}

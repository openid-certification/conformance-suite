package net.openid.conformance.openid.ssf.conditions;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * A condition that always raises the given finding; the caller chooses the
 * severity (e.g. {@code ConditionResult.WARNING} or {@code FAILURE}). Used by
 * test modules that detect a problem in event-driven callbacks, where there is
 * no dedicated condition to express the finding.
 */
public class OIDSSFFindingCondition extends AbstractCondition {

	protected final String message;

	public OIDSSFFindingCondition(String message) {
		this.message = message;
	}

	@Override
	public Environment evaluate(Environment env) {
		throw error(message);
	}
}

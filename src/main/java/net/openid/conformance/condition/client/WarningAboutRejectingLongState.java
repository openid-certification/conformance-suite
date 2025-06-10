package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class WarningAboutRejectingLongState extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		throw error("Rejecting a state of this length may result in interoperability issues.");
	}

}

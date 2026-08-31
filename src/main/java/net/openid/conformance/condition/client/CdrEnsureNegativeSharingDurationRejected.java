package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class CdrEnsureNegativeSharingDurationRejected extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		throw error("The authorisation SHOULD fail when a negative sharing_duration is requested, but the Data Holder allowed the authorisation to complete");
	}

}

package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CDRRefreshTokenRequiredWhenSharingDurationRequested extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		throw error("No refresh token found - the CDR standard requires that a refresh token is returned as the sharing_duration claim was included in the request.");

	}
}

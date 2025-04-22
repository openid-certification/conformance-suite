package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class WarningAboutRequestUriError extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		throw error("The server rejected reuse of the 'request_uri' prior to authentication completion.");
	}

}

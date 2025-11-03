package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFInjectPushAuthorizationHeader extends AbstractCondition {

	protected final String pushAuthorizationHeader;

	public OIDSSFInjectPushAuthorizationHeader(String pushAuthorizationHeader) {
		this.pushAuthorizationHeader = pushAuthorizationHeader;
	}

	@Override
	public Environment evaluate(Environment env) {

		env.putString("ssf","transmitter.push_endpoint_authorization_header", pushAuthorizationHeader);
		log("Configured authorization header for push delivery", args("push_authorization_header", pushAuthorizationHeader));
		return env;
	}
}

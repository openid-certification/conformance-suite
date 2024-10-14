package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.testmodule.Environment;

public class OIDSSFAttemptCreateStreamConfigCallWithInvalidToken extends OIDSSFCreateStreamConfigCall {

	@Override
	protected boolean throwOnClientResponseException() {
		return false;
	}

	@Override
	protected String getTransmitterAccessToken(Environment env) {
		return "invalid";
	}
}

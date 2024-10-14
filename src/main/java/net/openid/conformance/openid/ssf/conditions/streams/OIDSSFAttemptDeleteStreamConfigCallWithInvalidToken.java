package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.testmodule.Environment;

public class OIDSSFAttemptDeleteStreamConfigCallWithInvalidToken extends OIDSSFDeleteStreamConfigCall {

	@Override
	protected boolean throwOnClientResponseException() {
		return false;
	}

	@Override
	protected String getTransmitterAccessToken(Environment env) {
		return "invalid";
	}

	@Override
	protected String getStreamId(Environment env) {
		return "dummy";
	}
}

package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.testmodule.Environment;

public class OIDSSFAttemptCreateStreamConfigCallWithBrokenInput extends OIDSSFCreateStreamConfigCall {

	@Override
	protected boolean throwOnClientResponseException() {
		return false;
	}

	@Override
	protected String createResourceRequestEntityString(Environment env) {
		return "{ broken";
	}
}

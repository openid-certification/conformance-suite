package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.testmodule.Environment;

public class OIDSSFAttemptReadStreamConfigCallWithUnknownStreamId extends OIDSSFReadStreamConfigCall {

	@Override
	protected boolean throwOnClientResponseException() {
		return false;
	}

	@Override
	protected String getStreamId(Environment env) {
		return "unknown_stream_id";
	}
}

package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.testmodule.Environment;

public class OIDSSFReadStreamStatusCall extends AbstractOIDSSFStreamConfigCall {

	@Override
	protected void prepareRequest(Environment env) {
		env.putString("resource", "resourceMethod", "GET");
	}

	@Override
	protected Object getBody(Environment env) {
		return null;
	}

	@Override
	protected String getEndpointName() {
		return "read stream status";
	}

	@Override
	protected void configureResourceUrl(Environment env) {
		String readStreamUri = getStreamStatusEndpointUrlWithStreamId(env);
		env.putString("protected_resource_url", readStreamUri);
	}
}

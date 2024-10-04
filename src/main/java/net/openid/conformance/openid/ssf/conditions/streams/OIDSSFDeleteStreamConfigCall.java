package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.testmodule.Environment;

public class OIDSSFDeleteStreamConfigCall extends AbstractOIDSSFStreamConfigCall {

	@Override
	protected boolean requireJsonResponseBody() {
		return false;
	}

	@Override
	protected Object getBody(Environment env) {
		return null;
	}

	@Override
	protected String getEndpointName() {
		return "delete stream configuration";
	}

	@Override
	protected void prepareRequest(Environment env) {
		env.putString("resource", "resourceMethod", "DELETE");
	}

	@Override
	protected void configureResourceUrl(Environment env) {
		String deleteStreamUri = getStreamConfigEndpointUrlWithStreamIdIfPresent(env);
		env.putString("protected_resource_url", deleteStreamUri);
	}
}

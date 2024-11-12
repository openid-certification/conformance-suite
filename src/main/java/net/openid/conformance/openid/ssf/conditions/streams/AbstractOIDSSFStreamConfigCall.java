package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.openid.ssf.conditions.AbstractOIDSSFTransmitterEndpointCall;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractOIDSSFStreamConfigCall extends AbstractOIDSSFTransmitterEndpointCall {

	@Override
	protected String getResourceEndpointUrl(Environment env) {
		return getConfigurationEndpointUrl(env);
	}

	protected String getStreamStatusEndpointUrlWithStreamId(Environment env) {
		return appendStreamIdIfPresent(getStatusEndpointUrl(env), env);
	}

	protected String getStreamConfigEndpointUrlWithStreamIdIfPresent(Environment env) {
		return appendStreamIdIfPresent(getConfigurationEndpointUrl(env), env);
	}

	private String appendStreamIdIfPresent(String endpoint, Environment env) {
		String streamId = getStreamId(env);
		String effectiveEndpoint = endpoint;
		if (streamId != null) {
			effectiveEndpoint = effectiveEndpoint + "?stream_id=" + streamId;
		}
		return effectiveEndpoint;
	}

	protected String getStreamId(Environment env) {
		return env.getString("ssf", "stream.stream_id");
	}
}

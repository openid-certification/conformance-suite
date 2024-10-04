package net.openid.conformance.openid.ssf.conditions.subjects;

import com.google.gson.Gson;
import net.openid.conformance.openid.ssf.conditions.streams.AbstractOIDSSFStreamConfigCall;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class OIDSSFRemoveSubjectToStreamConfigCall extends AbstractOIDSSFStreamConfigCall {

	@Override
	protected String getEndpointName() {
		return "Remove subject to stream configuration";
	}

	@Override
	protected String getResourceEndpointUrl(Environment env) {
		return getRemoveSubjectEndpointUrl(env);
	}

	@Override
	protected void prepareRequest(Environment env) {

		env.putString("resource", "resourceMethod", "POST");
		String streamId = getStreamId(env);

		env.putString("resource_request_entity", new Gson().toJson(
			Map.of(
				"stream_id", streamId,
				"subject", Map.of("format", "email", "email", "example.user@example.com")
			)
		));
	}
}

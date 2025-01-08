package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.Gson;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class OIDSSFUpdateStreamStatusCall extends AbstractOIDSSFStreamConfigCall {

	@Override
	protected void prepareRequest(Environment env) {
		env.putString("resource", "resourceMethod", "POST");
		addResourceRequestEntity(env);
	}

	protected void addResourceRequestEntity(Environment env) {
		env.putString("resource_request_entity", createResourceRequestEntityString(env));
	}

	protected String createResourceRequestEntityString(Environment env) {

		String streamId = getStreamId(env);

		// TODO check all allowed stream status values: "enabled,paused,disabled"
		return new Gson().toJson(
			Map.of(
				"stream_id", streamId,
				"status", "paused",
				"reason", "Updated Stream status for Receiver OIDF Conformance Test-Suite"
			)
		);
	}

	@Override
	protected String getEndpointName() {
		return "update stream status";
	}

	@Override
	protected void configureResourceUrl(Environment env) {
		String readStreamUri = getStreamStatusEndpointUrlWithStreamId(env);
		env.putString("protected_resource_url", readStreamUri);
	}
}

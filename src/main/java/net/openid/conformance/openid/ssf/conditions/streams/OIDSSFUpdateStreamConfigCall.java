package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.Gson;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;
import java.util.Set;

public class OIDSSFUpdateStreamConfigCall extends AbstractOIDSSFStreamConfigCall {

	@Override
	protected String getEndpointName() {
		return "update stream configuration";
	}

	@Override
	protected void prepareRequest(Environment env) {

		env.putString("resource", "resourceMethod", "PATCH");
		String streamId = getStreamId(env);

		env.putString("resource_request_entity",
			new Gson().toJson(
				Map.of(
					"stream_id", streamId,
					"events_requested",
					Set.of(
						"https://schemas.openid.net/secevent/caep/event-type/session-revoked",
						"https://schemas.openid.net/secevent/caep/event-type/credential-change",
						"https://schemas.openid.net/secevent/caep/event-type/device-compliance-change"
					),
					"description", "Updated Stream for Receiver OIDF Conformance Test-Suite"
				)
			));
	}
}

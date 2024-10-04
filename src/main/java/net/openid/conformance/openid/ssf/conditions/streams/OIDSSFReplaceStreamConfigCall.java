package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.Gson;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;
import java.util.Set;

public class OIDSSFReplaceStreamConfigCall extends AbstractOIDSSFStreamConfigCall {

	@Override
	protected String getEndpointName() {
		return "replace stream configuration";
	}

	@Override
	protected void prepareRequest(Environment env) {

		env.putString("resource", "resourceMethod", "PUT");
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
					"description", "Replaced Stream for Receiver OIDF Conformance Test-Suite",
					"delivery", Map.of("method", "urn:ietf:rfc:8935", "endpoint_url", "https://receiver.example.com/events"),
					"audience", "https://localhost.emobix.co.uk:8443"
				)
			));
	}
}

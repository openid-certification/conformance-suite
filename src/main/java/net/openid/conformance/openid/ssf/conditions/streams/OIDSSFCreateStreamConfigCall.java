package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.Gson;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;
import java.util.Set;

public class OIDSSFCreateStreamConfigCall extends AbstractOIDSSFStreamConfigCall {

	@Override
	protected void prepareRequest(Environment env) {

		env.putString("resource", "resourceMethod", "POST");

		addResourceRequestEntity(env);
	}

	@Override
	protected String getEndpointName() {
		return "create stream configuration";
	}

	protected void addResourceRequestEntity(Environment env) {
		env.putString("resource_request_entity", createResourceRequestEntityString(env));
	}

	protected String createResourceRequestEntityString(Environment env) {
		return new Gson().toJson(
			Map.of(
				"events_requested",
				Set.of(
					"https://schemas.openid.net/secevent/caep/event-type/session-revoked",
					"https://schemas.openid.net/secevent/caep/event-type/credential-change",
					"https://schemas.openid.net/secevent/caep/event-type/device-compliance-change"
				),
				"description", "Stream for Receiver OIDF Conformance Test-Suite",
				"delivery", Map.of("method", "urn:ietf:rfc:8935", "endpoint_url", "https://receiver.example.com/events"),
				"audience", "https://localhost.emobix.co.uk:8443"
			)
		);
	}
}

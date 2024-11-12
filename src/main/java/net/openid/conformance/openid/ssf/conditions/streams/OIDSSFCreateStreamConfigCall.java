package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;
import java.util.Set;

public class OIDSSFCreateStreamConfigCall extends AbstractOIDSSFStreamConfigCall {

	@Override
	protected String getEndpointName() {
		return "create stream configuration";
	}

	@Override
	protected void prepareRequest(Environment env) {

		env.putString("resource", "resourceMethod", "POST");

		addResourceRequestEntity(env);
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
					"https://schemas.openid.net/secevent/caep/event-type/credential-change"
				),
				"format", "iss_sub",
				"description", "Stream for Receiver OIDF Conformance Test-Suite",
				 "delivery", Map.of("method", "urn:ietf:rfc:8935", "endpoint_url", "https://receiver.example.com/events"),
//				"delivery", Map.of( //
//					"method", "https://schemas.openid.net/secevent/risc/delivery-method/push", //
//					"endpoint_url", "https://receiver.example.com/events", //
//					"authorization_header", "{authorizationHeaderValue}" //
//					)
				 "audience", "https://localhost.emobix.co.uk:8443"
			)
		);
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		super.handleClientResponse(env, responseCode, responseBody, responseHeaders, fullResponse);
		env.putObject("ssf", "stream", env.getElementFromObject("resource_endpoint_response_full", "body_json").getAsJsonObject());
		return env;
	}
}

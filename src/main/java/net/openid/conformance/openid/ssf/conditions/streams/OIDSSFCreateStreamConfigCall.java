package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.Environment;

import java.util.LinkedHashMap;
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

		String deliveryMethod = env.getString("ssf", "delivery_method");
		Map<String, Object> delivery = null;
		if (SsfDeliveryMode.PUSH.getAlias().equals(deliveryMethod)) {

			String deliveryEndpoint = createPushDeliveryEndpointUrl(env);

			delivery = new LinkedHashMap<>();
			delivery.put("method", deliveryMethod);
			delivery.put("endpoint_url", deliveryEndpoint);
			// TODO make some auth header configurable
			delivery.put("authorization_header", "someAuthHeaderValue");
		}

		Map<String, Object> streamConfig = new LinkedHashMap<>(Map.of(
				"events_requested",
				Set.of(
						"https://schemas.openid.net/secevent/caep/event-type/session-revoked",
						"https://schemas.openid.net/secevent/caep/event-type/credential-change"
				),
				"format", "iss_sub",
				"description", "Stream for Receiver OIDF Conformance Test-Suite",
//				"delivery", Map.of( //
//					"method", "https://schemas.openid.net/secevent/risc/delivery-method/push", //
//					"endpoint_url", "https://receiver.example.com/events", //
//					"authorization_header", "{authorizationHeaderValue}" //
//					)
				"audience", "https://localhost.emobix.co.uk:8443"
		));

		if (delivery != null) {
			streamConfig.put("delivery", delivery);
		}

		return new Gson().toJson(streamConfig);
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		super.handleClientResponse(env, responseCode, responseBody, responseHeaders, fullResponse);
		JsonObject streamConfigObject = env.getElementFromObject("resource_endpoint_response_full", "body_json").getAsJsonObject();
		env.putObject("ssf", "stream", streamConfigObject);
		return env;
	}

	protected String createPushDeliveryEndpointUrl(Environment env) {

		String baseUrl = env.getString("base_url");
		String externalUrlOverride = env.getString("external_url_override");
		if (!Strings.isNullOrEmpty(externalUrlOverride)) {
			baseUrl = externalUrlOverride;
		}
		return "${baseUrl}/ssf-push".replace("${baseUrl}", baseUrl);
	}
}

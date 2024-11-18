package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

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

		JsonObject deliveryObject = env.getElementFromObject("ssf", "delivery").getAsJsonObject();

		String deliveryMethod = OIDFJSON.getString(deliveryObject.get("delivery_method"));

		Map<String, Object> delivery = null;
		if (SsfDeliveryMode.PUSH.getAlias().equals(deliveryMethod)) {

			String deliveryEndpoint = createPushDeliveryEndpointUrl(env);

			String authHeader = null;
			if (deliveryObject.has("authorization_header")) {
				authHeader = OIDFJSON.getString(deliveryObject.get("authorization_header"));
			}

			delivery = new LinkedHashMap<>();
			delivery.put("method", deliveryMethod);
			delivery.put("endpoint_url", deliveryEndpoint);
			if (authHeader != null) {
				delivery.put("authorization_header", authHeader);
			}
		}

		Map<String, Object> streamConfig = new LinkedHashMap<>(Map.of(
				"events_requested",
				Set.of(
						"https://schemas.openid.net/secevent/caep/event-type/session-revoked",
						"https://schemas.openid.net/secevent/caep/event-type/credential-change"
				),
				"format", "iss_sub",
				"description", "Stream for Receiver OIDF Conformance Test-Suite",
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

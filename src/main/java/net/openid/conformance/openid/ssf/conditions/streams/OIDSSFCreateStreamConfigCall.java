package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.util.StringUtils;

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

		String audience = env.getString("config", "ssf.stream.audience");

		Set<String> eventsRequested = Set.of( //
			"https://schemas.openid.net/secevent/caep/event-type/session-revoked", //
			"https://schemas.openid.net/secevent/caep/event-type/credential-change" //
		);

		Map<String, Object> streamConfig = new LinkedHashMap<>( //
			Map.of( //
				"events_requested", eventsRequested, //
				"format", "iss_sub", //
				"description", "Stream for Receiver OIDF Conformance Test-Suite", //
				// TODO make audience configurable
				"audience", audience //
			) //
		);

		JsonObject delivery = createDeliveryObject(env);
		if (delivery != null) {
			streamConfig.put("delivery", delivery);
		}

		return new Gson().toJson(streamConfig);
	}

	protected JsonObject createDeliveryObject(Environment env) {

		String deliveryMethod = env.getString("ssf", "delivery_method");

		if (SsfDeliveryMode.PUSH.getAlias().equals(deliveryMethod)) {
			return createPushDelivery(env, deliveryMethod);
		}

		if (SsfDeliveryMode.POLL.getAlias().equals(deliveryMethod)) {
			JsonObject delivery = new JsonObject();
			delivery.addProperty("method", deliveryMethod);
			return delivery;
		}

		throw error("Unsupported delivery method " + deliveryMethod);
	}

	protected JsonObject createPushDelivery(Environment env, String deliveryMethod) {

		JsonObject delivery = new JsonObject();
		delivery.addProperty("method", deliveryMethod);

		String pushDeliveryEndpoint = env.getString("ssf", "push_delivery_endpoint_url");
		delivery.addProperty("endpoint_url", pushDeliveryEndpoint);

		String authHeader = getPushDeliveryAuthorizationHeader(env);
		if (authHeader != null) {
			delivery.addProperty("authorization_header", authHeader);
		}
		return delivery;
	}

	private static String getPushDeliveryAuthorizationHeader(Environment env) {
		String authHeader = null;
		JsonElement authorizationHeaderEl = env.getElementFromObject("config", "ssf.transmitter.push_endpoint_authorization_header");
		if (authorizationHeaderEl != null) {
			String pushAuthorizationHeader = OIDFJSON.getString(authorizationHeaderEl);
			if (StringUtils.hasText(pushAuthorizationHeader)) {
				authHeader = pushAuthorizationHeader;
			}
		}
		return authHeader;
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		super.handleClientResponse(env, responseCode, responseBody, responseHeaders, fullResponse);
		JsonObject streamConfigObject = env.getElementFromObject("resource_endpoint_response_full", "body_json").getAsJsonObject();
		env.putObject("ssf", "stream", streamConfigObject);
		return env;
	}
}

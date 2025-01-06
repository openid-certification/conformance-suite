package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.util.StringUtils;

public class OIDSSFPrepareStreamConfigObjectSetDeliveryMethod extends AbstractOIDSSFPrepareStreamConfigObject {

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonObject streamConfig = getStreamConfig(env);

		JsonObject delivery = createDeliveryObject(env);
		if (delivery != null) {
			streamConfig.add("delivery", delivery);
			logSuccess("Added 'delivery' to stream configuration", args("config", streamConfig, "delivery", delivery));
		} else {
			log("No 'delivery' added to stream configuration");
		}

		return env;
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

	private String getPushDeliveryAuthorizationHeader(Environment env) {
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
}

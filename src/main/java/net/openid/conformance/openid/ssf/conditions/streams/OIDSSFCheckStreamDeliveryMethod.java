package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfConstants;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFCheckStreamDeliveryMethod extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonElement streamDeliveryEl = env.getElementFromObject("ssf", "stream.delivery");
		if (streamDeliveryEl == null) {
			logFailure("Could not find delivery object in stream configuration",
				args("stream_configuration", env.getElementFromObject("ssf", "stream")));
			return env;
		}

		JsonObject supportedDeliveryObject = streamDeliveryEl.getAsJsonObject();
		if (supportedDeliveryObject == null) {
			logFailure("Could not find delivery object in stream configuration",
				args("stream_configuration", env.getElementFromObject("ssf", "stream")));
			return env;
		}

		String deliveryMethod = OIDFJSON.getString(supportedDeliveryObject.get("method"));
		if (deliveryMethod == null) {
			logFailure("Could not find delivery method in stream configuration",
				args("stream_configuration", env.getElementFromObject("ssf", "stream")));
			return env;
		}


		if (!SsfConstants.STANDARD_DELIVERY_METHODS.contains(deliveryMethod)) {
			logFailure("Found unknown delivery method in stream configuration",
				args("stream_configuration", env.getElementFromObject("ssf", "stream"),
					"unknown_delivery_method", deliveryMethod));
			return env;
		}

		logSuccess("The delivery method found matches the standard delivery methods",
			args("delivery_method", deliveryMethod,
				"standard_delivery_methods", SsfConstants.STANDARD_DELIVERY_METHODS));

		return env;
	}
}

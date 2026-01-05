package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFEnsureDeliveryMethodIsSupported extends AbstractCondition {

	private final SsfDeliveryMode deliveryMode;

	public OIDSSFEnsureDeliveryMethodIsSupported(SsfDeliveryMode deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonElement deliveryMethodsSupported = env.getElementFromObject("ssf", "transmitter_metadata.delivery_methods_supported");
		if (!OIDFJSON.convertJsonArrayToList(deliveryMethodsSupported.getAsJsonArray()).contains(deliveryMode.getAlias())) {
			throw error("Selected delivery method " + deliveryMode.getAlias() + " is not support by Transmitter.",
				args("delivery_methods_supported", deliveryMethodsSupported, "delivery_method", deliveryMode.getAlias()));
		}

		logSuccess("Selected delivery method " + deliveryMode.getAlias() + " is support by Transmitter.",
			args("delivery_methods_supported", deliveryMethodsSupported, "delivery_method", deliveryMode.getAlias()));

		return env;
	}
}

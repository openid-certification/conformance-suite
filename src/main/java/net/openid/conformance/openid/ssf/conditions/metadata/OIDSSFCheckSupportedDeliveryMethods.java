package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfConstants;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class OIDSSFCheckSupportedDeliveryMethods extends AbstractCondition {

	public static final Set<String> STANDARD_DELIVERY_METHODS = Set.of(
		SsfConstants.DELIVERY_METHOD_PUSH_RFC_8935_URI, //
		SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI
	);

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonElement supportedDeliveryMethodsEl = env.getElementFromObject("ssf", "transmitter_metadata.delivery_methods_supported");
		if (supportedDeliveryMethodsEl == null) {
			throw error("Couldn't find delivery_methods_supported in transmitter_metadata", args("transmitter_metadata", env.getElementFromObject("ssf", "transmitter_metadata")));
		}

		List<String> supportedDeliveryMethods = OIDFJSON.convertJsonArrayToList(supportedDeliveryMethodsEl.getAsJsonArray());

		Set<String> unknownDeliveryMethods = findUnknownDeliveryMethods(supportedDeliveryMethods);

		if (!unknownDeliveryMethods.isEmpty()) {
			throw error("Found unknown delivery methods in transmitter_metadata",
				args("unknown_delivery_methods", unknownDeliveryMethods,
					"delivery_methods_supported", supportedDeliveryMethods,
					"standard_delivery_methods", STANDARD_DELIVERY_METHODS));
		} else {
			logSuccess("All found delivery methods in transmitter_metadata are supported",
				args("delivery_methods_supported", supportedDeliveryMethods));
		}

		return env;
	}

	protected Set<String> findUnknownDeliveryMethods(List<String> supportedEventTypes) {
		Set<String> unknownEventTypes = new LinkedHashSet<>(supportedEventTypes);
		unknownEventTypes.removeAll(STANDARD_DELIVERY_METHODS);
		return unknownEventTypes;
	}
}

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

		Set<String> extensionDeliveryMethods = findExtensionDeliveryMethods(supportedDeliveryMethods);

		if (!extensionDeliveryMethods.isEmpty()) {
			// Per SSF 1.0 6.1, delivery methods are identified by URIs and the spec
			// does not restrict delivery_methods_supported to the two standard RFC
			// URIs — extension methods (e.g. RISC-specific URIs) are permitted.
			log("Transmitter metadata advertises delivery methods beyond RFC 8935/8936. Extension methods are permitted per SSF 1.0 6.1.",
				args("extension_delivery_methods", extensionDeliveryMethods,
					"delivery_methods_supported", supportedDeliveryMethods,
					"standard_delivery_methods", STANDARD_DELIVERY_METHODS));
		}

		logSuccess("Transmitter metadata delivery_methods_supported validated",
			args("delivery_methods_supported", supportedDeliveryMethods,
				"standard_delivery_methods", STANDARD_DELIVERY_METHODS));

		return env;
	}

	protected Set<String> findExtensionDeliveryMethods(List<String> supportedDeliveryMethods) {
		Set<String> extensionMethods = new LinkedHashSet<>(supportedDeliveryMethods);
		extensionMethods.removeAll(STANDARD_DELIVERY_METHODS);
		return extensionMethods;
	}
}

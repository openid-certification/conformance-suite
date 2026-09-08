package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

/**
 * CAEP Interop Profile 2.3.2: "The Transmitter Configuration Metadata MUST include the
 * delivery_methods_supported field." This checks presence and shape only. Whether the
 * advertised methods cover the scheduled test run is judged separately by
 * {@link OIDSSFEnsureDeliveryMethodIsSupported} with the selected delivery-mode variant:
 * certification runs are per delivery mode, and the profile's transmitter-side wording
 * (2.3.8.1) is ambiguous about requiring both standard methods, so only the selected
 * method is required to be advertised.
 */
public class OIDSSFCaepInteropDeliveryMethodsTransmitterMetadataCheck extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonElement supportedDeliveryMethodsEl = env.getElementFromObject("ssf", "transmitter_metadata.delivery_methods_supported");
		if (supportedDeliveryMethodsEl == null) {
			throw error("Transmitter metadata is missing the delivery_methods_supported field required by the CAEP Interop Profile (2.3.2)",
				args("transmitter_metadata", env.getElementFromObject("ssf", "transmitter_metadata")));
		}

		if (!supportedDeliveryMethodsEl.isJsonArray()) {
			throw error("delivery_methods_supported in the transmitter metadata must be a JSON array",
				args("delivery_methods_supported", supportedDeliveryMethodsEl));
		}

		List<String> supportedDeliveryMethods = OIDFJSON.convertJsonArrayToList(supportedDeliveryMethodsEl.getAsJsonArray());
		if (supportedDeliveryMethods.isEmpty()) {
			throw error("delivery_methods_supported in the transmitter metadata must not be empty",
				args("delivery_methods_supported", supportedDeliveryMethods));
		}

		logSuccess("Transmitter metadata contains the delivery_methods_supported field",
			args("delivery_methods_supported", supportedDeliveryMethods));

		return env;
	}
}

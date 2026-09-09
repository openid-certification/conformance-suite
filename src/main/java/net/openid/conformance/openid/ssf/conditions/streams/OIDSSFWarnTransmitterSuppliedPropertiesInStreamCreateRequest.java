package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * SSF 1.0 8.1.1.1 lists the values a Create Stream request MAY carry - the Receiver-Supplied
 * {@code events_requested}, {@code delivery} and {@code description}. The Transmitter-Supplied
 * properties ({@code stream_id}, {@code iss}, {@code aud}, {@code events_supported},
 * {@code events_delivered}, {@code min_verification_interval}, {@code inactivity_timeout}) are
 * decided by the transmitter, so a receiver sending them is proposing values it does not
 * control. Nothing forbids that, and Table 1 reserves 400 for a request that "cannot be
 * parsed", so the emulated transmitter honours the request and ignores those values; their
 * presence is reported here as a sender-side finding. Callers invoke this condition at WARNING.
 */
public class OIDSSFWarnTransmitterSuppliedPropertiesInStreamCreateRequest extends AbstractOIDSSFHandleReceiverRequest {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonElement streamConfigInputEl = env.getElementFromObject("ssf", "stream_input");
		if (streamConfigInputEl == null || !streamConfigInputEl.isJsonObject()) {
			log("No parsed stream configuration in the request body; nothing to check");
			return env;
		}
		JsonObject streamConfigInput = streamConfigInputEl.getAsJsonObject();

		Set<String> transmitterSupplied = new LinkedHashSet<>(getTransmitterSuppliedStreamConfigKeys());
		transmitterSupplied.retainAll(streamConfigInput.keySet());

		if (!transmitterSupplied.isEmpty()) {
			throw error("The Create Stream request carries Transmitter-Supplied stream configuration properties. "
					+ "SSF 1.0 8.1.1.1 lists only events_requested, delivery and description as request content; the transmitter "
					+ "decides the other values, so the ones sent were ignored.",
				args("transmitter_supplied_properties", transmitterSupplied, "stream_input", streamConfigInput));
		}

		logSuccess("The Create Stream request carries only Receiver-Supplied properties", args("stream_input", streamConfigInput));
		return env;
	}
}

package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class OIDSSFHandleStreamUpdateRequestValidation extends OIDSSFHandleStreamCreateRequestValidation {

	@Override
	protected Set<String> getTransmitterSuppliedProperties() {
		// allow stream_id for updates
		return super.getTransmitterSuppliedProperties().stream().filter(Predicate.not("stream_id"::equals)).collect(Collectors.toSet());
	}

	/**
	 * SSF 1.0 §8.1.1.3 / §8.1.1.4: for updates (PATCH) and replacements (PUT) the
	 * transmitter-supplied properties MAY be present in the request body as long as
	 * they match the expected values — §8.1.1.4 even suggests "read the
	 * configuration first, modify the JSON, then PUT it back". Presence alone is
	 * therefore not an error here; the value comparison (and the 400 on mismatch)
	 * happens in the update/replace request handlers, where the stored stream
	 * configuration is available.
	 */
	@Override
	protected void checkInvalidTransmitterSuppliedProperties(JsonObject streamConfigInput) {
		Set<String> echoedTransmitterSuppliedProperties = new HashSet<>(getTransmitterSuppliedProperties());
		echoedTransmitterSuppliedProperties.retainAll(streamConfigInput.keySet());
		if (!echoedTransmitterSuppliedProperties.isEmpty()) {
			log("Found transmitter-supplied properties echoed in the stream update/replace request body. "
					+ "This is permitted as long as the values match the current stream configuration.",
				args("echoed_transmitter_supplied", echoedTransmitterSuppliedProperties));
		}
	}

	/**
	 * For update/replace bodies, transmitter-supplied properties are legitimate
	 * content (see above) — exclude them from the unknown-properties log.
	 */
	@Override
	protected void checkUnknownProperties(JsonObject streamConfigInput) {
		Set<String> unknownProperties = new HashSet<>(streamConfigInput.keySet());
		unknownProperties.removeAll(supportedReceiverSuppliedProperties);
		unknownProperties.removeAll(transmitterSuppliedProperties);
		if (!unknownProperties.isEmpty()) {
			log("Found unknown properties in stream request body. This may indicate the receiver has misunderstood the spec, or it may be using extensions the test suite is unaware of.",
				args("stream_config", streamConfigInput, "unknown_properties", unknownProperties));
		}
	}
}

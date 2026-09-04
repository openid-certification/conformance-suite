package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFSpecVersionTransmitterMetadataCheck extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonObject transmitterMetadata = env.getElementFromObject("ssf","transmitter_metadata").getAsJsonObject();

		if (!transmitterMetadata.has("spec_version")) {
			throw error("Missing required spec_version field in transmitter_metadata",
				args("transmitter_metadata", transmitterMetadata));
		}

		String specVersion = OIDFJSON.getString(transmitterMetadata.get("spec_version"));
		if (!isValidVersion(specVersion)) {
			throw error("Found invalid spec_version field in transmitter_metadata. The CAEP Interop Profile (2.3.1) requires the value to be 1_0 or greater.",
				args("spec_version", specVersion));
		}

		logSuccess("Found valid spec_version field in transmitter_metadata", args("spec_version", specVersion));
		return env;
	}

	/**
	 * CAEP Interop Profile 2.3.1: "The Transmitter Configuration Metadata MUST
	 * include a spec_version field, and its value MUST be 1_0 or greater."
	 * <p>
	 * A trailing classifier such as {@code -ID2} marks an implementer's draft of
	 * that version, which precedes the version itself - so {@code 1_0-ID2} is NOT
	 * "1_0 or greater", while e.g. {@code 2_0-ID1} (a draft of a version beyond
	 * 1_0) is. Malformed values are reported as invalid, never as a crash.
	 */
	boolean isValidVersion(String specVersion) {

		if (specVersion == null || specVersion.isBlank() || specVersion.contains(".")) {
			return false;
		}

		String[] parts = specVersion.split("-", 2);
		boolean hasClassifier = parts.length > 1;
		if (hasClassifier && parts[1].isBlank()) {
			// e.g. "1_0-": a dangling classifier separator is not a valid version
			return false;
		}
		if (!parts[0].matches("(0|[1-9][0-9]*)_(0|[1-9][0-9]*)")) {
			// non-numeric or zero-padded components (e.g. "01_0") are not valid
			return false;
		}

		String[] versionParts = parts[0].split("_");
		int major = Integer.parseInt(versionParts[0]);
		int minor = Integer.parseInt(versionParts[1]);

		if (major > 1 || (major == 1 && minor > 0)) {
			return true;
		}

		return major == 1 && minor == 0 && !hasClassifier;
	}
}

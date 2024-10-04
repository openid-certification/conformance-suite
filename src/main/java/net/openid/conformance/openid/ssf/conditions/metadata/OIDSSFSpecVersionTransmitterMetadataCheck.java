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
			log("Skipping missing optional spec_version field in transmitter_metadata");
			return env;
		}

		String specVersion = OIDFJSON.getString(transmitterMetadata.get("spec_version"));
		if (isValidVersion(specVersion)) {
			throw error("Found invalid spec_version field in transmitter_metadata. Must be greater than 1.0-ID2.", args("spec_version", specVersion));
		}

		logSuccess("Found valid spec_version field in transmitter_metadata", args("spec_version", specVersion));
		return env;
	}

	boolean isValidVersion(String specVersion) {
		String[] parts = specVersion.split("-");
		String versionPart = parts[0].replace('_', '.');
		String classifierPart = parts.length > 1 ? parts[1] : null;
		return Double.parseDouble(versionPart) < 1.0 || (classifierPart != null && classifierPart.compareTo("ID2") < 0);
	}
}

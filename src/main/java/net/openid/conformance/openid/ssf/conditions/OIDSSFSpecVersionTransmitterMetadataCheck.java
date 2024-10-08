package net.openid.conformance.openid.ssf.conditions;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFSpecVersionTransmitterMetadataCheck extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"transmitter_metadata"})
	public Environment evaluate(Environment env) {

		JsonObject transmitterMetadata = env.getObject("transmitter_metadata");

		if (!transmitterMetadata.has("spec_version")) {
			log("Could not find spec_version field in transmitter_metadata");
			return env;
		}

		log("Found spec_version field in transmitter_metadata");
		String specVersion = OIDFJSON.getString(transmitterMetadata.get("spec_version"));
		String[] parts = specVersion.split("-");
		String versionPart = parts[0].replace('_', '.');
		String classifierPart = parts.length > 1 ? parts[1] : null;
		// TODO compare classifier 1_0-ID3
		if (Double.parseDouble(versionPart) < 1.0 || (classifierPart != null && classifierPart.compareTo("ID2") < 0)) {
			throw error("Invalid spec_version field in transmitter_metadata. Must be greater than 1.0-ID2.", args("spec_version", specVersion));
		}
		log("Found valid spec_version field in transmitter_metadata", args("spec_version", specVersion));

		return env;
	}
}

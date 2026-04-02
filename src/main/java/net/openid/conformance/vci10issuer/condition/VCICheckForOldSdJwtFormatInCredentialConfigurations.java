package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks if the credential issuer metadata uses 'vc+sd-jwt' (the pre-final spec format)
 * instead of 'dc+sd-jwt' (the final spec format). This may indicate the issuer has not
 * been updated since the older spec version.
 */
public class VCICheckForOldSdJwtFormatInCredentialConfigurations extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "vci" })
	public Environment evaluate(Environment env) {

		JsonObject metadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();
		JsonObject credentialConfigurations = metadata.getAsJsonObject("credential_configurations_supported");
		if (credentialConfigurations == null) {
			throw error("credential_configurations_supported is missing from credential issuer metadata");
		}

		List<String> oldFormatKeys = new ArrayList<>();
		for (String key : credentialConfigurations.keySet()) {
			JsonObject config = credentialConfigurations.getAsJsonObject(key);
			JsonElement formatEl = config.get("format");
			if (formatEl == null) {
				continue;
			}
			String format = OIDFJSON.getString(formatEl);
			if ("vc+sd-jwt".equals(format)) {
				oldFormatKeys.add(key);
			}
		}

		if (!oldFormatKeys.isEmpty()) {
			throw error("Credential configurations use 'vc+sd-jwt' format from pre-final spec versions. "
				+ "The final OID4VCI spec uses 'dc+sd-jwt'. This may indicate the issuer has not been "
				+ "updated to the final spec version.",
				args("credential_configurations_with_old_format", oldFormatKeys));
		}

		logSuccess("No credential configurations use the old 'vc+sd-jwt' format");
		return env;
	}
}

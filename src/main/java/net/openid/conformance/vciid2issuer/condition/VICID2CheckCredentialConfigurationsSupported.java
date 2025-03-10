package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class VICID2CheckCredentialConfigurationsSupported extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject credentialIssuerMetadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();

		JsonObject credentialConfigurationsSupported = credentialIssuerMetadata.getAsJsonObject("credential_configurations_supported");

		Map<String, Set<String>> invalidEntries = new LinkedHashMap<>();

		for (var entry : credentialConfigurationsSupported.entrySet()) {
			String credentialKey = entry.getKey();
			JsonObject credentialConfiguration = entry.getValue().getAsJsonObject();

			JsonElement formatEl = credentialConfiguration.get("format");
			if (formatEl == null) {
				invalidEntries.computeIfAbsent(credentialKey, k -> new LinkedHashSet<>()).add("Missing attribute claim: format");
			}

			JsonElement proofTypesSupportedEl = credentialConfiguration.get("proof_types_supported");
			if (proofTypesSupportedEl != null) {
				JsonObject proofTypesSupportedObject = proofTypesSupportedEl.getAsJsonObject();
				for (var proofTypesSupportedObjectEntry : proofTypesSupportedObject.entrySet()) {
					String proofTypeKey = proofTypesSupportedObjectEntry.getKey();
					JsonObject proofTypeObject = proofTypesSupportedObjectEntry.getValue().getAsJsonObject();
					JsonArray proofSigningAlgValuesSupported = proofTypeObject.getAsJsonArray("proof_signing_alg_values_supported");
					if (proofSigningAlgValuesSupported == null) {
						invalidEntries.computeIfAbsent(credentialKey, k -> new LinkedHashSet<>()).add("Missing proof_signing_alg_values_supported claim in proof type " + proofTypeKey + " object found in proof_types_supported object");
					}
				}
			}

			JsonElement displayEl = credentialConfiguration.get("display");
			if (displayEl != null) {
				JsonArray displayArray = displayEl.getAsJsonArray();
				int i = 0;
				for (var displayEntry : displayArray) {
					JsonObject displayObject = displayEntry.getAsJsonObject();
					String name = OIDFJSON.getString(displayObject.get("name"));
					if (name == null) {
						invalidEntries.computeIfAbsent(credentialKey, k -> new LinkedHashSet<>()).add("Missing name claim in display object entry " + i + " found in proof_types_supported object");
					}

					JsonObject logoObj = displayObject.getAsJsonObject("logo");
					if (logoObj != null) {
						if (logoObj.get("uri") == null) {
							invalidEntries.computeIfAbsent(credentialKey, k -> new LinkedHashSet<>()).add("Missing logo/uri claim in display object entry " + i + " found in proof_types_supported object");
						}
					}

					JsonObject backgroundImageObj = displayObject.getAsJsonObject("background_image");
					if (backgroundImageObj != null) {
						if (backgroundImageObj.get("uri") == null) {
							invalidEntries.computeIfAbsent(credentialKey, k -> new LinkedHashSet<>()).add("Missing background_image/uri claim in display object entry " + i + " found in proof_types_supported object");
						}
					}

					i++;
				}
			}
		}

		if (!invalidEntries.isEmpty()) {
			throw error("Found invalid entries in credential_configurations_supported", args("invalid_entries", invalidEntries));
		}

		logSuccess("Found valid credential_configurations_supported element", args("credential_configurations_supported", credentialConfigurationsSupported));

		return env;
	}
}

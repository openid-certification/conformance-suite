package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

public class SsfCheckTransmitterMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"transmitter_metadata"})
	public Environment evaluate(Environment env) {

		JsonObject transmitterMetadata = env.getObject("transmitter_metadata");

		if (transmitterMetadata.has("spec_version")) {
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
		}

		if (!transmitterMetadata.has("issuer")) {
			throw error("Couldn't find required issuer field in transmitter_metadata");
		}

		if (transmitterMetadata.has("jwks_uri")) {
			log("Found optional jwks_uri field");
			ensureHttpsUrl(transmitterMetadata, "jwks_uri");
		}

		if (transmitterMetadata.has("delivery_methods_supported")) {
			log("Found recommended delivery_methods_supported field");
		}

		if (transmitterMetadata.has("configuration_endpoint")) {
			log("Found optional configuration_endpoint field");
			String configurationEndpoint = ensureHttpsUrl(transmitterMetadata, "configuration_endpoint");

			env.putString("ssf", "configuration_endpoint", configurationEndpoint);
		}

		if (transmitterMetadata.has("status_endpoint")) {
			log("Found optional status_endpoint field");
			ensureHttpsUrl(transmitterMetadata, "status_endpoint");
		}

		if (transmitterMetadata.has("add_subject_endpoint")) {
			log("Found optional add_subject_endpoint field");
			ensureHttpsUrl(transmitterMetadata, "add_subject_endpoint");
		}

		if (transmitterMetadata.has("remove_subject_endpoint")) {
			log("Found optional remove_subject_endpoint field");
			ensureHttpsUrl(transmitterMetadata, "remove_subject_endpoint");
		}

		if (transmitterMetadata.has("verification_endpoint")) {
			log("Found optional verification_endpoint field");
			ensureHttpsUrl(transmitterMetadata, "verification_endpoint");
		}

		if (transmitterMetadata.has("critical_subject_members")) {
			log("Found optional critical_subject_members field");
		}

		if (transmitterMetadata.has("authorization_schemes")) {
			log("Found optional authorization_schemes field");
		}

		if (transmitterMetadata.has("default_subjects")) {
			log("Found optional default_subjects field");
			String defaultSubjects = OIDFJSON.getString(transmitterMetadata.get("default_subjects"));
			Set<String> allowedValues = Set.of("ALL", "NONE");
			if (!allowedValues.contains(defaultSubjects)) {
				throw error("Found invalid values for default_subjects, only " + allowedValues + " are allowed!",
					args("default_subjects", defaultSubjects));
			}
		}

		if (!transmitterMetadata.has("authorization_schemes")) {
			throw error("Missing required field authorization_schemes!");
		} else {
			// OIDSSF-6.1.1
			JsonArray authorizationSchemes = transmitterMetadata.getAsJsonArray("authorization_schemes");
			log("Found authorization_schemes", args("authorization_schemes", authorizationSchemes));

			// OIDCAEPIOP-2.3.7
			boolean rfc6749Found = false;
			for (var element : authorizationSchemes) {
				String specUrn = OIDFJSON.getString(element.getAsJsonObject().get("spec_urn"));
				if (specUrn.equals("urn:ietf:rfc:6749")) {
					rfc6749Found = true;
					break;
				}
			}
			if (!rfc6749Found) {
				throw error("Missing required authorization_scheme with spec_urn urn:ietf:rfc:6749");
			}
		}

		logSuccess("Successfully validated ssf configuration");

		return env;
	}

	private String ensureHttpsUrl(JsonObject transmitterMetadata, String uriField) {
		String uri = OIDFJSON.getString(transmitterMetadata.get(uriField));
		if (!uri.startsWith("https://")) {
			throw error(uriField + " must use https://!");
		}
		return uri;
	}
}

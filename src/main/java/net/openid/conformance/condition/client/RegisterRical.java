package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * Registers a RICAL (ISO/IEC 18013-5 second edition draft Annex F Reader Identity Certificate
 * Authority List) from the test configuration for later validation of the verifier's request
 * object signing certificate chain. The RICAL can be supplied either inline (base64-encoded
 * signed RICAL CBOR) or as an HTTPS URL to fetch it from.
 */
public class RegisterRical extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		// the config form keeps cleared fields as empty strings, so treat blank as absent
		String ricalB64 = Strings.emptyToNull(env.getString("config", "client.rical"));
		String ricalUrl = Strings.emptyToNull(env.getString("config", "client.rical_url"));

		if (ricalB64 != null && ricalUrl != null) {
			throw error("Only one of the 'RICAL' and 'RICAL URL' fields in the 'Client' section may be provided in the test configuration");
		}

		if (ricalB64 != null) {
			JsonObject rical = new JsonObject();
			rical.addProperty("value", ricalB64);
			env.putObject("rical", rical);
			logSuccess("Registered RICAL from test configuration");
		} else if (ricalUrl != null) {
			// Annex F describes RICAL distribution URLs as HTTPS; a plaintext fetch
			// must not be used as the source of trust decisions
			if (!ricalUrl.toLowerCase(java.util.Locale.ROOT).startsWith("https://")) {
				throw error("The 'RICAL URL' field in the 'Client' section of the test configuration must be an https:// URL",
					args("rical_url", ricalUrl));
			}
			env.putString("rical_url", ricalUrl);
			logSuccess("Registered RICAL URL from test configuration; the RICAL will be fetched from it",
				args("rical_url", ricalUrl));
		} else {
			log("No RICAL configured, skipping RICAL-based verifier trust checks");
		}

		return env;
	}
}

package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * Registers a VICAL (ISO/IEC 18013-5 Verified Issuer Certificate Authority List) from the test
 * configuration for later validation of mdoc issuer certificate chains. The VICAL can be supplied
 * either inline (base64-encoded signed VICAL CBOR) or as an HTTPS URL to fetch it from.
 */
public class RegisterVical extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		// the config form keeps cleared fields as empty strings, so treat blank as absent
		String vicalB64 = Strings.emptyToNull(env.getString("config", "credential.vical"));
		String vicalUrl = Strings.emptyToNull(env.getString("config", "credential.vical_url"));

		if (vicalB64 != null && vicalUrl != null) {
			throw error("Only one of the 'VICAL' and 'VICAL URL' fields in the 'Credential Issuer' section may be provided in the test configuration");
		}

		if (vicalB64 != null) {
			JsonObject vical = new JsonObject();
			vical.addProperty("value", vicalB64);
			env.putObject("vical", vical);
			logSuccess("Registered VICAL from test configuration");
		} else if (vicalUrl != null) {
			// ISO/IEC 18013-5 defines VICAL distribution URLs as HTTPS; a plaintext fetch
			// must not be used as the source of trust decisions
			if (!vicalUrl.toLowerCase(java.util.Locale.ROOT).startsWith("https://")) {
				throw error("The 'VICAL URL' field in the 'Credential Issuer' section of the test configuration must be an https:// URL",
					args("vical_url", vicalUrl));
			}
			env.putString("vical_url", vicalUrl);
			logSuccess("Registered VICAL URL from test configuration; the VICAL will be fetched from it",
				args("vical_url", vicalUrl));
		} else {
			log("No VICAL configured, skipping VICAL-based mdoc issuer trust checks");
		}

		return env;
	}
}

package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateJWEHeaderEncMatchesRequestedAlgorithm extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"response_jwe", "authorization_endpoint_request"})
	public Environment evaluate(Environment env) {
		String enc = env.getString("response_jwe", "jwe_header.enc");

		if (enc == null) {
			throw error("JWE header enc is absent");
		}

		JsonElement encValuesSupportedEl = env.getElementFromObject("authorization_endpoint_request",
			"client_metadata.encrypted_response_enc_values_supported");

		if (encValuesSupportedEl == null) {
			// Per OID4VP section 8.3, when encrypted_response_enc_values_supported is absent,
			// the default is A128GCM
			if (!"A128GCM".equals(enc)) {
				throw error("JWE header enc does not match the default algorithm; when "
					+ "encrypted_response_enc_values_supported is absent the default is A128GCM",
					args("expected", "A128GCM", "actual", enc));
			}
			logSuccess("JWE header enc matches default algorithm A128GCM");
			return env;
		}

		JsonArray encValuesSupported = encValuesSupportedEl.getAsJsonArray();
		for (JsonElement value : encValuesSupported) {
			if (enc.equals(OIDFJSON.getString(value))) {
				logSuccess("JWE header enc matches one of the requested algorithms",
					args("enc", enc, "encrypted_response_enc_values_supported", encValuesSupported));
				return env;
			}
		}

		throw error("JWE header enc does not match any of the algorithms in "
			+ "encrypted_response_enc_values_supported in the authorization request client_metadata",
			args("enc", enc, "encrypted_response_enc_values_supported", encValuesSupported));
	}

}

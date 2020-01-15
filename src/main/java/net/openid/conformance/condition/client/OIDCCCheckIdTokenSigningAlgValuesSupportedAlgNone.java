package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

public class OIDCCCheckIdTokenSigningAlgValuesSupportedAlgNone extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonElement idTokenSigningAlgSupported = env.getElementFromObject("server", "id_token_signing_alg_values_supported");

		String errorMessage = null;

		if (idTokenSigningAlgSupported == null) {

			errorMessage = "'id_token_signing_alg_values_supported' is null";

		} else if(!idTokenSigningAlgSupported.isJsonArray()) {

			errorMessage = "'id_token_signing_alg_values_supported' is not a json array";

		} else {

			// convert JsonArray idTokenSigningAlgSupported to list string
			List<String> idTokenSigningAlgSupportedList = new ArrayList<>();
			for (JsonElement alg : idTokenSigningAlgSupported.getAsJsonArray()) {
				idTokenSigningAlgSupportedList.add(OIDFJSON.getString(alg));
			}

			if (!idTokenSigningAlgSupportedList.contains("none")) {
				errorMessage = "'id_token_signing_alg_values_supported' doesn't contain 'none' algorithm'";
			}
		}

		if (errorMessage != null) {
			// skip test when id_token_signing_alg_values_supported is not supported 'none' algorithm
			env.putBoolean("id_token_signing_alg_not_supported_flag", true);
			throw error(errorMessage, args("id_token_signing_alg_values_supported", idTokenSigningAlgSupported));
		}

		logSuccess("'id_token_signing_alg_values_supported' contain 'none' algorithm", args("id_token_signing_alg_values_supported", idTokenSigningAlgSupported));

		return env;
	}

}

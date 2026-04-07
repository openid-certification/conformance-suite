package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Warns if the cnf claim in an SD-JWT credential contains unexpected fields beyond 'jwk'.
 */
public class WarnIfUnexpectedFieldsInCredentialCnf extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt"})
	public Environment evaluate(Environment env) {

		JsonElement cnfEl = env.getElementFromObject("sdjwt", "credential.claims.cnf");
		if (cnfEl == null || !cnfEl.isJsonObject()) {
			log("No cnf claim found, skipping check");
			return env;
		}

		JsonObject cnf = cnfEl.getAsJsonObject();
		List<String> unexpectedFields = new ArrayList<>();
		for (String key : cnf.keySet()) {
			if (!"jwk".equals(key)) {
				unexpectedFields.add(key);
			}
		}

		if (!unexpectedFields.isEmpty()) {
			throw error("cnf claim contains unexpected fields beyond 'jwk'",
				args("unexpected_fields", unexpectedFields));
		}

		logSuccess("cnf claim contains only expected fields");
		return env;
	}
}

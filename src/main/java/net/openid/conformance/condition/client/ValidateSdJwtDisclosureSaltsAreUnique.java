package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates that all disclosures in an SD-JWT use unique salt values.
 *
 * Per SD-JWT spec section 4.2.1, each disclosure MUST use a unique salt to prevent
 * correlation between disclosures.
 */
public class ValidateSdJwtDisclosureSaltsAreUnique extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "sdjwt" })
	public Environment evaluate(Environment env) {

		JsonArray disclosures = env.getElementFromObject("sdjwt", "disclosures").getAsJsonArray();

		if (disclosures.size() <= 1) {
			logSuccess("Only %d disclosure(s), no duplicates possible".formatted(disclosures.size()));
			return env;
		}

		Set<String> salts = new HashSet<>();
		for (JsonElement disclosureEl : disclosures) {
			// Each disclosure is a JSON array string: ["salt", "claim_name", "claim_value"]
			JsonArray disclosure = JsonParser.parseString(OIDFJSON.getString(disclosureEl)).getAsJsonArray();
			String salt = OIDFJSON.getString(disclosure.get(0));
			if (!salts.add(salt)) {
				throw error("Duplicate salt found in SD-JWT disclosures",
					args("duplicate_salt", salt, "disclosures", disclosures));
			}
		}

		logSuccess("All %d disclosures have unique salts".formatted(disclosures.size()));
		return env;
	}
}

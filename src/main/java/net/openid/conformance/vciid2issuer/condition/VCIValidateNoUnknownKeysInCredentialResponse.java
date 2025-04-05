package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;

/**
 * See: 8.3. Credential Response https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.3
 */
public class VCIValidateNoUnknownKeysInCredentialResponse extends AbstractCondition {

	@SuppressWarnings("unused")
	@Override
	public Environment evaluate(Environment env) {

		JsonObject credentialResponseBodyJson = env.getElementFromObject("endpoint_response", "body_json").getAsJsonObject();
		Set<String> expected = Set.of("credentials", "transaction_id", "notification_id");

		Set<String> unexpected = credentialResponseBodyJson.keySet();
		unexpected.removeAll(expected);

		if (!unexpected.isEmpty()) {
			throw error("Unexpected keys in credential response", args("unexpected", unexpected, "expected", expected));
		}

		logSuccess("No unexpected keys in credential response", args("expected", expected));

		return env;
	}
}

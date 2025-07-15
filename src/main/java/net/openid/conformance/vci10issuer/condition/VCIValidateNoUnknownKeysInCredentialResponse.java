package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

/**
 * See: 8.3. Credential Response https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.3
 */
public class VCIValidateNoUnknownKeysInCredentialResponse extends AbstractCondition {

	@SuppressWarnings("unused")
	@Override
	public Environment evaluate(Environment env) {

		JsonObject endpointResponse = env.getObject("endpoint_response").getAsJsonObject();
		JsonObject credentialResponseBodyJson = JsonParser.parseString(OIDFJSON.getString(endpointResponse.get("body"))).getAsJsonObject();
		Set<String> expected = Set.of("credentials", "transaction_id", "notification_id", "interval");

		Set<String> unexpected = credentialResponseBodyJson.keySet();
		unexpected.removeAll(expected);

		if (!unexpected.isEmpty()) {
			throw error("Unexpected keys in credential response. This may indicate the issuer has misunderstood the spec, or it may be using extensions the test suite is unaware of.", args("unexpected", unexpected, "expected", expected));
		}

		logSuccess("No unexpected keys in credential response", args("expected", expected));

		return env;
	}
}

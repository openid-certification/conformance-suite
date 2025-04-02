package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * See: 8.3. Credential Response https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.3
 */
public class VCIValidateCredentialResponse extends AbstractCondition {

	@SuppressWarnings("unused")
	@Override
	public Environment evaluate(Environment env) {

		JsonObject endpointResponse = env.getObject("endpoint_response").getAsJsonObject();

		JsonObject credentialResponseBodyJson = env.getElementFromObject("endpoint_response", "body_json").getAsJsonObject();

		// TODO validate credential response

		logSuccess("Extracted credential response", args("credential", credentialResponseBodyJson));

		return env;
	}
}

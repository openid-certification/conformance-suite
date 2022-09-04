package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
//TODO I could not see an equivalent check for FAPI
public class EnsureMTLSRequestContainsValidClientId extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"token_endpoint_request", "client"})
	public Environment evaluate(Environment env) {

		String clientId = env.getString("token_endpoint_request", "body_form_params.client_id");

		if (Strings.isNullOrEmpty(clientId)) {
			throw error("Couldn't find client_id in form parameters");
		}
		String expectedClientId = env.getString("client", "client_id");
		if(!expectedClientId.equals(clientId)) {
			throw error("client_id in request does not match the expected client_id",
						args("actual", clientId, "expected", expectedClientId));
		}
		logSuccess("Request parameters contain a valid client_id parameter");
		return env;
	}

}

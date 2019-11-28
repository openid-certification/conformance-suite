package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientIdAndSecret extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client_authentication", "client" })
	public Environment evaluate(Environment env)
	{
		String clientIdFromRequest = env.getString("client_authentication", "client_id");
		String clientSecretFromRequest = env.getString("client_authentication", "client_secret");

		String expectedClientId = env.getString("client", "client_id");
		String expectedClientSecret = env.getString("client", "client_secret");

		if (Strings.isNullOrEmpty(clientIdFromRequest)) {
			throw error("Couldn't find client id in request",
						args("client_authentication", env.getObject("client_authentication")));
		}

		if(expectedClientId.equals(clientIdFromRequest) && expectedClientSecret.equals(clientSecretFromRequest)) {
			logSuccess("Client id and secret match");
			return env;
		}
		throw error("Client authentication failed", args("expected_client_id", expectedClientId,
																	"received_client_id", clientIdFromRequest,
																	"expected_client_secret", expectedClientSecret,
																	"received_client_secret", clientSecretFromRequest
			));
	}
}

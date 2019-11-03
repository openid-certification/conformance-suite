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
			throw error("Couldn't find client id in request");
		}

		if(expectedClientId.equals(clientIdFromRequest) && expectedClientSecret.equals(clientSecretFromRequest)) {
			logSuccess("Client id and secret match");
			return env;
		}
		throw error("Client authentication failed", args("expected client id", expectedClientId,
																	"received client id", clientIdFromRequest,
																	"expected client secret", expectedClientSecret,
																	"received client secret", clientSecretFromRequest
			));
	}
}

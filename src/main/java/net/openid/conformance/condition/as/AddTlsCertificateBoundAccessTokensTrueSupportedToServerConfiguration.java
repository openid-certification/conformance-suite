package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddTlsCertificateBoundAccessTokensTrueSupportedToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");
		server.addProperty("tls_client_certificate_bound_access_tokens", true);

		logSuccess("Added 'tls_client_certificate_bound_access_tokens' as 'true' to server metadata");

		return env;
	}
}

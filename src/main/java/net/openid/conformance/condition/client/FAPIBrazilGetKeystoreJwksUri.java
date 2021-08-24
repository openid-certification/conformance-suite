package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilGetKeystoreJwksUri extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config"})
	public Environment evaluate(Environment env) {
		String keystore = getStringFromEnvironment(env, "config", "directory.keystore",
			"Directory Keystore base in test configuration");

		String orgId = env.getString("config", "resource.brazilOrganizationId");
		if (Strings.isNullOrEmpty(orgId)) {
			throw error("Resource server organization id missing from test configuration; this must be provided when 'scope' contains payments");
		}

		String orgJwks = keystore + orgId + "/application.jwks";

		JsonObject orgServer = new JsonObject();
		orgServer.addProperty("jwks_uri", orgJwks);
		env.putObject("org_server", orgServer);

		logSuccess("Determined organisation jwks uri", args("org_jwks_uri", orgJwks));
		return env;
	}
}

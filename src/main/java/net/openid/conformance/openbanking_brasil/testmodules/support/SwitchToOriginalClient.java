package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SwitchToOriginalClient extends AbstractCondition {
	@Override
	@PreEnvironment(required = {"original_client", "original_mutual_tls_authentication"}, strings = "original_redirect_uri")
	public Environment evaluate(Environment env) {
		JsonObject originalClient = env.getObject("original_client").deepCopy();
		JsonObject originalMtls = env.getObject("original_mutual_tls_authentication").deepCopy();
		String originalRedirectUri = env.getString("original_redirect_uri");
		env.putObject("client", originalClient);
		env.putObject("mutual_tls_authentication", originalMtls);
		env.putString("redirect_uri", originalRedirectUri);

		logSuccess("Switched to original client", args("Client", originalClient, "MTLS", originalMtls ,"Redirect URI", originalRedirectUri));
		return env;
	}
}

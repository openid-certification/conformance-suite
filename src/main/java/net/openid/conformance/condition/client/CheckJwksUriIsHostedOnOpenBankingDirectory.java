package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.URL;

public class CheckJwksUriIsHostedOnOpenBankingDirectory extends AbstractJsonUriIsValidAndHttps {

	private static final String requiredHostname = "keystore.openbanking.org.uk";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonElement server = super.getServerValueOrDie(env, "jwks_uri");
		URL theURL = super.extractURLOrDie(server);

		if (!theURL.getHost().equals(requiredHostname)) {
			throw error("JWKS URI is not hosted on the OpenBanking Directory. This is acceptable on a sandbox, but production systems must use an OpenBanking Directory hosted JWKS.", args("expected", requiredHostname, "actual", theURL.getHost()));
		} else {
			logSuccess("JWKS is hosted on the OpenBanking Directory", args("required", requiredHostname, "actual", theURL.getHost()));
		}

		return env;
	}
}

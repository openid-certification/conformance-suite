package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonElement;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.net.URL;

public class CheckJwksUriIsHostedOnOpenBankingDirectory extends ValidateJsonUri {

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

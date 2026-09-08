package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SaveMutualTLsAuthenticationToConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = "mutual_tls_authentication")
	@PostEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		String certString = env.getString("mutual_tls_authentication", "cert");
		String keyString = env.getString("mutual_tls_authentication", "key");
		String caString = env.getString("mutual_tls_authentication", "ca");

		JsonObject mtls = new JsonObject();
		mtls.addProperty("cert", certString);
		mtls.addProperty("key", keyString);
		if (caString != null) {
			mtls.addProperty("ca", caString);
		}

		// these are not PEM encoded, need to be careful if expecting config.mtls to be PEM format
		// ExtractMTLSCertificatesFromConfiguration already takes this into account
		env.putObject("config", "mtls", mtls);

		logSuccess("Mutual TLS authentication credentials saved to config", mtls);

		return env;
	}

}

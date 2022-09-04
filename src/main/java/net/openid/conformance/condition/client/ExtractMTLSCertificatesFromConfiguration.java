package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.PEMFormatter;
import net.openid.conformance.testmodule.Environment;

public class ExtractMTLSCertificatesFromConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "mutual_tls_authentication")
	public Environment evaluate(Environment env) {
		// mutual_tls_authentication

		String certString = env.getString("config", "mtls.cert");
		String keyString = env.getString("config", "mtls.key");
		String caString = env.getString("config", "mtls.ca");

		if (Strings.isNullOrEmpty(certString) || Strings.isNullOrEmpty(keyString)) {
			throw error("Couldn't find TLS client certificate or key for MTLS");
		}

		if (Strings.isNullOrEmpty(caString)) {
			// Not an error; we just won't send a CA chain
			log("No certificate authority found for MTLS");
		}

		try {
			certString = PEMFormatter.stripPEM(certString);

			keyString = PEMFormatter.stripPEM(keyString);

			if (caString != null) {
				caString = PEMFormatter.stripPEM(caString);
			}
		} catch (IllegalArgumentException e) {
			throw error("Couldn't decode certificate, key, or CA chain from Base64", e, args("cert", certString, "key", keyString, "ca", Strings.emptyToNull(caString)));
		}

		JsonObject mtls = new JsonObject();
		mtls.addProperty("cert", certString);
		mtls.addProperty("key", keyString);
		if (caString != null) {
			mtls.addProperty("ca", caString);
		}

		env.putObject("mutual_tls_authentication", mtls);

		logSuccess("Mutual TLS authentication credentials loaded", mtls);

		return env;
	}

}

package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.util.PEMFormatter;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ExtractMTLSCertificatesFromConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "mutual_tls_authentication")
	public Environment evaluate(Environment env) {
		// mutual_tls_authentication

		boolean usingAlternateKeyConfig = false;
		String certString = env.getString("config", "mtls.cert");
		String keyString = null;
		JsonElement keyElement = env.getElementFromObject("config", "mtls.key");
		if(keyElement instanceof JsonObject) {
			JsonObject mtlsKey = (JsonObject) keyElement;
			env.putObject("mtls_alternate_key", mtlsKey.getAsJsonObject("alternateKeystore"));
			keyString = "see.mtls_alternate_key";
			usingAlternateKeyConfig = true;
		} else if(keyElement != null){
			keyString = OIDFJSON.getString(keyElement);
		}
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

			if(!usingAlternateKeyConfig){
				keyString = PEMFormatter.stripPEM(keyString);
			}

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

package net.openid.conformance.condition.client;

import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractMDocGeneratedNonceFromJWEHeaderApu extends AbstractCheckUnpaddedBase64Url {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		String apu = env.getString("response_jwe", "jwe_header.apu");

		if (apu == null) {
			throw error("JWE header apu is absent");
		}

		checkUnpaddedBase64Url(apu, "apu");

		String decodedApu = new Base64URL(apu).decodeToString();

		env.putString("mdoc_generated_nonce", decodedApu);

		logSuccess("MDocGeneratedNonce extracted from JWE header apu value",
			args("mdoc_generated_nonce", decodedApu,
				"apu", apu));

		return env;
	}

}

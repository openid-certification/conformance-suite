package net.openid.conformance.condition.client;

import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateJWEHeaderApvIsAuthRequestNonce extends AbstractCheckUnpaddedBase64Url {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		String apv = env.getString("response_jwe", "jwe_header.apv");

		if (apv == null) {
			throw error("JWE header apv is absent");
		}

		checkUnpaddedBase64Url(apv, "apv");

		String decodedApv = new Base64URL(apv).decodeToString();

		String nonce = env.getString("nonce");
		if (!decodedApv.equals(nonce)) {
			throw error("JWE header apv value must be nonce from authorization request",
				args("expected", nonce, "actual", decodedApv, "apv_b64", apv));
		}

		logSuccess("JWE header apv value is base64url encoded nonce from authorization request");
		return env;
	}

}

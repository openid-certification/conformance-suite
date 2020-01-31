package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * userinfo_encrypted_response_enc
 * OPTIONAL. JWE enc algorithm [JWA] REQUIRED for encrypting UserInfo Responses.
 * If userinfo_encrypted_response_alg is specified, the default for this value is A128CBC-HS256.
 * When userinfo_encrypted_response_enc is included, userinfo_encrypted_response_alg MUST also be provided.
 *
 */
public class EnsureUserinfoEncryptedResponseAlgIsSetIfEncIsSet extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");
		String enc = getUserinfoEncryptedResponseEnc();
		String alg = getUserinfoEncryptedResponseAlg();
		if(enc!=null && alg==null) {
			throw error("When userinfo_encrypted_response_enc is included, userinfo_encrypted_response_alg MUST " +
						"also be provided.",
						args("userinfo_encrypted_response_alg", alg, "userinfo_encrypted_response_enc", enc));
		}
		if(enc == null) {
			logSuccess("userinfo_encrypted_response_enc is not set");
			return env;
		}
		logSuccess("userinfo_encrypted_response_alg is set",
					args("userinfo_encrypted_response_alg", alg, "userinfo_encrypted_response_enc", enc));
		return env;
	}
}

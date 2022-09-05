package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * id_token_encrypted_response_enc
 * OPTIONAL. JWE enc algorithm [JWA] REQUIRED for encrypting the ID Token issued to this Client.
 * If id_token_encrypted_response_alg is specified, the default for this value is A128CBC-HS256.
 * When id_token_encrypted_response_enc is included, id_token_encrypted_response_alg MUST also be provided.
 *
 */
public class EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");
		String enc = getIdTokenEncryptedResponseEnc();
		String alg = getIdTokenEncryptedResponseAlg();
		if(enc!=null && alg==null) {
			throw error("When id_token_encrypted_response_enc is included, id_token_encrypted_response_alg MUST " +
						"also be provided.",
						args("id_token_encrypted_response_alg", alg, "id_token_encrypted_response_enc", enc));
		}
		if(enc == null) {
			logSuccess("id_token_encrypted_response_enc is not set");
			return env;
		}
		logSuccess("id_token_encrypted_response_alg is set",
					args("id_token_encrypted_response_alg", alg, "id_token_encrypted_response_enc", enc));
		return env;
	}
}

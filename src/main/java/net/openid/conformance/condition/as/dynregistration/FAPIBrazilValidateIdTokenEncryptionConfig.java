package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilValidateIdTokenEncryptionConfig extends AbstractClientValidationCondition {

	private static final String REQUIRED_ALGORITHM = "RSA-OAEP";
	private static final String REQUIRED_ENCRYPTION_METHOD = "A256GCM";

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		client = env.getObject("client");
		String algorithm = getIdTokenEncryptedResponseAlg();
		String encryptionMethod = getIdTokenEncryptedResponseEnc();

		if (!REQUIRED_ALGORITHM.equals(algorithm)
			|| !REQUIRED_ENCRYPTION_METHOD.equals(encryptionMethod)) {
			throw error("Open Finance Brazil requires ID Token encryption using RSA-OAEP with A256GCM",
				args("id_token_encrypted_response_alg", algorithm,
					"id_token_encrypted_response_enc", encryptionMethod,
					"required_id_token_encrypted_response_alg", REQUIRED_ALGORITHM,
					"required_id_token_encrypted_response_enc", REQUIRED_ENCRYPTION_METHOD));
		}

		logSuccess("ID Token encryption metadata uses RSA-OAEP with A256GCM",
			args("id_token_encrypted_response_alg", algorithm,
				"id_token_encrypted_response_enc", encryptionMethod));
		return env;
	}
}

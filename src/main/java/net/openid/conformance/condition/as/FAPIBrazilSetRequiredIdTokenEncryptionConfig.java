package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilSetRequiredIdTokenEncryptionConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		env.putString("client", "id_token_encrypted_response_alg", "RSA-OAEP");
		env.putString("client", "id_token_encrypted_response_enc", "A256GCM");

		logSuccess("Set Open Finance Brazil required ID Token encryption configuration",
			args("id_token_encrypted_response_alg", "RSA-OAEP",
				"id_token_encrypted_response_enc", "A256GCM"));
		return env;
	}

}

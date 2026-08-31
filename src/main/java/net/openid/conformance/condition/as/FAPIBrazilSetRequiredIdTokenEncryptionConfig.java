package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class FAPIBrazilSetRequiredIdTokenEncryptionConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		String previousAlg = env.getString("client", "id_token_encrypted_response_alg");
		String previousEnc = env.getString("client", "id_token_encrypted_response_enc");
		env.putString("client", "id_token_encrypted_response_alg", "RSA-OAEP");
		env.putString("client", "id_token_encrypted_response_enc", "A256GCM");

		Map<String, Object> details = args(
			"id_token_encrypted_response_alg", "RSA-OAEP",
			"id_token_encrypted_response_enc", "A256GCM");
		boolean overridesNonconformantValue = previousAlg != null && !"RSA-OAEP".equals(previousAlg)
			|| previousEnc != null && !"A256GCM".equals(previousEnc);
		if (overridesNonconformantValue) {
			details.put("previous_id_token_encrypted_response_alg", previousAlg);
			details.put("previous_id_token_encrypted_response_enc", previousEnc);
			logSuccess("Overrode nonconformant ID Token encryption configuration required by Open Finance Brazil",
				details);
		} else {
			logSuccess("Set Open Finance Brazil required ID Token encryption configuration", details);
		}
		return env;
	}

}

package net.openid.conformance.condition.as;

import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.security.SecureRandom;

public class CreateMDocGeneratedNonce extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "mdoc_generated_nonce")
	public Environment evaluate(Environment env) {
		byte[] nonceBytes = new byte[16];
		new SecureRandom().nextBytes(nonceBytes);
		String mdocGeneratedNonce = Base64URL.encode(nonceBytes).toString();
		env.putString("mdoc_generated_nonce", mdocGeneratedNonce);

		log("Created mdoc generated nonce",
			args("mdoc_generated_nonce", mdocGeneratedNonce));

		return env;
	}

}

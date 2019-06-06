package io.fintechlabs.testframework.condition.as;

import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;
import java.text.ParseException;

public class SignIdTokenInvalid extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "id_token")
	@PostEnvironment(strings = "id_token")
	public Environment evaluate(Environment env) {

		String idTokenString = env.getString("id_token");

		try {
			SignedJWT idToken = SignedJWT.parse(idTokenString);

			Base64URL signature = idToken.getSignature();

			byte[] bytes = signature.decode();

			//Flip some of the bits in the signature to make it invalid
			for (int i = 0; i < bytes.length; i++) {
				bytes[i] ^= 0x5A;
			}

			Base64URL invalidSignature = Base64URL.encode(bytes);

			//Rebuild the JWT using Base64URL
			Base64URL[] idTokenParsedParts = idToken.getParsedParts();
			SignedJWT invalidIdToken = new SignedJWT(
				idTokenParsedParts[0],
				idTokenParsedParts[1],
				invalidSignature);

			env.putString("id_token", invalidIdToken.serialize());

			logSuccess("Made the id_token signature invalid", args("id_token", invalidIdToken.serialize()));

			return env;

		} catch (ParseException e) {
			throw error("Couldn't parse JWT", e, args("id_token", idTokenString));
		}

	}

}

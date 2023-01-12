package net.openid.conformance.condition.common;

import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public abstract class AbstractInvalidateJwsSignature extends AbstractCondition {

	public Environment invalidateSignature(Environment env, String environmentKey) {

		String jwtString = env.getString(environmentKey);

		String invalidJwtString = invalidateSignatureString(environmentKey, jwtString);

		env.putString(environmentKey, invalidJwtString);

		log("Made the "+environmentKey+" signature invalid", args(environmentKey, invalidJwtString));

		return env;

	}

	protected String invalidateSignatureString(String environmentKey, String jwtString) {
		try {
			SignedJWT parsedJwt = SignedJWT.parse(jwtString);

			Base64URL signature = parsedJwt.getSignature();

			byte[] bytes = signature.decode();

			//Flip some of the bits in the signature to make it invalid
			for (int i = 0; i < bytes.length; i++) {
				bytes[i] ^= 0x5A;
			}

			Base64URL invalidSignature = Base64URL.encode(bytes);

			//Rebuild the JWT using Base64URL
			Base64URL[] parsedJwtParsedParts = parsedJwt.getParsedParts();
			SignedJWT invalidJwt = new SignedJWT(
				parsedJwtParsedParts[0],
				parsedJwtParsedParts[1],
				invalidSignature);

			return invalidJwt.serialize();

		} catch (ParseException e) {

			throw error("Couldn't parse JWT", e, args(environmentKey, jwtString));
		}
	}
}

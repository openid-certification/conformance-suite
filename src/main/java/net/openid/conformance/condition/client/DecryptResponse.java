package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWEUtil;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class DecryptResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "original_authorization_endpoint_response")
	@PostEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject formBody = env.getObject("original_authorization_endpoint_response");
		JsonElement responseEl = formBody.get("response");
		if (responseEl == null) {
			throw error("Incoming auth response does not contain 'response' parameter");
		}
		String tokenString = OIDFJSON.getString(responseEl);

		JsonObject clientJwks = env.getObject("client_jwks");

		try {
			JWT token = JWTUtil.parseJWT(tokenString);
			if(!(token instanceof EncryptedJWT)) {
				throw error("'response' parameter does not contain an encrypted JWT");
			}
			EncryptedJWT encryptedJWT = (EncryptedJWT) token;
			JsonObject jweHeader = JWTUtil.jwtHeaderAsJsonObject(token);
			JWEAlgorithm alg = encryptedJWT.getHeader().getAlgorithm();

			if (clientJwks == null) {
				throw new ParseException("A JWKS is required to decrypt this JWT", 0);
			}
			JWKSet jwkSet = JWKUtil.parseJWKSet(clientJwks.toString());
			JWK decryptionKey = JWEUtil.selectAsymmetricKeyForEncryption(jwkSet, alg);
			if (decryptionKey == null) {
				throw new ParseException("No suitable key for decrypting this JWT was provided in the test configuration.", 0);
			}
			// FIXME try client_metadata without alg / key use / ...?
			// FIXME negative test for session transcript
			JWEDecrypter decrypter = JWEUtil.createDecrypter(alg, decryptionKey);
			encryptedJWT.decrypt(decrypter);
			String decryptedPayload = encryptedJWT.getPayload().toString();
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("value", tokenString);
			jsonObject.add("jwe_header", jweHeader);
			JsonObject claims = JsonParser.parseString(decryptedPayload).getAsJsonObject();
			jsonObject.add("claims", claims);

			env.putObject("response_jwe", jsonObject);

			env.putObject("authorization_endpoint_response", claims);

			env.putObject("decryption_jwk", JsonParser.parseString(decryptionKey.toJSONString()).getAsJsonObject());

			logSuccess("Found and decrypted the authorization response", jsonObject);

			return env;

		} catch (ParseException e) {
			throw error("Couldn't parse encrypted response as a JWT", e, args("jarm_response", tokenString));
		} catch (JOSEException e) {
			throw error("Decrypting encrypted response failed", e, args("jarm_response", tokenString));
		}

	}

}

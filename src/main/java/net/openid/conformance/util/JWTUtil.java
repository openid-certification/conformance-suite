package net.openid.conformance.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import net.openid.conformance.testmodule.OIDFJSON;

import java.text.ParseException;
import java.util.UUID;

public class JWTUtil {

	/**
	 * wrapper for Nimbus JWTParser
	 * just in case we want to override something one day
	 * @param jwtAsString
	 * @return
	 * @throws ParseException
	 */
	public static JWT parseJWT(String jwtAsString) throws ParseException {
		JWT jwt = JWTParser.parse(jwtAsString);
		return jwt;
	}

	/**
	 * Also see jwtStringToJsonObjectForEnvironment
	 * @param jwt
	 * @return
	 * @throws ParseException
	 */
	public static JsonObject jwtClaimsSetAsJsonObject(JWT jwt) throws ParseException {
		//added this variable to make it obvious
		boolean includeNullValues = true;
		JsonObject claims = new JsonParser().parse(jwt.getJWTClaimsSet().toJSONObject(includeNullValues).toJSONString()).getAsJsonObject();
		return claims;
	}

	/**
	 * Also see jwtStringToJsonObjectForEnvironment
	 * Note: Nimbusds will always remove null values from JWT headers
	 * @param jwt
	 * @return
	 */
	public static JsonObject jwtHeaderAsJsonObject(JWT jwt) {
		JsonObject header = new JsonParser().parse(jwt.getHeader().toJSONObject().toJSONString()).getAsJsonObject();
		return header;
	}

	/**
	 * Parses the JWT and returns a JsonObject with value, header and claims entries
	 * @param jwtAsString
	 * @return
	 * @throws ParseException
	 */
	public static JsonObject jwtStringToJsonObjectForEnvironment(String jwtAsString) throws ParseException {
		JWT token = JWTUtil.parseJWT(jwtAsString);

		JsonObject header = JWTUtil.jwtHeaderAsJsonObject(token);
		JsonObject claims = JWTUtil.jwtClaimsSetAsJsonObject(token);

		return createJsonObjectForEnvironment(jwtAsString, header, claims);
	}

	private static JsonObject createJsonObjectForEnvironment(String jwtString, JsonObject header, JsonObject claims) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("value", jwtString);
		jsonObject.add("header", header);
		jsonObject.add("claims", claims);
		return jsonObject;
	}

	/**
	 * use if the JWT may be encrypted, e.g an encrypted request object
	 * @param jwtAsString
	 * @param client
	 * @param publicJwksWithEncKeys
	 * @return may return null if decryption fails
	 * @throws ParseException
	 */
	public static JsonObject jwtStringToJsonObjectForEnvironment(String jwtAsString, JsonObject client, JsonObject publicJwksWithEncKeys)
		throws ParseException, JOSEException {
		JWT token = JWTUtil.parseJWT(jwtAsString);
		if(token instanceof EncryptedJWT) {
			EncryptedJWT encryptedJWT = (EncryptedJWT) token;
			JWEAlgorithm alg = encryptedJWT.getHeader().getAlgorithm();
			JWK decryptionKey = null;
			if(JWEAlgorithm.Family.SYMMETRIC.contains(alg)) {
				decryptionKey = JWEUtil.createSymmetricJWKForAlgAndSecret(OIDFJSON.getString(client.get("client_secret")),
										alg, encryptedJWT.getHeader().getEncryptionMethod(), UUID.randomUUID().toString());
			} else {
				JWKSet jwkSet = JWKUtil.parseJWKSet(publicJwksWithEncKeys.toString());
				decryptionKey = JWEUtil.selectAsymmetricKeyForEncryption(jwkSet, alg);
			}
			JWEDecrypter decrypter = null;
			decrypter = JWEUtil.createDecrypter(decryptionKey);
			encryptedJWT.decrypt(decrypter);
			return JWTUtil.jwtStringToJsonObjectForEnvironment(encryptedJWT.getPayload().toString());
		} else {
			JsonObject header = JWTUtil.jwtHeaderAsJsonObject(token);
			JsonObject claims = JWTUtil.jwtClaimsSetAsJsonObject(token);
			return createJsonObjectForEnvironment(jwtAsString, header, claims);
		}
	}

}

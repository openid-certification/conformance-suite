package net.openid.conformance.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
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
		JWTClaimsSet jwtClaimsSet = jwt.getJWTClaimsSet();
		if (jwtClaimsSet == null) {
			throw new ParseException("Failed to extract JWT claims", 0);
		}
		JsonObject claims = new JsonParser().parse(jwtClaimsSet.toJSONObject(includeNullValues).toJSONString()).getAsJsonObject();
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

		if(token instanceof EncryptedJWT) {
			throw new ParseException("EncryptedJWT found, which this test currently doesn't support", 0);
		}
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
	 *
	 * @param client Client object containing client_secret, required if symmetric encryption used
	 * @param privateJwksWithEncKeys Key for decryption, if encryption used
	 * @return may return null if decryption fails
	 */
	public static JsonObject jwtStringToJsonObjectForEnvironment(String jwtAsString, JsonObject client, JsonObject privateJwksWithEncKeys)
		throws ParseException, JOSEException {
		JWT token = JWTUtil.parseJWT(jwtAsString);
		if(token instanceof EncryptedJWT) {
			EncryptedJWT encryptedJWT = (EncryptedJWT) token;
			JsonObject jweHeader = JWTUtil.jwtHeaderAsJsonObject(token);
			JWEAlgorithm alg = encryptedJWT.getHeader().getAlgorithm();
			JWK decryptionKey = null;
			if(JWEAlgorithm.Family.SYMMETRIC.contains(alg)) {
				String client_secret = OIDFJSON.getString(client.get("client_secret"));
				if (client_secret == null) {
					throw new ParseException("A client secret is required to decrypt this JWT", 0);
				}
				decryptionKey = JWEUtil.createSymmetricJWKForAlgAndSecret(client_secret,
										alg, encryptedJWT.getHeader().getEncryptionMethod(), UUID.randomUUID().toString());
			} else {
				if (privateJwksWithEncKeys == null) {
					throw new ParseException("A JWKS is required to decrypt this JWT", 0);
				}
				JWKSet jwkSet = JWKUtil.parseJWKSet(privateJwksWithEncKeys.toString());
				decryptionKey = JWEUtil.selectAsymmetricKeyForEncryption(jwkSet, alg);
			}
			JWEDecrypter decrypter = JWEUtil.createDecrypter(decryptionKey);
			encryptedJWT.decrypt(decrypter);
			JsonObject out = JWTUtil.jwtStringToJsonObjectForEnvironment(encryptedJWT.getPayload().toString());
			out.add("jwe_header", jweHeader);
			return out;
		} else {
			JsonObject header = JWTUtil.jwtHeaderAsJsonObject(token);
			JsonObject claims = JWTUtil.jwtClaimsSetAsJsonObject(token);
			return createJsonObjectForEnvironment(jwtAsString, header, claims);
		}
	}

}

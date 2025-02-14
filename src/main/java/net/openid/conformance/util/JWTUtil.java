package net.openid.conformance.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObject;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import net.openid.conformance.testmodule.OIDFJSON;

import java.text.ParseException;
import java.util.regex.Pattern;

public class JWTUtil {

	/**
	 * wrapper for Nimbus JWTParser
	 * just in case we want to override something one day
	 * @param jwtAsString
	 * @return
	 * @throws ParseException
	 */
	public static JWT parseJWT(String jwtAsString) throws ParseException {
		validateJwtContainsOnlyAllowedCharacters(jwtAsString);
		JWT jwt = JWTParser.parse(jwtAsString);
		return jwt;
	}

	public static void validateJwtContainsOnlyAllowedCharacters(String jwt) throws ParseException {
		// the allowed characters is base64url plus '.'
		String regex = "[.a-zA-Z0-9_-]";
		for (int i = 0; i < jwt.length(); i++) {
			char character = jwt.charAt(i);
			if (!Pattern.matches(regex, String.valueOf(character))) {
				throw new ParseException("The jwt is invalid because at index %s it contains the character %s that is neither a '.' nor one permitted in unpadded base64url".formatted(i, character), 0);
			}
		}

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
		JWTClaimsSet jwtClaimsSet;
		if (!(jwt instanceof JOSEObject)) {
			throw new RuntimeException("jwt is not an instance of JOSEObject");
		}

		// This code does multiple conversions to JSON; we could just call 'JsonParser.parseString' here, however
		// that seems to result in the unit test failing as we no longer detect JSON payloads that have the same
		// claim more than once.
		String jsonPayload = ((JOSEObject)jwt).getPayload().toString();
		if (jsonPayload == null) {
			throw new ParseException("Failed to get JWT payload as a string", 0);
		}

		jwtClaimsSet = JWTClaimsSet.parse(JSONObjectUtils.parse(jsonPayload));
		if (jwtClaimsSet == null) {
			throw new ParseException("Failed to extract JWT claims", 0);
		}
		JsonObject claims = JsonParser.parseString(JSONObjectUtils.toJSONString(jwtClaimsSet.toJSONObject(includeNullValues))).getAsJsonObject();
		return claims;
	}

	/**
	 * Also see jwtStringToJsonObjectForEnvironment
	 * Note: Nimbusds will always remove null values from JWT headers
	 * @param jwt
	 * @return
	 */
	public static JsonObject jwtHeaderAsJsonObject(JWT jwt) {
		JsonObject header = JsonParser.parseString(JSONObjectUtils.toJSONString(jwt.getHeader().toJSONObject())).getAsJsonObject();
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
		if(token instanceof EncryptedJWT encryptedJWT) {
			JsonObject jweHeader = JWTUtil.jwtHeaderAsJsonObject(token);
			JWEAlgorithm alg = encryptedJWT.getHeader().getAlgorithm();
			JWK decryptionKey = null;
			if(JWEAlgorithm.Family.SYMMETRIC.contains(alg)) {
				if (client == null) {
					throw new ParseException("A client secret is required to decrypt this JWT", 0);
				}
				String client_secret = OIDFJSON.getString(client.get("client_secret"));
				if (client_secret == null) {
					throw new ParseException("A client secret is required to decrypt this JWT", 0);
				}
				decryptionKey = JWEUtil.createSymmetricJWKForAlgAndSecret(client_secret,
										alg, encryptedJWT.getHeader().getEncryptionMethod(), null);
			} else {
				if (privateJwksWithEncKeys == null) {
					throw new ParseException("No suitable key for decrypting this JWT was provided in the test configuration. A private key of the correct key type with 'use': 'enc' and other matching properties is required.", 0);
				}
				JWKSet jwkSet = JWKUtil.parseJWKSet(privateJwksWithEncKeys.toString());
				decryptionKey = JWEUtil.selectAsymmetricKeyForEncryption(jwkSet, alg);
				if (decryptionKey == null) {
					throw new ParseException("No suitable key for decrypting this JWT was provided in the test configuration. A private key of the correct key type with 'use': 'enc' and other matching properties is required.", 0);
				}
			}
			JWEDecrypter decrypter = JWEUtil.createDecrypter(alg, decryptionKey);
			encryptedJWT.decrypt(decrypter);
			String decryptedJwt = encryptedJWT.getPayload().toString();
			try {
				validateJwtContainsOnlyAllowedCharacters(decryptedJwt);
			} catch (ParseException e) {
				throw new ParseException("The JWE has been decrypted. " + e.getMessage(), 0);
			}
			JsonObject out = JWTUtil.jwtStringToJsonObjectForEnvironment(decryptedJwt);
			out.add("jwe_header", jweHeader);
			return out;
		} else {
			JsonObject header = JWTUtil.jwtHeaderAsJsonObject(token);
			JsonObject claims = JWTUtil.jwtClaimsSetAsJsonObject(token);
			return createJsonObjectForEnvironment(jwtAsString, header, claims);
		}
	}

}

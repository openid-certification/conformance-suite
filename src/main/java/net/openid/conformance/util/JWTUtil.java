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

	// 2024-01-01T00:00:00Z in seconds — any valid iat should be at least this recent
	private static final long MIN_REASONABLE_TIMESTAMP = 1704067200L;
	private static final long SECONDS_PER_YEAR = 365L * 24 * 60 * 60;
	private static final long CLOCK_SKEW_SECONDS = 5 * 60; // 5 minutes

	/**
	 * Validates an 'iat' (Issued At) claim value per RFC 7519.
	 * Checks that the value is a number, not before 2024 (catches millisecond timestamps),
	 * and not in the future (with 5 minutes clock skew tolerance).
	 *
	 * @param iat the raw claim value from the JWT payload (may be null)
	 * @return the validated timestamp in seconds
	 * @throws IllegalArgumentException if the value is missing, not a number, or out of range
	 */
	public static long validateIatClaim(Object iat) {
		if (iat == null) {
			throw new IllegalArgumentException("Missing 'iat' claim");
		}
		if (!(iat instanceof Number)) {
			throw new IllegalArgumentException("'iat' claim is not a number");
		}
		long iatValue = ((Number) iat).longValue();
		if (iatValue < MIN_REASONABLE_TIMESTAMP) {
			throw new IllegalArgumentException("'iat' claim value " + iatValue
				+ " is too far in the past (this may indicate the value is not a unix timestamp in seconds)");
		}
		long nowSeconds = System.currentTimeMillis() / 1000L;
		if (iatValue > nowSeconds + CLOCK_SKEW_SECONDS) {
			throw new IllegalArgumentException("'iat' claim value " + iatValue + " is in the future (now: " + nowSeconds + ")");
		}
		return iatValue;
	}

	/**
	 * Validates an 'nbf' (Not Before) claim value per RFC 7519.
	 * Checks that the value is a number, not before 2024 (catches millisecond timestamps and
	 * epoch-default bugs), and not in the future (with 5 minutes clock skew tolerance).
	 *
	 * @param nbf the raw claim value from the JWT payload (may be null)
	 * @return the validated timestamp in seconds, or -1 if nbf is null (not present)
	 * @throws IllegalArgumentException if the value is not a number, out of range, or in the future
	 */
	public static long validateNbfClaim(Object nbf) {
		if (nbf == null) {
			return -1;
		}
		if (!(nbf instanceof Number)) {
			throw new IllegalArgumentException("'nbf' claim is not a number");
		}
		long nbfValue = ((Number) nbf).longValue();
		if (nbfValue < MIN_REASONABLE_TIMESTAMP) {
			throw new IllegalArgumentException("'nbf' claim value " + nbfValue
				+ " is too far in the past (this may indicate the value is not a unix timestamp in seconds)");
		}
		long nowSeconds = System.currentTimeMillis() / 1000L;
		if (nbfValue > nowSeconds + CLOCK_SKEW_SECONDS) {
			throw new IllegalArgumentException("'nbf' claim value " + nbfValue + " is in the future (now: " + nowSeconds + ")");
		}
		return nbfValue;
	}

	/**
	 * Validates an 'exp' (Expiration Time) claim value per RFC 7519.
	 * Checks that the value is a number, not more than 50 years in the future (catches millisecond timestamps),
	 * and not in the past (with 5 minutes clock skew tolerance).
	 *
	 * @param exp the raw claim value from the JWT payload (may be null)
	 * @return the validated timestamp in seconds, or -1 if exp is null (not present)
	 * @throws IllegalArgumentException if the value is not a number, out of range, or expired
	 */
	public static long validateExpClaim(Object exp) {
		if (exp == null) {
			return -1;
		}
		if (!(exp instanceof Number)) {
			throw new IllegalArgumentException("'exp' claim is not a number");
		}
		long expValue = ((Number) exp).longValue();
		long nowSeconds = System.currentTimeMillis() / 1000L;
		long maxTimestamp = nowSeconds + 50 * SECONDS_PER_YEAR;
		if (expValue > maxTimestamp) {
			throw new IllegalArgumentException("'exp' claim value " + expValue
				+ " is too far in the future (this may indicate the value is not a unix timestamp in seconds)");
		}
		if (expValue < nowSeconds - CLOCK_SKEW_SECONDS) {
			throw new IllegalArgumentException("'exp' claim value " + expValue + " indicates the JWT has expired (now: " + nowSeconds + ")");
		}
		return expValue;
	}

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
				decryptionKey = JWEUtil.selectAsymmetricKeyForEncryption(jwkSet, alg, encryptedJWT.getHeader().getKeyID());
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

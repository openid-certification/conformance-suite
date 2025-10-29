package net.openid.conformance.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.util.JSONObjectUtils;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.openqa.selenium.InvalidArgumentException;

import java.text.ParseException;
import java.util.List;

public class JWKUtil {
	public static JWKSet parseJWKSet(String jwksString) throws ParseException {
		JWKSet jwkSet = JWKSet.parse(jwksString);
		return jwkSet;
	}

	public static JsonObject getPublicJwksAsJsonObject(JWKSet jwks) {
		JsonObject publicJwks = JsonParser.parseString(JSONObjectUtils.toJSONString(jwks.toJSONObject(true))).getAsJsonObject();
		return publicJwks;
	}

	public static JsonObject getPrivateJwksAsJsonObject(JWKSet jwks) {
		JsonObject privateJwks = JsonParser.parseString(JSONObjectUtils.toJSONString(jwks.toJSONObject(false))).getAsJsonObject();
		return privateJwks;
	}

	public static String getAlgFromClientJwks(Environment env) {
		JsonObject jwks = env.getObject("client_jwks");
		JsonArray keys = jwks.get("keys").getAsJsonArray();
		JsonObject key = keys.get(0).getAsJsonObject();
		return OIDFJSON.getString(key.get("alg"));
	}

	public static JsonArray getAlgsFromJwks(JsonObject jwks) {
		JsonArray keys = jwks.get("keys").getAsJsonArray();
		JsonArray algs = new JsonArray();
		for (JsonElement key : keys) {
			algs.add(OIDFJSON.getString(key.getAsJsonObject().get("alg")));
		}
		return algs;
	}

	/**
	 * Will select the first key with the correct type, use and alg if possible
	 * or will select the key with correct type and use
	 * or will select the key with correct type
	 * Note: Server jwks will probably contain only 1 matching key (we create it), but just in case...
	 * @param jwsAlgorithm
	 * @param keys
	 * @return null if not found
	 */
	public static JWK selectAsymmetricJWSKey(JWSAlgorithm jwsAlgorithm, List<JWK> keys) {
		JWK bestMatch = null;
		JWK secondBestMatch = null;
		JWK thirdMatch = null;
		// an alternative to this code would be using nimbusds JWKMatcher
		for(JWK key : keys) {
			if(JWSAlgorithm.Family.EC.contains(jwsAlgorithm) && KeyType.EC.equals(key.getKeyType())) {
				ECKey ecKey = (ECKey)key;
				if(JWSAlgorithm.ES256.equals(jwsAlgorithm) && !Curve.P_256.equals(ecKey.getCurve())) {
					continue;
				}
				if(JWSAlgorithm.ES256K.equals(jwsAlgorithm) && !Curve.SECP256K1.equals(ecKey.getCurve())) {
					continue;
				}
				if(key.getKeyUse()!=null) {
					if(KeyUse.SIGNATURE.equals(key.getKeyUse())) {
						if(key.getAlgorithm()==null) {
							secondBestMatch = key;
						} else {
							if(key.getAlgorithm().equals(jwsAlgorithm)) {
								//this is the best match
								bestMatch = key;
								break;
							}
						}
					}
				} else {
					thirdMatch = key;
				}
			} else if(JWSAlgorithm.Family.ED.contains(jwsAlgorithm) && KeyType.OKP.equals(key.getKeyType())) {
				if(key.getKeyUse()!=null) {
					if(KeyUse.SIGNATURE.equals(key.getKeyUse())) {
						if(key.getAlgorithm()==null) {
							secondBestMatch = key;
						} else {
							if(key.getAlgorithm().equals(jwsAlgorithm)) {
								//this is the best match
								bestMatch = key;
								break;
							}
						}
					}
				} else {
					thirdMatch = key;
				}
			} else if(jwsAlgorithm.getName().startsWith("PS") && KeyType.RSA.equals(key.getKeyType())) {
				if(key.getKeyUse()!=null) {
					if(KeyUse.SIGNATURE.equals(key.getKeyUse())) {
						if(key.getAlgorithm()==null) {
							secondBestMatch = key;
						} else {
							if(key.getAlgorithm().equals(jwsAlgorithm)) {
								//this is the best match
								bestMatch = key;
								break;
							}
						}
					}
				} else {
					thirdMatch = key;
				}
			} else if(jwsAlgorithm.getName().startsWith("RS") && KeyType.RSA.equals(key.getKeyType())) {
				if(key.getKeyUse()!=null) {
					if(KeyUse.SIGNATURE.equals(key.getKeyUse())) {
						if(key.getAlgorithm()==null) {
							secondBestMatch = key;
						} else {
							if(key.getAlgorithm().equals(jwsAlgorithm)) {
								//this is the best match
								bestMatch = key;
								break;
							}
						}
					}
				} else {
					thirdMatch = key;
				}
			}
		}
		if(bestMatch!=null) {
			return bestMatch;
		} else if(secondBestMatch!=null) {
			return secondBestMatch;
		} else {
			return thirdMatch;
		}
	}

	public static JWK getSigningKey(JsonObject jwks) throws ParseException {
		int count = 0;
		JWK signingJwk = null;

		JWKSet jwkSet = JWKSet.parse(jwks.toString());
		for (JWK jwk : jwkSet.getKeys()) {
			var use = jwk.getKeyUse();
			if (use != null && !use.equals(KeyUse.SIGNATURE)) {
				continue;
			}
			count++;
			signingJwk = jwk;
		}

		if (count == 0) {
			throw new InvalidArgumentException("Did not find a key with 'use': 'sig' or no 'use' claim, no key available to sign jwt");
		}
		if (count > 1) {
			throw new InvalidArgumentException("Expected only one signing JWK in the set. Please ensure the signing key is the only one in the jwks, or that other keys have a 'use' other than 'sig'.");
		}

		return signingJwk;
	}

	/**
	 * Creates a {@link JsonObject} with a keys array containing the JWK {@link JsonObject}.
	 * @param keys
	 * @return
	 */
	public static JsonObject createJwksObjectFromJwkObjects(JsonObject ... keys) {

		if (keys == null) {
			throw new InvalidArgumentException("keys must not be null");
		}

		JsonObject jwks = new JsonObject();
		JsonArray jwksKeys = new JsonArray();
		for (JsonObject key : keys) {
			jwksKeys.add(key);
		}

		jwks.add("keys", jwksKeys);
		return jwks;
	}

	public static JsonObject toPublicJWKSet(JsonObject input) {
		try {
			String json = input.toString();
			JWKSet fullSet = JWKSet.parse(json);
			JWKSet publicSet = fullSet.toPublicJWKSet();
			return JsonParser.parseString(publicSet.toString(true)).getAsJsonObject();
		} catch (Exception e) {
			throw new RuntimeException("Failed to convert JWKS to public version", e);
		}
	}}

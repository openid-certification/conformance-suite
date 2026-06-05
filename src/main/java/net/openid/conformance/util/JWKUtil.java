package net.openid.conformance.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.JSONObjectUtils;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.openqa.selenium.InvalidArgumentException;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class JWKUtil {

	/** A JWK that {@link #parseJWKSetLeniently(String, List)} skipped, with the reason the JOSE library rejected it. */
	public record SkippedJwk(JsonElement keyJson, String reason) { }

	public static JWKSet parseJWKSet(String jwksString) throws ParseException {
		JWKSet jwkSet = JWKSet.parse(jwksString);
		return jwkSet;
	}

	/**
	 * Parse a JWK set, skipping any individual keys the JOSE library cannot handle (e.g. keys
	 * using curves it does not support, such as Brainpool). This mirrors how a real recipient
	 * would behave when offered a set of keys: ignore the ones it cannot use and keep the rest.
	 *
	 * Use this only when selecting a usable key from a counterparty's JWK set (e.g. picking an
	 * encryption key), NOT when validating a JWK set where every key is expected to be valid.
	 *
	 * Each skipped key - together with the reason the JOSE library could not parse it - is recorded
	 * in {@code skippedKeys}, so the calling condition can log it into the test log.
	 *
	 * @param jwksString the JWK set as a JSON string
	 * @param skippedKeys populated with one entry per key that could not be parsed
	 * @return a JWK set containing only the keys that parsed successfully (possibly empty)
	 * @throws ParseException if the JSON is not a valid JWK set object (missing "keys" array)
	 */
	public static JWKSet parseJWKSetLeniently(String jwksString, List<SkippedJwk> skippedKeys) throws ParseException {
		JsonObject jwks = JsonParser.parseString(jwksString).getAsJsonObject();
		JsonElement keysElement = jwks.get("keys");
		if (keysElement == null || !keysElement.isJsonArray()) {
			throw new ParseException("JWK set does not contain a \"keys\" array", 0);
		}
		JsonArray keys = keysElement.getAsJsonArray();
		List<JWK> parsed = new ArrayList<>();
		for (JsonElement keyEl : keys) {
			try {
				parsed.add(JWK.parse(keyEl.toString()));
			} catch (ParseException e) {
				// skip keys the JOSE library cannot handle (e.g. unsupported curves or key types)
				skippedKeys.add(new SkippedJwk(keyEl, e.getMessage()));
			}
		}
		return new JWKSet(parsed);
	}

	/** A problem found with a single key in a JWK set, identified by its position (index) within the set. */
	public record JwkIssue(int index, JsonElement key, String detail) { }

	private static final List<String> PRIVATE_KEY_MEMBERS =
		List.of("d", "p", "q", "dp", "dq", "qi", "oth", "k");

	private static final Set<String> SUPPORTED_KEY_TYPES = Set.of(
		KeyType.EC.getValue(), KeyType.RSA.getValue(), KeyType.OCT.getValue(), KeyType.OKP.getValue());

	private static final Set<String> KNOWN_ALGORITHMS = buildKnownAlgorithms();

	private static final Pattern BASE64URL = Pattern.compile("^[A-Za-z0-9_-]*$");

	private static Set<String> buildKnownAlgorithms() {
		Set<String> names = new HashSet<>();
		for (JWSAlgorithm alg : JWSAlgorithm.Family.SIGNATURE) {
			names.add(alg.getName());
		}
		for (JWEAlgorithm alg : JWEAlgorithm.Family.ASYMMETRIC) {
			names.add(alg.getName());
		}
		for (JWEAlgorithm alg : JWEAlgorithm.Family.SYMMETRIC) {
			names.add(alg.getName());
		}
		return names;
	}

	private static JsonArray keysArrayOrEmpty(JsonObject jwks) {
		if (jwks != null) {
			JsonElement keys = jwks.get("keys");
			if (keys != null && keys.isJsonArray()) {
				return keys.getAsJsonArray();
			}
		}
		return new JsonArray();
	}

	private static String stringMember(JsonObject key, String member) {
		JsonElement el = key.get(member);
		return (el != null && el.isJsonPrimitive()) ? OIDFJSON.getString(el) : null;
	}

	/**
	 * Scan a JWK set for private or symmetric key material by inspecting the raw JSON members,
	 * deliberately NOT relying on the JOSE library to parse each key. {@code JWKSet.parse} /
	 * {@code JWK.parse} silently drop a key with an unknown {@code kty} (RFC 7517 section 5) and
	 * collapse every other failure into a single ParseException, so private material in a key the
	 * library cannot parse (e.g. on an unsupported curve) would otherwise go undetected.
	 *
	 * @return one issue per key that carries private (d/p/q/dp/dq/qi/oth/k) or symmetric (oct) material
	 */
	public static List<JwkIssue> findPrivateOrSymmetricKeyMembers(JsonObject jwks) {
		List<JwkIssue> issues = new ArrayList<>();
		JsonArray keys = keysArrayOrEmpty(jwks);
		for (int i = 0; i < keys.size(); i++) {
			JsonElement keyEl = keys.get(i);
			if (!keyEl.isJsonObject()) {
				continue; // reported by findStructurallyInvalidKeys
			}
			JsonObject key = keyEl.getAsJsonObject();
			if (KeyType.OCT.getValue().equals(stringMember(key, "kty"))) {
				issues.add(new JwkIssue(i, keyEl, "is a symmetric (oct) key"));
				continue;
			}
			for (String member : PRIVATE_KEY_MEMBERS) {
				if (key.has(member)) {
					issues.add(new JwkIssue(i, keyEl, "contains private key material (member '" + member + "')"));
					break;
				}
			}
		}
		return issues;
	}

	/**
	 * Check that each key has the members required for its (recognised) key type and that the
	 * encoded coordinate values are unpadded base64url. Keys whose {@code kty} the JOSE library does
	 * not recognise are left to {@link #findUnusableKeys(JsonObject)} (a warning), not reported here.
	 *
	 * @return one issue per structurally invalid key
	 */
	public static List<JwkIssue> findStructurallyInvalidKeys(JsonObject jwks) {
		List<JwkIssue> issues = new ArrayList<>();
		JsonArray keys = keysArrayOrEmpty(jwks);
		for (int i = 0; i < keys.size(); i++) {
			JsonElement keyEl = keys.get(i);
			if (!keyEl.isJsonObject()) {
				issues.add(new JwkIssue(i, keyEl, "is not a JSON object"));
				continue;
			}
			JsonObject key = keyEl.getAsJsonObject();
			String kty = stringMember(key, "kty");
			if (kty == null) {
				issues.add(new JwkIssue(i, keyEl, "is missing the required 'kty' member"));
				continue;
			}
			String[] required;
			String[] base64urlMembers;
			if (KeyType.RSA.getValue().equals(kty)) {
				required = new String[] { "e", "n" };
				base64urlMembers = new String[] { "e", "n" };
			} else if (KeyType.EC.getValue().equals(kty)) {
				required = new String[] { "x", "y" };
				base64urlMembers = new String[] { "x", "y" };
			} else if (KeyType.OKP.getValue().equals(kty)) {
				required = new String[] { "x", "crv" };
				base64urlMembers = new String[] { "x" };
			} else {
				continue; // oct handled by the private/symmetric check; unknown kty by the warning check
			}
			String missing = firstMissingMember(key, required);
			if (missing != null) {
				issues.add(new JwkIssue(i, keyEl, "is missing the required '" + missing + "' member for a " + kty + " key"));
				continue;
			}
			String badMember = firstNonBase64UrlMember(key, base64urlMembers);
			if (badMember != null) {
				issues.add(new JwkIssue(i, keyEl, "has a '" + badMember + "' value that is not valid unpadded base64url"));
			}
		}
		return issues;
	}

	/**
	 * Identify keys the JOSE library cannot use: an unrecognised key type, an unsupported curve, or
	 * an unrecognised {@code alg}. These are surfaced as warnings (a real recipient ignores keys it
	 * cannot use, RFC 7517 section 5) rather than failures.
	 *
	 * @return one issue per unusable key
	 */
	public static List<JwkIssue> findUnusableKeys(JsonObject jwks) {
		List<JwkIssue> issues = new ArrayList<>();
		JsonArray keys = keysArrayOrEmpty(jwks);
		for (int i = 0; i < keys.size(); i++) {
			JsonElement keyEl = keys.get(i);
			if (!keyEl.isJsonObject()) {
				continue;
			}
			JsonObject key = keyEl.getAsJsonObject();
			String kty = stringMember(key, "kty");
			if (kty == null) {
				continue; // missing kty is a structural failure, reported elsewhere
			}
			if (!SUPPORTED_KEY_TYPES.contains(kty)) {
				issues.add(new JwkIssue(i, keyEl, "uses an unsupported key type (kty '" + kty + "')"));
			} else if (KeyType.EC.getValue().equals(kty)) {
				String unsupported = unsupportedCurve(key, ECKey.SUPPORTED_CURVES);
				if (unsupported != null) {
					issues.add(new JwkIssue(i, keyEl, "uses an unsupported EC curve ('" + unsupported + "')"));
				}
			} else if (KeyType.OKP.getValue().equals(kty)) {
				String unsupported = unsupportedCurve(key, OctetKeyPair.SUPPORTED_CURVES);
				if (unsupported != null) {
					issues.add(new JwkIssue(i, keyEl, "uses an unsupported OKP curve ('" + unsupported + "')"));
				}
			}
			String alg = stringMember(key, "alg");
			if (alg != null && !alg.isEmpty() && !KNOWN_ALGORITHMS.contains(alg)) {
				issues.add(new JwkIssue(i, keyEl, "uses an unrecognised algorithm (alg '" + alg + "')"));
			}
		}
		return issues;
	}

	private static String unsupportedCurve(JsonObject key, Set<Curve> supported) {
		String crv = stringMember(key, "crv");
		if (crv == null || crv.isEmpty()) {
			return null; // missing/empty crv is a structural concern, not an "unusable" warning
		}
		return supported.contains(Curve.parse(crv)) ? null : crv;
	}

	private static String firstMissingMember(JsonObject key, String... members) {
		for (String member : members) {
			if (!key.has(member)) {
				return member;
			}
		}
		return null;
	}

	private static String firstNonBase64UrlMember(JsonObject key, String... members) {
		for (String member : members) {
			String value = stringMember(key, member);
			if (value == null || !BASE64URL.matcher(value).matches()) {
				return member;
			}
		}
		return null;
	}

	/** Render a list of {@link JwkIssue}s as a JSON array suitable for logging in condition args. */
	public static JsonArray issuesToJson(List<JwkIssue> issues) {
		JsonArray arr = new JsonArray();
		for (JwkIssue issue : issues) {
			JsonObject o = new JsonObject();
			o.addProperty("index", issue.index());
			o.addProperty("detail", issue.detail());
			if (issue.key() != null) {
				o.add("key", issue.key());
			}
			arr.add(o);
		}
		return arr;
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

package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;

/**
 * Verifies a SET's signature against the transmitter's JWKS in {@code server_jwks}. When the
 * SET names a {@code kid} the fetched JWKS does not contain, the JWKS is fetched once more from
 * the transmitter's {@code jwks_uri} before verifying: SSF 1.0 4.1.4 resolves signing keys
 * through the {@code jwks_uri}, and a transmitter that rotated its key during the run publishes
 * the new key there. A key that is still missing after the refresh fails the signature check.
 */
public class OIDSSFVerifySignatureOfSecurityEventToken extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = {"ssf", "server_jwks"})
	public Environment evaluate(Environment env) {

		String tokenString = env.getString("ssf", "verification.jwt");

		refreshJwksIfKidUnknown(env, tokenString);

		JsonObject serverJwks = env.getObject("server_jwks"); // to validate the signature
		verifyJwsSignature(tokenString, serverJwks, "Security Event Token", false, "server");

		return env;
	}

	protected void refreshJwksIfKidUnknown(Environment env, String tokenString) {
		String kid;
		try {
			kid = JWSObject.parse(tokenString).getHeader().getKeyID();
		} catch (ParseException e) {
			// the signature check reports the unparseable token
			return;
		}
		if (kid == null || jwksContainsKid(env.getObject("server_jwks"), kid)) {
			return;
		}
		String jwksUri = env.getString("ssf", "transmitter_metadata.jwks_uri");
		if (jwksUri == null) {
			log("The SET names a kid the fetched JWKS does not contain and the transmitter metadata has no jwks_uri to refresh from",
				args("kid", kid));
			return;
		}
		log("The SET names a kid the fetched JWKS does not contain; fetching the JWKS again from the transmitter's jwks_uri",
			args("kid", kid, "jwks_uri", jwksUri));
		try {
			RestTemplate restTemplate = createRestTemplate(env);
			String jwksString = restTemplate.getForObject(jwksUri, String.class);
			JsonElement jwksEl = jwksString == null ? null : JsonParser.parseString(jwksString);
			if (jwksEl == null || !jwksEl.isJsonObject()) {
				log("The refreshed JWKS is not a JSON object; the previously fetched JWKS is used", args("jwks_uri", jwksUri, "jwks", jwksString));
				return;
			}
			env.putObject("server_jwks", jwksEl.getAsJsonObject());
			log("Refreshed the transmitter JWKS", args("jwks_uri", jwksUri, "jwks", jwksEl, "kid_now_present", jwksContainsKid(jwksEl.getAsJsonObject(), kid)));
		} catch (Exception e) {
			log("Could not refresh the transmitter JWKS; the previously fetched JWKS is used", args("jwks_uri", jwksUri, "error", e.getMessage()));
		}
	}

	protected static boolean jwksContainsKid(JsonObject jwks, String kid) {
		if (jwks == null) {
			return false;
		}
		JsonElement keysEl = jwks.get("keys");
		if (keysEl == null || !keysEl.isJsonArray()) {
			return false;
		}
		for (JsonElement keyEl : keysEl.getAsJsonArray()) {
			if (keyEl.isJsonObject() && kid.equals(OIDFJSON.tryGetString(keyEl.getAsJsonObject().get("kid")))) {
				return true;
			}
		}
		return false;
	}
}

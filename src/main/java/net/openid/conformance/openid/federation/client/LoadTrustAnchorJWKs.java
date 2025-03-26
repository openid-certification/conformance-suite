package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWKSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;

public class LoadTrustAnchorJWKs extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	//may also add server_encryption_keys if encryption keys are found, optional
	@PostEnvironment(required = { "trust_anchor_public_jwks", "trust_anchor_jwks" })
	public Environment evaluate(Environment env) {

		JsonElement configured = env.getElementFromObject("config", "federation_trust_anchor.trust_anchor_jwks");

		if (configured == null) {
			throw error("Couldn't find a JWK set in configuration");
		}

		// parse the JWKS to make sure it's valid
		try {
			JWKSet jwks = JWKUtil.parseJWKSet(configured.toString());

			JsonObject publicJwks = JWKUtil.getPublicJwksAsJsonObject(jwks);
			JsonObject privateJwks = JWKUtil.getPrivateJwksAsJsonObject(jwks);

			env.putObject("trust_anchor_public_jwks", publicJwks);
			env.putObject("trust_anchor_jwks", privateJwks);
			JsonArray keys = privateJwks.get("keys").getAsJsonArray();
			JsonObject encKeysJwks = new JsonObject();
			JsonArray foundEncKeys = new JsonArray();
			for(JsonElement keyElement : keys) {
				JsonObject keyObject = keyElement.getAsJsonObject();
				if(!keyObject.has("use")) {
					foundEncKeys.add(keyObject);
				} else {
					String keyUse = OIDFJSON.getString(keyObject.get("use"));
					if("enc".equals(keyUse)) {
						foundEncKeys.add(keyObject);
					}
				}
			}
			if(!foundEncKeys.isEmpty()) {
				encKeysJwks.add("keys", foundEncKeys);
				env.putObject("trust_anchor_encryption_keys", encKeysJwks);
			}

			logSuccess("Parsed public and private JWK sets",
				args("trust_anchor_public_jwks", publicJwks, "trust_anchor_jwks", privateJwks, "trust_anchor_encryption_keys", encKeysJwks));

			return env;

		} catch (ParseException e) {
			throw error("Failure parsing JWK Set", e, args("jwk_string", configured));
		}

	}

}

package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddIsoMdocClientMetadataToAuthorizationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request", "client_public_jwks"})
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		var clientMetaData = (JsonObject) JsonParser.parseString("""
{
	"authorization_encrypted_response_alg": "ECDH-ES",
	"authorization_encrypted_response_enc": "A256GCM",
	"require_signed_request_object": true,
	"vp_formats": {
	  "mso_mdoc": {
		"alg": [
		  "ES256"
		]
	  }
	}
}
""");

		JsonObject publicJwks = env.getObject("client_public_jwks");
		JsonArray keys = publicJwks.getAsJsonArray("keys");
		JsonObject encKey = null;
		for (JsonElement jwkEl: keys) {
			JsonObject jwk = jwkEl.getAsJsonObject();
			if (!jwk.has("use")) {
				continue;
			}
			String use = OIDFJSON.getString(jwk.get("use"));
			if (use.equals("enc")) {
				if (encKey != null) {
					throw error("client jwks contains more than one key with 'use': 'enc'", args("clientjwks", publicJwks));
				}
				encKey = jwk;
			}
		}
		if (encKey == null) {
			throw error("The client jwks does not contain a key with 'use': 'enc'", args("clientjwks", publicJwks));
		}
		JsonArray keysArray = new JsonArray();
		keysArray.add(encKey);
		JsonObject jwks = new JsonObject();
		jwks.add("keys", keysArray);

		clientMetaData.add("jwks", jwks);

		authorizationEndpointRequest.add("client_metadata", clientMetaData);

		log("Added client_metadata to authorization endpoint request", args("client_metdata", clientMetaData));

		return env;
	}
}

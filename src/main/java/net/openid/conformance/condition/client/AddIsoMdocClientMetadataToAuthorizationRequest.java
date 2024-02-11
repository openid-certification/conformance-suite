package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddIsoMdocClientMetadataToAuthorizationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request"})
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

		var encPrivateJwks = JsonParser.parseString("""
{
    "keys": [
        {
            "kty": "EC",
            "d": "7N8jd8HvUp3vHC7a-xitehRnYuyZLy3kqkxG7KmpfMY",
            "use": "enc",
            "crv": "P-256",
            "kid": "A541J5yUqazgE8WBFkIyeh2OtK-udqUR_OC0kB7l3oU",
            "x": "cwYyuS94hcOtcPlrMMtGtflCfbZUwz5Mf1Gfa2m0AM8",
            "y": "KB7sJkFQyB8jZHO9vmWS5LNECL4id3OJO9HX9ChNonA",
            "alg": "ECDH-ES"
        }
    ]
}
""");
		// FIXME: get this from the client jwks?
		var encPubJwks = JsonParser.parseString("""
{
    "keys": [
        {
            "kty": "EC",
            "use": "enc",
            "crv": "P-256",
            "x": "cwYyuS94hcOtcPlrMMtGtflCfbZUwz5Mf1Gfa2m0AM8",
            "y": "KB7sJkFQyB8jZHO9vmWS5LNECL4id3OJO9HX9ChNonA"
        }
    ]
}
""");
		clientMetaData.add("jwks", encPubJwks);

		authorizationEndpointRequest.add("client_metadata", clientMetaData);

		log("Added client_metadata to authorization endpoint request", args("client_metdata", clientMetaData));

		return env;
	}
}

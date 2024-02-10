package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddAudToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		String serverIssuerUrl = env.getString("server", "issuer");
serverIssuerUrl="https://self-issued.me/v2"; // as per https://openid.net/specs/openid-4-verifiable-presentations-1_0-ID2.html#name-aud-of-a-request-object

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

		if (serverIssuerUrl != null) {
			requestObjectClaims.addProperty("aud", serverIssuerUrl);
			requestObjectClaims.add("client_metadata", clientMetaData);

			env.putObject("request_object_claims", requestObjectClaims);

			logSuccess("Added aud to request object claims", args("aud", serverIssuerUrl));
		} else {
			// Only a "should" requirement
			log("Request object contains no audience and server issuer URL not found");
		}

		return env;
	}
}

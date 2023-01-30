package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
// Due to historical reasons there is potential ambiguity regarding the
//   appropriate audience value to use when employing JWT client assertion
//   based authentication (defined in Section 2.2 of [RFC7523] with
//   "private_key_jwt" or "client_secret_jwt" authentication method names
//   per Section 9 of [OIDC]).  To address that ambiguity the issuer
//   identifier URL of the authorization server according to [RFC8414]
//   SHOULD be used as the value of the audience.  In order to facilitate
//   interoperability the authorization server MUST accept its issuer
//   identifier, token endpoint URL, or pushed authorization request
//   endpoint URL as values that identify it as an intended audience.

public class ValidateClientAssertionClaimsForPAREndpoint extends ValidateClientAssertionClaims {

	@Override
	protected void validateAud(Environment env) {
		String tokenEndpoint = env.getString("server", "token_endpoint");
		String issuer = env.getString("server", "issuer");
		String parEndpoint = env.getString("server", "pushed_authorization_request_endpoint");
		String parMTLSEndpoint = env.getString("server", "mtls_endpoint_aliases.pushed_authorization_request_endpoint");
		JsonElement aud = env.getElementFromObject("client_assertion", "claims.aud");
		if (aud == null) {
			throw error("Missing aud");
		}
		JsonArray expectedValues = new JsonArray();
		expectedValues.add(issuer);
		expectedValues.add(tokenEndpoint);
		expectedValues.add(parEndpoint);
		if(parMTLSEndpoint != null && !parMTLSEndpoint.equals("")) {
			expectedValues.add(parMTLSEndpoint);
		}

		if (aud.isJsonArray()) {
			if (!(
					aud.getAsJsonArray().contains(new JsonPrimitive(tokenEndpoint)) ||
					aud.getAsJsonArray().contains(new JsonPrimitive(issuer)) ||
					aud.getAsJsonArray().contains(new JsonPrimitive(parEndpoint)) ||
					aud.getAsJsonArray().contains(new JsonPrimitive(parMTLSEndpoint))
				)) {
				throw error("aud values do not contain any of the expected values", args("expected", expectedValues, "actual", aud));
			}
		} else {
			if (!(
					tokenEndpoint.equals(OIDFJSON.getString(aud)) ||
					issuer.equals(OIDFJSON.getString(aud)) ||
					parEndpoint.equals(OIDFJSON.getString(aud)) ||
					parMTLSEndpoint.equals(OIDFJSON.getString(aud))
				)) {
				throw error("aud claim is not one of the expected values", args("expected", expectedValues, "actual", aud));
			}
		}
	}
}

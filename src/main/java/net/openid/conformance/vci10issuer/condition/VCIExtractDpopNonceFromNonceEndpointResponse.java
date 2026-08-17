package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.http.DpopNonceResponseHeader;

/**
 * Picks up a {@code DPoP-Nonce} header supplied on the Nonce Endpoint response.
 *
 * <p>OID4VCI 1.0 section 7.2: "The Credential Issuer MAY provide a DPoP nonce in an HTTP header as
 * defined in Section 8.2 of [RFC9449]. In this case, the Wallet uses the new nonce value in the DPoP
 * proof when presenting an access token at the Credential Endpoint." The nonce is therefore stored as
 * the resource server nonce so that the next DPoP proof for the credential endpoint carries it,
 * avoiding a {@code use_dpop_nonce} challenge/retry round-trip.
 *
 * <p>Expects the nonce endpoint response to be mapped to {@code endpoint_response}.
 */
public class VCIExtractDpopNonceFromNonceEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {
		JsonElement headersEl = env.getElementFromObject("endpoint_response", "headers");
		JsonObject responseHeaders = headersEl != null && headersEl.isJsonObject() ? headersEl.getAsJsonObject() : null;

		DpopNonceResponseHeader nonceHeader = DpopNonceResponseHeader.from(responseHeaders);
		if (nonceHeader.violation() != null) {
			throw error(nonceHeader.violation(), args("headers", responseHeaders));
		}

		if (nonceHeader.nonce() == null) {
			logSuccess("Nonce endpoint response does not contain a DPoP-Nonce header; the credential issuer"
				+ " may still request a nonce at the credential endpoint via a use_dpop_nonce error");
			return env;
		}

		env.putString("resource_server_dpop_nonce", nonceHeader.nonce());
		logSuccess("Nonce endpoint response contains a DPoP-Nonce header; the nonce will be used in the DPoP proof"
			+ " for the credential endpoint request", args("dpop_nonce", nonceHeader.nonce()));
		return env;
	}
}

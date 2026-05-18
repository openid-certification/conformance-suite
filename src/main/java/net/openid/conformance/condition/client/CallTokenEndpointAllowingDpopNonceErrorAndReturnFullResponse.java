package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.http.DpopNonceResponseHeader;
import org.springframework.http.ResponseEntity;

import java.util.List;


/**
 * Wrapper around {@link CallTokenEndpointAndReturnFullResponse} that recognizes a {@code use_dpop_nonce} 400
 * response and exposes the supplied DPoP-Nonce so the caller can retry, and likewise recognizes an
 * {@code use_attestation_challenge} 400 response (draft-ietf-oauth-attestation-based-client-auth-07 §6.2)
 * so the caller can retry with a freshly harvested {@code OAuth-Client-Attestation-Challenge}.
 */
public class CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse extends CallTokenEndpointAndReturnFullResponse {

	/** The nonce the server supplied on this response, or null if it supplied none. */
	private String suppliedDpopNonce;

	// WARNING optional token_endpoint_dpop_nonce_error / token_endpoint_use_attestation_challenge_error
	// returned with the respective nonce / challenge value.
	@Override
	@PreEnvironment(required = { "server", "token_endpoint_request_form_parameters" })
	@PostEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		env.removeNativeValue("token_endpoint_dpop_nonce_error");
		env.removeNativeValue("token_endpoint_use_attestation_challenge_error");
		return super.evaluate(env);
	}

	@Override
	protected void addFullResponse(Environment env, ResponseEntity<String> response) {
		super.addFullResponse(env, response);
		JsonElement jsonError = env.getElementFromObject("token_endpoint_response_full", "body_json.error");
		JsonObject jsonResponseHeaders = env.getObject("token_endpoint_response_headers");
		int status = env.getInteger("token_endpoint_response_http_status");

		// A DPoP-Nonce that breaks RFC9449 is reported whatever the status code was, so that the violation is
		// attributed to the response that carried it rather than to whatever we do with the value later on.
		DpopNonceResponseHeader nonceHeader = DpopNonceResponseHeader.from(jsonResponseHeaders);
		if (nonceHeader.violation() != null) {
			throw error(nonceHeader.violation(), args("headers", jsonResponseHeaders));
		}
		String dpopNonce = nonceHeader.nonce();
		suppliedDpopNonce = dpopNonce;

		String errorCode = (status == 400 && jsonError != null) ? OIDFJSON.getString(jsonError) : null;

		if ("use_dpop_nonce".equals(errorCode)) {
			if (dpopNonce == null) {
				throw error("The token endpoint returned a 'use_dpop_nonce' error but supplied no DPoP-Nonce"
					+ " header, leaving no nonce to retry the request with.",
					args("headers", jsonResponseHeaders));
			}
			env.putString("authorization_server_dpop_nonce", dpopNonce);
			env.putString("token_endpoint_dpop_nonce_error", dpopNonce);
			env.putObject("token_endpoint_response", env.getElementFromObject("token_endpoint_response_full", "body_json").getAsJsonObject());
		} else if ("use_attestation_challenge".equals(errorCode)) {
			// Per draft-ietf-oauth-attestation-based-client-auth-07 §6.2, the use_attestation_challenge
			// error MUST be accompanied by the OAuth-Client-Attestation-Challenge response header so the
			// client has a fresh challenge to retry with. If it isn't (missing, repeated, or empty), the
			// test cannot proceed.
			List<String> challengeList = response.getHeaders().get("OAuth-Client-Attestation-Challenge");
			if (challengeList == null || challengeList.size() != 1 || Strings.isNullOrEmpty(challengeList.get(0))) {
				throw error("use_attestation_challenge error response did not include exactly one non-empty OAuth-Client-Attestation-Challenge header",
					args("headers", jsonResponseHeaders));
			}
			// Flag the retry-able error. The caller's retry loop is responsible for harvesting the
			// freshly returned OAuth-Client-Attestation-Challenge header (via
			// ExtractClientAttestationChallengeFromResponseHeader) and regenerating the client_attestation PoP.
			// token_endpoint_response is already populated from body_json by the superclass.
			env.putString("token_endpoint_use_attestation_challenge_error", errorCode);
			log("Got use_attestation_challenge error response — caller may retry with the freshly returned challenge",
				args("error", errorCode));
		} else if (status >= 200 && status < 300 && dpopNonce != null) {
			// RFC 9449 §8.2: the server may rotate the DPoP nonce on every response and the
			// client MUST use the newly supplied value on subsequent requests. Some ASes treat
			// each nonce as single-use (reusing one returns invalid_dpop_proof with no recovery
			// path), so harvesting the freshly issued nonce from a successful response is
			// required to avoid stale-nonce reuse on the next call.
			env.putString("authorization_server_dpop_nonce", dpopNonce);
		}
	}

	@Override
	protected String parsedResponseLogSuffix() {
		return " - " + DpopNonceResponseHeader.describeSuppliedNonce(suppliedDpopNonce);
	}
}

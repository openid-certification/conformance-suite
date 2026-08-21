package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.http.DpopNonceResponseHeader;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * This class makes a http post to PAR endpoint and examines the response for DPoP nonce errors and stores the
 * required nonce for retry.
 *
 * <p>{@code use_attestation_challenge} (draft-ietf-oauth-attestation-based-client-auth-07 §6.2) is an
 * attestation-specific error, so this class deliberately does NOT treat it as retryable — for
 * private_key_jwt/mtls (+DPoP) flows an AS returning it is misbehaving and the 400 must surface as a test
 * failure rather than be masked by a retry. Use
 * {@link CallPAREndpointAllowingDpopNonceOrUseAttestationChallengeError} on client_attestation flows,
 * where the error is a legitimate challenge-(re)issuance path.
 */
public class CallPAREndpointAllowingDpopNonceError extends CallPAREndpoint {

	/** The nonce the server supplied on this response, or null if it supplied none. */
	private String suppliedDpopNonce;

	/**
	 * Whether a 400 {@code use_attestation_challenge} response is flagged as retryable via
	 * {@code par_endpoint_use_attestation_challenge_error}. Only the client_attestation subclass returns
	 * true; for every other client auth type the error is invalid and must fail the test downstream.
	 */
	protected boolean recognizeUseAttestationChallengeError() {
		return false;
	}

	@Override
	public Environment evaluate(Environment env) {
		env.removeNativeValue("par_endpoint_dpop_nonce_error");
		env.removeNativeValue("par_endpoint_use_attestation_challenge_error");
		return super.evaluate(env);
	}

	@Override
	protected void addFullResponse(Environment env, ResponseEntity<String> response) {
		super.addFullResponse(env, response);
		JsonElement jsonError = env.getElementFromObject(RESPONSE_KEY, "body_json.error");
		JsonObject jsonResponseHeaders = env.getElementFromObject(RESPONSE_KEY, "headers").getAsJsonObject();
		int status = OIDFJSON.getInt(env.getElementFromObject(RESPONSE_KEY, "status"));

		// checked whatever the status code was, so the violation is attributed to the response that carried it
		DpopNonceResponseHeader nonceHeader = DpopNonceResponseHeader.from(jsonResponseHeaders);
		if (nonceHeader.violation() != null) {
			throw error(nonceHeader.violation(), args("headers", jsonResponseHeaders));
		}
		String dpopNonce = nonceHeader.nonce();
		suppliedDpopNonce = dpopNonce;

		String errorCode = (status == 400 && jsonError != null) ? OIDFJSON.getString(jsonError) : null;

		if ("use_dpop_nonce".equals(errorCode)) {
			if (dpopNonce == null) {
				throw error("The PAR endpoint returned a 'use_dpop_nonce' error but supplied no DPoP-Nonce"
					+ " header, leaving no nonce to retry the request with.",
					args("headers", jsonResponseHeaders));
			}
			env.putString("authorization_server_dpop_nonce", dpopNonce);
			env.putString("par_endpoint_dpop_nonce_error", dpopNonce);
			env.putObject("par_endpoint_response", env.getElementFromObject(RESPONSE_KEY, "body_json").getAsJsonObject());
		} else if ("use_attestation_challenge".equals(errorCode) && recognizeUseAttestationChallengeError()) {
			// Per draft-ietf-oauth-attestation-based-client-auth-07 §6.2, the use_attestation_challenge
			// error MUST be accompanied by exactly one non-empty OAuth-Client-Attestation-Challenge
			// response header.
			List<String> challengeList = response.getHeaders().get("OAuth-Client-Attestation-Challenge");
			if (challengeList == null || challengeList.size() != 1 || Strings.isNullOrEmpty(challengeList.get(0))) {
				throw error("use_attestation_challenge error response did not include exactly one non-empty OAuth-Client-Attestation-Challenge header",
					args("headers", jsonResponseHeaders));
			}
			env.putString("par_endpoint_use_attestation_challenge_error", errorCode);
			env.putObject("par_endpoint_response", env.getElementFromObject(RESPONSE_KEY, "body_json").getAsJsonObject());
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

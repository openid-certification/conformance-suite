package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Wrapper around {@link CallTokenEndpointAndReturnFullResponse} that recognises a
 * {@code use_attestation_challenge} 400 response (draft-ietf-oauth-attestation-based-client-auth-07 §6.2)
 * and exposes {@code token_endpoint_use_attestation_challenge_error} so the caller can retry. Pair with
 * {@link ExtractClientAttestationChallengeFromResponseHeader} so the next attempt picks up the freshly
 * returned {@code OAuth-Client-Attestation-Challenge}.
 *
 * <p>Use this on non-DPoP code paths; the DPoP-nonce wrapper
 * {@link CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse} already detects both errors.
 */
public class CallTokenEndpointAllowingUseAttestationChallengeErrorAndReturnFullResponse extends CallTokenEndpointAndReturnFullResponse {

	@Override
	@PreEnvironment(required = { "server", "token_endpoint_request_form_parameters" })
	@PostEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		env.removeNativeValue("token_endpoint_use_attestation_challenge_error");
		return super.evaluate(env);
	}

	@Override
	protected void addFullResponse(Environment env, ResponseEntity<String> response) {
		super.addFullResponse(env, response);
		JsonElement jsonError = env.getElementFromObject("token_endpoint_response_full", "body_json.error");
		int status = env.getInteger("token_endpoint_response_http_status");
		if (status != 400 || jsonError == null) {
			return;
		}
		String errorCode = OIDFJSON.getString(jsonError);
		if ("use_attestation_challenge".equals(errorCode)) {
			// Per draft-ietf-oauth-attestation-based-client-auth-07 §6.2, the use_attestation_challenge
			// error MUST be accompanied by exactly one non-empty OAuth-Client-Attestation-Challenge
			// response header.
			List<String> challengeList = response.getHeaders().get("OAuth-Client-Attestation-Challenge");
			if (challengeList == null || challengeList.size() != 1 || Strings.isNullOrEmpty(challengeList.get(0))) {
				JsonObject jsonResponseHeaders = env.getObject("token_endpoint_response_headers");
				throw error("use_attestation_challenge error response did not include exactly one non-empty OAuth-Client-Attestation-Challenge header",
					args("headers", jsonResponseHeaders));
			}
			// token_endpoint_response is already populated from body_json by the superclass.
			env.putString("token_endpoint_use_attestation_challenge_error", errorCode);
			log("Got use_attestation_challenge error response — caller may retry with the freshly returned challenge",
				args("error", errorCode));
		}
	}
}

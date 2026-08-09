package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.http.DpopNonceResponseHeader;
import org.springframework.http.ResponseEntity;


/**
 * Wrapper around {@link CallTokenEndpointAndReturnFullResponse} that recognizes a {@code use_dpop_nonce} 400
 * response and exposes the supplied DPoP-Nonce so the caller can retry.
 */
public class CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse extends CallTokenEndpointAndReturnFullResponse {

	/** The nonce the server supplied on this response, or null if it supplied none. */
	private String suppliedDpopNonce;

	// WARNING optional token_endpoint_dpop_nonce_error returned with required nonce value
	@Override
	@PreEnvironment(required = { "server", "token_endpoint_request_form_parameters" })
	@PostEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		env.removeNativeValue("token_endpoint_dpop_nonce_error");
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

		if((status == 400) && (null != jsonError) && OIDFJSON.getString(jsonError).equals("use_dpop_nonce")) {
			if (dpopNonce == null) {
				throw error("The token endpoint returned a 'use_dpop_nonce' error but supplied no DPoP-Nonce"
					+ " header, leaving no nonce to retry the request with.",
					args("headers", jsonResponseHeaders));
			}
			env.putString("authorization_server_dpop_nonce", dpopNonce);
			env.putString("token_endpoint_dpop_nonce_error", dpopNonce);
			env.putObject("token_endpoint_response", env.getElementFromObject("token_endpoint_response_full", "body_json").getAsJsonObject());
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

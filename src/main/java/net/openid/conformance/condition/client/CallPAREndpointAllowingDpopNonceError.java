package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.http.DpopNonceResponseHeader;
import org.springframework.http.ResponseEntity;

/**
 * This class makes a http post to PAR endpoint and examines the response for DPoP nonce errors and stores the
 * required nonce for retry.
 */
public class CallPAREndpointAllowingDpopNonceError extends CallPAREndpoint {

	/** The nonce the server supplied on this response, or null if it supplied none. */
	private String suppliedDpopNonce;

	@Override
	public Environment evaluate(Environment env) {
		env.removeNativeValue("par_endpoint_dpop_nonce_error");
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

		if((status == 400) && (null != jsonError) && OIDFJSON.getString(jsonError).equals("use_dpop_nonce")) {
			if (dpopNonce == null) {
				throw error("The PAR endpoint returned a 'use_dpop_nonce' error but supplied no DPoP-Nonce"
					+ " header, leaving no nonce to retry the request with.",
					args("headers", jsonResponseHeaders));
			}
			env.putString("authorization_server_dpop_nonce", dpopNonce);
			env.putString("par_endpoint_dpop_nonce_error", dpopNonce);
			env.putObject("par_endpoint_response", env.getElementFromObject(RESPONSE_KEY, "body_json").getAsJsonObject());
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

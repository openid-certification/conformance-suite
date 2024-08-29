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


public class CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse extends CallTokenEndpointAndReturnFullResponse {

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
		if((status == 400) && (null != jsonError) && OIDFJSON.getString(jsonError).equals("use_dpop_nonce")) {
			List<String> nonceList = response.getHeaders().get("DPoP-Nonce");
			if(nonceList.size() != 1) {
				throw error("Unexpected DPoP-Nonce header response", args("headers", jsonResponseHeaders));
			}
			String dpopNonce = nonceList.get(0);
			if(!Strings.isNullOrEmpty(dpopNonce)) {
				env.putString("authorization_server_dpop_nonce", dpopNonce);
				env.putString("token_endpoint_dpop_nonce_error", dpopNonce);
				env.putObject("token_endpoint_response", env.getElementFromObject("token_endpoint_response_full", "body_json").getAsJsonObject());
				log("Got DPoP-Nonce header", args("DPoP-Nonce", dpopNonce));
			} else {
				throw error("Unexpected DPoP-Nonce header response", args("headers", jsonResponseHeaders));
			}
		}
	}
}

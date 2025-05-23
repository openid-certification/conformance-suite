package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.HttpHeaders;

public class VCIValidateCredentialNonceResponse extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject nonceResponseHeaders = env.getElementFromObject("endpoint_response", "headers").getAsJsonObject();
		if (!nonceResponseHeaders.has(HttpHeaders.CACHE_CONTROL.toLowerCase())) {
			throw error("Could not find Cache-Control in NonceResponse headers", args("nonce_response_headers", nonceResponseHeaders));
		}

		String cacheControlHeader = OIDFJSON.getString(nonceResponseHeaders.get(HttpHeaders.CACHE_CONTROL.toLowerCase()));
		if (!"no-store".equals(cacheControlHeader)) {
			throw error("Cache-Control header in NonceResponse headers must be 'no-store'.", args("nonce_response_headers", nonceResponseHeaders));
		}

		String nonceResponseBody = OIDFJSON.getString(env.getElementFromObject("endpoint_response", "body"));
		JsonObject nonceResponseObject = JsonParser.parseString(nonceResponseBody).getAsJsonObject();
		if (!nonceResponseObject.has("c_nonce")) {
			throw error("Could not find c_nonce in NonceResponse", args("nonce_response", nonceResponseObject));
		}

		String cnonce = OIDFJSON.getString(nonceResponseObject.get("c_nonce"));

		env.putString("vci", "c_nonce", cnonce);

		logSuccess("Found valid NonceResponse", args("nonce", cnonce, "cache-control", cacheControlHeader, "nonce_response", nonceResponseObject));
		return env;
	}
}

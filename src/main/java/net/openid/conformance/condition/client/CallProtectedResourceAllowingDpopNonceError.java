package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is to call a generic resource server endpoint with an access token using DPoP.
 * It will extract the required DPoP nonce in resource_server_dpop_nonce when use_dpop_nonce error is returned
 *
 * Note that this returns success if the HTTP transaction returns a valid response
 * (i.e. no network error occurred) - regardless of the http status - callers will
 * generally need to explicitly verify the http status.
 */
public class CallProtectedResourceAllowingDpopNonceError extends CallProtectedResource {


	@Override
	@PreEnvironment(required = "access_token", strings = "protected_resource_url")
	@PostEnvironment(required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {
		env.removeNativeValue("resource_endpoint_dpop_nonce_error");
		return super.evaluate(env);
	}



	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {

		super.handleClientResponse(env, responseCode, responseBody, responseHeaders, fullResponse);
		if(null != fullResponse) {
			int status = OIDFJSON.getInt(responseCode.get("code"));
			if(status == 401) {
				if(null != responseHeaders) {
					if(responseHeaders.has("www-authenticate")) {
						JsonArray wwwHeaderArray;
						if(responseHeaders.get("www-authenticate").isJsonArray()) {
							wwwHeaderArray = responseHeaders.getAsJsonArray("www-authenticate");
						} else {
							wwwHeaderArray = new JsonArray();
							wwwHeaderArray.add(responseHeaders.get("www-authenticate"));
						}

						// simple match key=\"value\", assumes only 1 challenge per header value
						Pattern pattern = Pattern.compile("\\s*([^=]+)=\"([^\"]+)\",*");
						for(JsonElement wwwHeaderElement : wwwHeaderArray) {
							String wwwHeader = OIDFJSON.getString(wwwHeaderElement);
							// Pattern dPopSchemePattern = Pattern.compile("^(DPoP)((\\s*([^=]+)=\"([^\"]+)\",*)+)$");
							if(wwwHeader.startsWith("DPoP ")) {
								Matcher matcher = pattern.matcher(wwwHeader.substring(5));
								HashMap<String, String> keyPairs = new HashMap<>();
								while(matcher.find()) {
									String key = matcher.group(1);
									String val = matcher.group(2);
									keyPairs.put(key, val);
								}
								if("use_dpop_nonce".equals(keyPairs.get("error"))) {
									if(responseHeaders.has("dpop-nonce")) {
										env.putString("resource_server_dpop_nonce", OIDFJSON.getString(responseHeaders.get("dpop-nonce")));
										env.putString("resource_endpoint_dpop_nonce_error", OIDFJSON.getString(responseHeaders.get("dpop-nonce")));
									}
								}
							}
						}
					}
				}
			}
		}
		return env;
	}

}

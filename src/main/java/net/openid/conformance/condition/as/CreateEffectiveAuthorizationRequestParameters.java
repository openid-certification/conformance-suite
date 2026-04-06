package net.openid.conformance.condition.as;

import com.google.crypto.tink.internal.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.io.IOException;

/**
 * Merges http request parameters and request object parameters
 * and creates effective_authorization_endpoint_request environment entry
 */
public class CreateEffectiveAuthorizationRequestParameters extends AbstractCondition {

	public static final String ENV_KEY = "effective_authorization_endpoint_request";

	//always use these constants to get values. just in case the keys, JsonObject structure change etc
	public static final String MAX_AGE = "max_age";
	public static final String PROMPT = "prompt";
	public static final String STATE = "state";
	public static final String REDIRECT_URI = "redirect_uri";
	public static final String RESPONSE_TYPE = "response_type";
	public static final String CLIENT_ID = "client_id";
	public static final String SCOPE = "scope";
	public static final String NONCE = "nonce";
	public static final String CODE_CHALLENGE = "code_challenge";
	public static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
	public static final String DPOP_JKT = "dpop_jkt";

	//WARNING "authorization_request_object" is also used but it's not required
	@Override
	@PreEnvironment(required = {"authorization_endpoint_http_request_params"})
	@PostEnvironment(required = {ENV_KEY})
	public Environment evaluate(Environment env) {
		return createEffectiveAuthorizationRequestParameters(env);
	}

	protected void customizeEffectiveAuthorizationRequestParams(Environment env, JsonObject jsonObject) {
	}

	protected Environment createEffectiveAuthorizationRequestParameters(Environment env) {

		JsonObject authzEndpointReqParams = env.getObject("authorization_endpoint_http_request_params");
		JsonObject effective = authzEndpointReqParams.deepCopy();
		effective.remove("request_uri");

		customizeEffectiveAuthorizationRequestParams(env, effective);

		convertJsonStringParam(effective, "authorization_details");
		convertJsonStringParam(effective, "dcql_query");
		convertJsonStringParam(effective, "client_metadata");

		// Normalize numeric HTTP query params (e.g. max_age arrives as string "99" from URL).
		// This runs BEFORE the request object merge so that request object types are preserved —
		// a request object that sends max_age as a string is a protocol violation that should be
		// caught by a downstream condition, not silently normalized here.
		for(String claimName : EnsureNumericRequestObjectClaimsAreNotNull.numericClaimNames) {
			if(effective.has(claimName)) {
				JsonElement claimJsonElement = effective.get(claimName);
				try {
					Number claimAsNumber = OIDFJSON.forceConversionToNumber(claimJsonElement);
					effective.addProperty(claimName, claimAsNumber);
				} catch (OIDFJSON.ValueIsJsonNullException | OIDFJSON.UnexpectedJsonTypeException ex) {
					// leave as-is; will be handled after request object merge
				}
			}
		}

		//override request parameters if authorization_request_object exists
		if(env.containsObject("authorization_request_object")) {
			JsonObject requestObjectClaims = env.getElementFromObject("authorization_request_object", "claims").getAsJsonObject();

			for (String paramName : requestObjectClaims.keySet()) {
				effective.add(paramName, requestObjectClaims.get(paramName));
			}
		}

		env.putObject(ENV_KEY, effective);
		logSuccess("Merged http request parameters with request object claims", args(ENV_KEY, effective));
		return env;
	}

	/**
	 * When parameters like dcql_query, client_metadata, or authorization_details are passed as URL query
	 * params (URL_QUERY request method), they arrive as JSON-serialized strings. Parse them back into
	 * JSON so downstream conditions can work with them the same way as when they come from a signed
	 * request object (JAR).
	 */
	protected void convertJsonStringParam(JsonObject params, String paramName) {
		if (!params.has(paramName)) {
			return;
		}
		JsonElement el = params.get(paramName);
		if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
			return; // already a JSON object/array, nothing to do
		}
		String jsonString = OIDFJSON.getString(el);
		try {
			JsonElement parsed = JsonParser.parse(jsonString);
			params.add(paramName, parsed);
		} catch (IOException e) {
			throw error("Unable to parse " + paramName + " as JSON", e, args(paramName, jsonString));
		}
	}

}

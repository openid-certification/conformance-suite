package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * grant_types
 * OPTIONAL. JSON array containing a list of the OAuth 2.0 Grant Types that the Client is declaring
 * that it will restrict itself to using. The Grant Type values used by OpenID Connect are:
 * 		authorization_code: The Authorization Code Grant Type described in OAuth 2.0 Section 4.1.
 * 		implicit: The Implicit Grant Type described in OAuth 2.0 Section 4.2.
 * 		refresh_token: The Refresh Token Grant Type described in OAuth 2.0 Section 6.
 * The following table lists the correspondence between response_type values that the Client will use
 * and grant_type values that MUST be included in the registered grant_types list:
 * 		code: authorization_code
 * 		id_token: implicit
 * 		token id_token: implicit
 * 		code id_token: authorization_code, implicit
 * 		code token: authorization_code, implicit
 * 		code token id_token: authorization_code, implicit
 * If omitted, the default is that the Client will use only the authorization_code Grant Type.
 *
 * NOTE: When grant_types is set but EMPTY we do NOT default to authorization_code.
 * It will default to authorization_code when grant_types is not included at all.
 */
public class ValidateClientGrantTypes extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");

		JsonArray grantTypes = getGrantTypes();
		JsonArray responseTypes = getResponseTypes();

		boolean needImplicit = false;
		boolean needAuthorizationCode = false;
		for(JsonElement responseTypeElement : responseTypes) {
			Set<String> responseTypeAsSet = new HashSet<>();
			responseTypeAsSet.addAll(Arrays.asList(OIDFJSON.getString(responseTypeElement).split(" ")));
			if(responseTypeAsSet.contains("code")) {
				needAuthorizationCode = true;
			}
			if(responseTypeAsSet.contains("token") || responseTypeAsSet.contains("id_token")) {
				needImplicit = true;
			}
		}

		JsonElement authorizationCodeJsonElement = new JsonPrimitive("authorization_code");
		JsonElement implicitJsonElement = new JsonPrimitive("implicit");

		if(needAuthorizationCode && !grantTypes.contains(authorizationCodeJsonElement)) {
			throw error("response_types require the use of authorization_code grant_type",
						args("grant_types", grantTypes, "response_types", responseTypes));
		}
		if(needImplicit && !grantTypes.contains(implicitJsonElement)) {
			throw error("response_types require the use of implicit grant_type",
				args("grant_types", grantTypes, "response_types", responseTypes));
		}

		logSuccess("grant_types match response_types",
					args("grant_types", grantTypes, "response_types", responseTypes));
		return env;
	}
}

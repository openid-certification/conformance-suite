package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VerifyScopesReturnedInAuthorizationEndpointIdToken extends AbstractCondition {

	// https://github.com/OpenIDC/pyoidc/blob/master/src/oic/oic/message.py#L996
	@SuppressWarnings("serial")
	private final Map<String, List<String>> SCOPE_STANDARD_CLAIMS = new HashMap<>() {{
		put("openid", new ArrayList<>(Arrays.asList("sub")));
		put("profile", new ArrayList<>(Arrays.asList("name",
			"given_name",
			"family_name",
			"middle_name",
			"nickname",
			"profile",
			"picture",
			"website",
			"gender",
			"birthdate",
			"zoneinfo",
			"locale",
			"updated_at",
			"preferred_username")));
		put("email", new ArrayList<>(Arrays.asList("email", "email_verified")));
		put("address", new ArrayList<>(Arrays.asList("address")));
		put("phone", new ArrayList<>(Arrays.asList("phone_number", "phone_number_verified")));
		put("offline_access", new ArrayList<>());
	}};

	@Override
	@PreEnvironment(required = { "authorization_endpoint_id_token", "authorization_endpoint_request" })
	public Environment evaluate(Environment env) {

		JsonElement claims = env.getElementFromObject("authorization_endpoint_id_token", "claims");

		if (claims == null || !claims.isJsonObject()) {
			throw error("'claims' in id_token is invalid", args("claims", claims));
		}

		String scopeStr = env.getString("authorization_endpoint_request", "scope");

		if (Strings.isNullOrEmpty(scopeStr)) {
			throw error("Not found 'scope' in authorization endpoint request");
		}

		Set<String> claimsSet = claims.getAsJsonObject().keySet();
		String [] scopeArr = scopeStr.split(" ");

		// https://github.com/rohe/oidctest/blob/master/src/oidctest/op/check.py#L2464
		for (String scope : scopeArr) {
			List<String> scopeNames = SCOPE_STANDARD_CLAIMS.get(scope);
			if (!claimsSet.containsAll(scopeNames)) {
				throw error("'claims' in id_token doesn't contain all scope names", args("claims", claims, "scope_standard_claims", SCOPE_STANDARD_CLAIMS));
			}
		}

		logSuccess("All scope names returned in id_token from authorization endpoint response", args("claims", claims, "scope_standard_claims", SCOPE_STANDARD_CLAIMS));

		return env;
	}
}

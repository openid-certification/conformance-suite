package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractVerifyScopesReturnedInClaims extends AbstractCondition {

	// https://github.com/OpenIDC/pyoidc/blob/master/src/oic/oic/message.py#L996
	@SuppressWarnings("serial")
	protected final Map<String, List<String>> SCOPE_STANDARD_CLAIMS = new HashMap<>() {{
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

	protected Environment verifyScopesInClaims(Environment env, JsonElement claims, String claimsKey) {

		if (claims == null || !claims.isJsonObject()) {
			throw error("'claims' in " + claimsKey + " is invalid", args("claims", claims));
		}

		String scopeStr = env.getString("authorization_endpoint_request", "scope");

		if (Strings.isNullOrEmpty(scopeStr)) {
			throw error("'scope' not found in authorization endpoint request");
		}

		Set<String> claimsSet = claims.getAsJsonObject().keySet();
		String [] scopeArr = scopeStr.split(" ");

		// https://github.com/rohe/oidctest/blob/master/src/oidctest/op/check.py#L2464
		List<String> expectedScopeItems = new ArrayList<>();
		for (String scope : scopeArr) {
			List<String> scopeItems = SCOPE_STANDARD_CLAIMS.get(scope);
			expectedScopeItems.addAll(scopeItems);
		}

		Set<String> missingItems = new HashSet<>(expectedScopeItems);
		missingItems.removeAll(claimsSet);

		if (!claimsSet.containsAll(expectedScopeItems)) {
			throw error("'claims' in " + claimsKey + " doesn't contain all scope items of scope in authorization request(corresponds to scope standard claims)", args("actual_scope_items", claimsSet, "expected_scope_items", expectedScopeItems, "missing_items", missingItems));
		}

		logSuccess("'claims' in " + claimsKey + " contains all scope items of scope in authorization request (corresponds to scope standard claims)", args("actual_scope_items", claimsSet, "expected_scope_items", expectedScopeItems));

		return env;
	}
}

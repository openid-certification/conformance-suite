package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CheckScopesFromDynamicRegistrationEndpointDoNotExceedRequestedOpenBankingScopes extends AbstractCondition {

	private static final List<String> OPEN_BANKING_SCOPES = List.of("openid", "payments", "accounts", "credit-cards-accounts", "consents",
		"customers", "invoice-financings", "financings", "loans", "unarranged-accounts-overdraft", "resources");
	@Override
	@PreEnvironment(required = "dynamic_registration_endpoint_response")
	public Environment evaluate(Environment env) {

		String grantedScope = getStringFromEnvironment(env, "dynamic_registration_endpoint_response", "body_json.scope", "scope in dynamic registration response");
		String requestedScope = getStringFromEnvironment(env, "dynamic_registration_request", "scope", "scope in dynamic registration request");

		Set<String> scopesGranted = Set.of(grantedScope.split(" "));
		Set<String> scopesRequested = Set.of(requestedScope.split(" "));
		ArrayList<String> notRequested = new ArrayList<>();
		ArrayList<String> ignored = new ArrayList<>();
		for(String scopeValue : scopesGranted) {
			if (OPEN_BANKING_SCOPES.contains(scopeValue)) {
				if(!scopesRequested.contains(scopeValue)) {
					notRequested.add(scopeValue);
				}
			}else {
				ignored.add(scopeValue);
			}
		}

		if (!notRequested.isEmpty()) {
			throw error("'scope' in dynamic registration response contains open banking scope(s) that were not requested.",
				args("granted", grantedScope, "requested", requestedScope, "not_requested", notRequested, "ignored", ignored));
		}

		logSuccess("'scope' in dynamic registration response contains only open banking scopes that were requested.",
			args("granted", grantedScope, "requested", requestedScope, "ignored", ignored));

		return env;
	}
}

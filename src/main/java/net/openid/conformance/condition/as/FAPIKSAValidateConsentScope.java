package net.openid.conformance.condition.as;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class FAPIKSAValidateConsentScope extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"scope"})
	public Environment evaluate(Environment env) {

		String scope = env.getString("scope");
		String consentId = env.getString("account_request_id");
		if (consentId == null) {
			throw error("You must create an account consent first by calling the account request endpoint.");
		}
		String consentScope = "accounts:" + consentId;
		List<String> scopes = Lists.newArrayList(Splitter.on(" ").split(scope).iterator());

		if (scopes.contains(consentScope)) {
			logSuccess("Found consent scope in request", args("expected", consentScope, "actual", scopes));
			return env;
		} else {
			throw error("Couldn't find consent scope in request", args("expected", consentScope, "actual", scopes));
		}
	}

}

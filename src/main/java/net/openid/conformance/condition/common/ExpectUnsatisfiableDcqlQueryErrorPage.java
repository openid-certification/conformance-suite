package net.openid.conformance.condition.common;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectUnsatisfiableDcqlQueryErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "unsatisfiable_dcql_query_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder(
			"The DCQL query contains a required Credential Query the wallet cannot satisfy, so the "
				+ "wallet must not return a vp_token. If the wallet does not return an 'access_denied' "
				+ "error response, it should display an error.");
		env.putString("unsatisfiable_dcql_query_error", placeholder);

		return env;
	}
}

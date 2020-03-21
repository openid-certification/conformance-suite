package net.openid.conformance.condition.as;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;
import java.util.Set;

/**
 * https://tools.ietf.org/html/rfc6749#section-6
 * scope
 *  OPTIONAL.  The scope of the access request as described by
 *  Section 3.3.  The requested scope MUST NOT include any scope
 *  not originally granted by the resource owner, and if omitted is
 *  treated as equal to the scope originally granted by the
 *  resource owner.
 */
public class EnsureScopeInRefreshRequestMatchesOriginallyGranted extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "scope", required = "token_endpoint_request")
	public Environment evaluate(Environment env) {

		String grantedScope = env.getString("scope");
		String requestedScope = env.getString("token_endpoint_request", "body_form_params.scope");
		if(requestedScope==null) {
			logSuccess("Refresh request does not contain scope parameter and is assumed to be the same as originally granted.");
			return env;
		}
		Set<String> scopesGranted = Set.of(grantedScope.split(" "));
		Set<String> scopesRequested = Set.of(requestedScope.split(" "));
		for(String scopeValue : scopesRequested) {
			if(!scopesGranted.contains(scopeValue)) {
				throw error("Scope value in refresh request does not match the originally granted scope.",
					args("originally_granted", grantedScope, "requested", requestedScope));
			}
		}

		logSuccess("Scope value in refresh request matches the originally granted scope.",
					args("originally_granted", grantedScope, "requested", requestedScope));
		return env;
	}

}

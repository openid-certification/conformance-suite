package net.openid.conformance.openid.ssf.conditions;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfConstants;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;
import java.util.List;

/**
 * Guards the read-only-token negative test: the token the suite obtained for {@code ssf.read}
 * must not also carry {@code ssf.manage}. RFC 6749 section 3.3 lets the authorization server
 * "fully or partially ignore the scope requested by the client", so a client that is always
 * granted {@code ssf.manage} yields a token with which a conformant transmitter correctly
 * accepts the create request - the scope enforcement under test would then be impossible to
 * exercise, and the transmitter must not be blamed for it. Per RFC 6749 section 5.1 an omitted
 * {@code scope} in the token response means the granted scope is identical to the requested
 * one.
 */
public class OIDSSFEnsureGrantedScopeIsReadOnly extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		String grantedScope = env.getString("token_endpoint_response", "scope");
		if (Strings.isNullOrEmpty(grantedScope)) {
			logSuccess("The token response carries no scope, so the granted scope is identical to the requested '"
				+ SsfConstants.SCOPE_SSF_READ + "' (RFC 6749 section 5.1)");
			return env;
		}

		List<String> grantedScopes = Arrays.asList(grantedScope.trim().split("\\s+"));
		if (grantedScopes.contains(SsfConstants.SCOPE_SSF_MANAGE)) {
			throw error("The authorization server granted '" + SsfConstants.SCOPE_SSF_MANAGE + "' although only '"
					+ SsfConstants.SCOPE_SSF_READ + "' was requested. RFC 6749 section 3.3 permits this, but a token that may "
					+ "manage streams cannot show whether the transmitter refuses stream creation to a read-only token, so this "
					+ "test cannot run. Configure the client used in the 'Client' section of the test configuration at the "
					+ "authorization server so that it can be issued '" + SsfConstants.SCOPE_SSF_READ + "' on its own.",
				args("requested_scope", SsfConstants.SCOPE_SSF_READ, "granted_scope", grantedScope));
		}

		logSuccess("The authorization server granted a read-only token",
			args("requested_scope", SsfConstants.SCOPE_SSF_READ, "granted_scope", grantedScope));
		return env;
	}
}

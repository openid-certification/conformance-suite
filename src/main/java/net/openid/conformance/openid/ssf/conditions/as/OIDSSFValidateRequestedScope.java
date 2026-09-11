package net.openid.conformance.openid.ssf.conditions.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfConstants;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Determines the SSF scope granted on a client_credentials token request
 * (CAEP Interop Profile §2.7.3 "OAuth Scopes": {@code ssf.read} and
 * {@code ssf.manage}). The granted scope string is echoed into
 * {@code env.scope} so {@link
 * net.openid.conformance.condition.as.CreateTokenEndpointResponse} includes it
 * in the token endpoint response.
 * <p>
 * This condition never fails a spec-conformant receiver:
 * <ul>
 *   <li>RFC 6749 §4.4.2 makes the {@code scope} parameter OPTIONAL for the
 *       client_credentials grant — a request without it is granted both SSF
 *       scopes (the emulated AS's default per RFC 6749 §3.3).</li>
 *   <li>RFC 6749 §3.3 allows the AS to ignore scopes it does not recognise —
 *       non-SSF scope values (e.g. {@code openid}) are logged and ignored, and
 *       only the requested SSF subset is granted.</li>
 * </ul>
 */
public class OIDSSFValidateRequestedScope extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_request")
	@PostEnvironment(strings = "scope")
	public Environment evaluate(Environment env) {

		String scopeParam = env.getString("token_endpoint_request", "body_form_params.scope");

		String defaultScope = SsfConstants.SCOPE_SSF_READ + " " + SsfConstants.SCOPE_SSF_MANAGE;

		if (scopeParam == null || scopeParam.isBlank()) {
			// RFC 6749 §4.4.2: scope is OPTIONAL; §3.3: absent scope means the AS
			// processes the request using a pre-defined default value.
			env.putString("scope", defaultScope);
			logSuccess("Token request contains no 'scope' parameter, which is optional; granting the default SSF scopes",
				args("granted_scope", defaultScope));
			return env;
		}

		Set<String> requestedScopes = new LinkedHashSet<>(Arrays.asList(scopeParam.trim().split("\\s+")));

		Set<String> requestedSsfScopes = new LinkedHashSet<>(requestedScopes);
		requestedSsfScopes.retainAll(SsfConstants.SSF_SCOPES);

		Set<String> ignoredScopes = new LinkedHashSet<>(requestedScopes);
		ignoredScopes.removeAll(SsfConstants.SSF_SCOPES);

		if (!ignoredScopes.isEmpty()) {
			log("Ignoring requested scope values that are not SSF scopes, as an authorization server may ignore scopes it does not recognise",
				args("requested_scope", scopeParam, "ignored_scopes", ignoredScopes,
					"ssf_scopes", SsfConstants.SSF_SCOPES));
		}

		String grantedScope = requestedSsfScopes.isEmpty()
			? defaultScope
			: String.join(" ", requestedSsfScopes);

		env.putString("scope", grantedScope);

		logSuccess("Determined granted SSF scope for token request",
			args("requested_scope", scopeParam, "granted_scope", grantedScope));

		return env;
	}
}

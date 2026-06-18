package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Emulated resource-server authorization check (CAEP Interop Profile §2.7.2:
 * "The SSF Transmitter as a Resource Server" — the transmitter MUST verify that
 * the authorization represented by the access token is sufficient for the
 * requested resource access).
 * <p>
 * Asserts that the scope granted to the dynamic-mode access token (stashed at
 * {@code ssf.current_token_scope} by {@link OIDSSFHandleAuthorizationHeader})
 * includes the scope required for the requested operation — {@code ssf.read}
 * for read/status operations, {@code ssf.manage} for stream-management
 * operations (create/update/replace/delete, verification, subject changes).
 * <p>
 * Follows the no-throw {@code ssf.auth_result} convention: on insufficient
 * scope it records an {@code insufficient_scope} error with HTTP 403 (RFC 6750
 * §3.1) so the caller returns the proper response rather than failing the test.
 */
public class OIDSSFEnsureTokenScopeSufficient extends AbstractOIDSSFHandleReceiverRequest {

	protected final String requiredScope;

	public OIDSSFEnsureTokenScopeSufficient(String requiredScope) {
		this.requiredScope = requiredScope;
	}

	@Override
	public Environment evaluate(Environment env) {

		JsonObject authResult = env.getElementFromObject("ssf", "auth_result").getAsJsonObject();

		String grantedScope = env.getString("ssf", "current_token_scope");
		Set<String> grantedScopes = grantedScope == null || grantedScope.isBlank()
			? Set.of()
			: new LinkedHashSet<>(Arrays.asList(grantedScope.trim().split("\\s+")));

		if (!grantedScopes.contains(requiredScope)) {
			authResult.add("error", createErrorObj("insufficient_scope",
				"Access token scope is insufficient for this operation. Required scope: " + requiredScope));
			authResult.addProperty("status_code", 403);
			log("Access token scope is insufficient for the requested operation",
				args("required_scope", requiredScope, "granted_scope", grantedScope));
			return env;
		}

		logSuccess("Access token scope is sufficient for the requested operation",
			args("required_scope", requiredScope, "granted_scope", grantedScope));

		return env;
	}
}

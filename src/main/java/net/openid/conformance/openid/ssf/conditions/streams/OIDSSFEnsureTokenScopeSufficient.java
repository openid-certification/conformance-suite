package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.SsfConstants;
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
 * is sufficient for the requested operation — {@code ssf.read} for read/status
 * operations, {@code ssf.manage} for stream-management operations
 * (create/update/replace/delete, verification, subject changes). Per CAEP
 * Interop Profile §2.7.3 "The ssf.manage scope includes all ssf.read
 * permissions", so a token granted only {@code ssf.manage} satisfies an
 * {@code ssf.read} requirement.
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

		if (!isSufficient(grantedScopes)) {
			authResult.add("error", createErrorObj("insufficient_scope",
				"Access token scope is insufficient for this operation. Required scope: " + requiredScope));
			authResult.addProperty("status_code", 403);
			// RFC 6750 3.1: insufficient_scope responses SHOULD name the required scope
			authResult.addProperty("www_authenticate", "Bearer error=\"insufficient_scope\", scope=\"" + requiredScope + "\"");
			log("Access token scope is insufficient for the requested operation",
				args("required_scope", requiredScope, "granted_scope", grantedScope));
			return env;
		}

		logSuccess("Access token scope is sufficient for the requested operation",
			args("required_scope", requiredScope, "granted_scope", grantedScope));

		return env;
	}

	protected boolean isSufficient(Set<String> grantedScopes) {
		if (grantedScopes.contains(requiredScope)) {
			return true;
		}
		// CAEP Interop Profile §2.7.3: "The ssf.manage scope includes all ssf.read
		// permissions" — a manage-only token may perform read operations.
		return SsfConstants.SCOPE_SSF_READ.equals(requiredScope)
			&& grantedScopes.contains(SsfConstants.SCOPE_SSF_MANAGE);
	}
}

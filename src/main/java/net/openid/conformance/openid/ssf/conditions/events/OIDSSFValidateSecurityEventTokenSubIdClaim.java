package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfSubjectIdentifiers;
import net.openid.conformance.testmodule.Environment;

/**
 * Validates the top-level {@code sub_id} claim of a parsed SET.
 * <p>
 * SSF 1.0 §3.1 requires a top-level {@code sub_id} claim describing the primary subject of every
 * SSF event, and §3.2 / §3.3 require its value to be a Subject Identifier per RFC 9493 (a Simple
 * Subject) or a Complex Subject. This condition checks presence and structural validity only —
 * profile restrictions on the permitted formats are checked separately, e.g. by
 * {@link OIDSSFEnsureCaepInteropEventSubjectFormat}.
 */
public class OIDSSFValidateSecurityEventTokenSubIdClaim extends AbstractCondition {

	@Override
	@PreEnvironment(required = "set_token")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getElementFromObject("set_token", "claims").getAsJsonObject();

		JsonElement subId = claims.get("sub_id");
		if (subId == null) {
			throw error("SET does not contain the required top-level 'sub_id' claim", args("claims", claims));
		}

		if (!subId.isJsonObject()) {
			throw error("The 'sub_id' claim must be a JSON object containing a subject identifier", args("sub_id", subId));
		}

		try {
			SsfSubjectIdentifiers.validate(subId);
		} catch (SsfSubjectIdentifiers.InvalidSubjectIdentifierException e) {
			throw error("The 'sub_id' claim does not contain a valid subject identifier: " + e.getMessage(),
				args("sub_id", subId, "format", SsfSubjectIdentifiers.getFormat(subId)));
		}

		logSuccess("The 'sub_id' claim contains a structurally valid subject identifier",
			args("sub_id", subId, "format", SsfSubjectIdentifiers.getFormat(subId)));

		return env;
	}
}

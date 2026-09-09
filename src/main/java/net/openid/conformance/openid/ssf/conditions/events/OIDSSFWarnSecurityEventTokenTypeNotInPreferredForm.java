package net.openid.conformance.openid.ssf.conditions.events;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import static net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenUsesTypeSecEventJwt.SECEVENT_JWT_MEDIA_TYPE;
import static net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenUsesTypeSecEventJwt.SECEVENT_JWT_TYP;
import static net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureSecurityEventTokenUsesTypeSecEventJwt.toMediaType;

/**
 * RFC 8417 2.3: "it is RECOMMENDED that the 'application/' prefix be omitted. Therefore, the
 * 'typ' value used SHOULD be 'secevent+jwt'". Raises a finding when the {@code typ} header names
 * the right media type but not in that spelling (e.g. {@code application/secevent+jwt} or a
 * different letter case). A missing or different media type is
 * {@link OIDSSFEnsureSecurityEventTokenUsesTypeSecEventJwt}'s FAILURE-level finding, not
 * repeated here. Callers invoke this condition at WARNING.
 */
public class OIDSSFWarnSecurityEventTokenTypeNotInPreferredForm extends AbstractCondition {

	@PreEnvironment(required = {"set_token"})
	@Override
	public Environment evaluate(Environment env) {

		String tokenType = env.getString("set_token", "header.typ");

		if (tokenType == null || !SECEVENT_JWT_MEDIA_TYPE.equals(toMediaType(tokenType))) {
			log("Token type is missing or names a different media type; reported by the explicit-typing check",
				args("typ", tokenType));
			return env;
		}

		if (!SECEVENT_JWT_TYP.equals(tokenType)) {
			throw error("Token type names the '" + SECEVENT_JWT_MEDIA_TYPE + "' media type but not in the spelling "
					+ "RFC 8417 section 2.3 recommends: the typ value SHOULD be '" + SECEVENT_JWT_TYP + "'",
				args("typ", tokenType, "recommended_typ", SECEVENT_JWT_TYP));
		}

		logSuccess("Token type uses the recommended spelling '" + SECEVENT_JWT_TYP + "'", args("typ", tokenType));

		return env;
	}
}

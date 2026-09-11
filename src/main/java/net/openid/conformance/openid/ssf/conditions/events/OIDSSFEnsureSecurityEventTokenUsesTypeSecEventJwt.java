package net.openid.conformance.openid.ssf.conditions.events;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Locale;

/**
 * SSF 1.0 4.1.1: "SSF events MUST use explicit typing as defined in Section 2.3 of
 * [RFC8417]", i.e. the JWT {@code typ} header MUST name the {@code application/secevent+jwt}
 * media type. RFC 7515 4.1.9 makes media type values case insensitive and tells a recipient to
 * "treat it as if 'application/' were prepended to any 'typ' value not containing a '/'", so
 * {@code secevent+jwt}, {@code application/secevent+jwt} and {@code SecEvent+JWT} all satisfy
 * the MUST. Whether the RECOMMENDED short spelling was used is a separate, WARNING-level
 * finding: {@link OIDSSFWarnSecurityEventTokenTypeNotInPreferredForm}.
 */
public class OIDSSFEnsureSecurityEventTokenUsesTypeSecEventJwt extends AbstractCondition {

	public static final String SECEVENT_JWT_TYP = "secevent+jwt";

	public static final String SECEVENT_JWT_MEDIA_TYPE = "application/" + SECEVENT_JWT_TYP;

	@PreEnvironment(required = {"set_token"})
	@Override
	public Environment evaluate(Environment env) {

		String tokenType = env.getString("set_token", "header.typ");

		if (tokenType == null) {
			throw error("Couldn't find typ claim in JWT header. SETs must be explicitly typed as '"
				+ SECEVENT_JWT_TYP + "'");
		}

		String mediaType = toMediaType(tokenType);
		if (!SECEVENT_JWT_MEDIA_TYPE.equals(mediaType)) {
			throw error("Invalid token type '" + tokenType + "'. Should be '" + SECEVENT_JWT_TYP + "'",
				args("typ", tokenType, "media_type", mediaType, "expected_media_type", SECEVENT_JWT_MEDIA_TYPE));
		}

		logSuccess("Token type names the '" + SECEVENT_JWT_MEDIA_TYPE + "' media type", args("typ", tokenType));

		return env;
	}

	/**
	 * The media type a {@code typ} value names, per RFC 7515 4.1.9: case insensitive, with
	 * {@code application/} implied when the value carries no '/'.
	 */
	static String toMediaType(String typ) {
		String lower = typ.trim().toLowerCase(Locale.ROOT);
		return lower.contains("/") ? lower : "application/" + lower;
	}
}

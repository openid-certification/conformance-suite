package net.openid.conformance.openid.ssf.conditions.events;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;

/**
 * Raises a finding when the {@code err} code of a push delivery error response is not a
 * registered Security Event Token Error Code: the initial IANA registry contents of
 * RFC 8935 2.4 plus {@code invalid_state}, which SSF 1.0 8.1.4.1 registers for a
 * verification event whose state does not match. The registry is extensible, so the
 * caller grades this as a WARNING.
 */
public class OIDSSFWarnPushDeliveryErrorCodeNotRegistered extends AbstractOIDSSFPushDeliveryErrorCodeCheck {

	public static final String ERROR_CODE_INVALID_REQUEST = "invalid_request";
	public static final String ERROR_CODE_INVALID_KEY = "invalid_key";
	public static final String ERROR_CODE_INVALID_ISSUER = "invalid_issuer";
	public static final String ERROR_CODE_INVALID_AUDIENCE = "invalid_audience";
	public static final String ERROR_CODE_AUTHENTICATION_FAILED = "authentication_failed";
	public static final String ERROR_CODE_ACCESS_DENIED = "access_denied";
	public static final String ERROR_CODE_INVALID_STATE = "invalid_state";

	public static final Set<String> REGISTERED_ERROR_CODES = Set.of(
		ERROR_CODE_INVALID_REQUEST,
		ERROR_CODE_INVALID_KEY,
		ERROR_CODE_INVALID_ISSUER,
		ERROR_CODE_INVALID_AUDIENCE,
		ERROR_CODE_AUTHENTICATION_FAILED,
		ERROR_CODE_ACCESS_DENIED,
		ERROR_CODE_INVALID_STATE
	);

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		String errorCode = getErrorCode(env);
		if (errorCode == null) {
			log("The error response carries no string 'err' member, so there is no error code to check against the registry");
			return env;
		}

		if (!REGISTERED_ERROR_CODES.contains(errorCode)) {
			throw error("The 'err' code of the error response is not a registered Security Event Token Error Code",
				args("err", errorCode, "registered_codes", REGISTERED_ERROR_CODES));
		}

		logSuccess("The 'err' code of the error response is a registered Security Event Token Error Code", args("err", errorCode));
		return env;
	}
}

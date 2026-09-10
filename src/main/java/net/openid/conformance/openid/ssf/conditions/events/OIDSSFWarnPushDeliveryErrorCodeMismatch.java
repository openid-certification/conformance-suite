package net.openid.conformance.openid.ssf.conditions.events;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Raises a finding when the {@code err} code of a push delivery error response differs from
 * the code the scenario expects for the defect the delivered SET carries (e.g.
 * {@code invalid_key} for a bad signature, RFC 8935 2.4; {@code invalid_state} for a
 * verification event with an unexpected state, SSF 1.0 8.1.4.1). Neither specification
 * binds one code to one cause, so the caller grades this as a WARNING.
 */
public class OIDSSFWarnPushDeliveryErrorCodeMismatch extends AbstractOIDSSFPushDeliveryErrorCodeCheck {

	protected final String expectedErrorCode;

	public OIDSSFWarnPushDeliveryErrorCodeMismatch(String expectedErrorCode) {
		this.expectedErrorCode = expectedErrorCode;
	}

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		String errorCode = getErrorCode(env);
		if (errorCode == null) {
			log("The error response carries no string 'err' member, so there is no error code to compare with the expected one",
				args("expected", expectedErrorCode));
			return env;
		}

		if (!expectedErrorCode.equals(errorCode)) {
			throw error("The 'err' code of the error response is not the code expected for the defect the delivered SET carries",
				args("err", errorCode, "expected", expectedErrorCode));
		}

		logSuccess("The 'err' code of the error response matches the defect the delivered SET carries", args("err", errorCode));
		return env;
	}
}

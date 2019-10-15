package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.OIDFJSON;

public abstract class AbstractExpectAccessDeniedErrorDueToUserRejectingRequest extends AbstractCondition {

	protected void checkAccessDeniedError(JsonObject callbackParams) {

		if (!callbackParams.has("error")) {
			throw error("error parameter not found. When running this test, the tester MUST press 'cancel' on the login screen or deny consent so that an error is returned to the relying party.", callbackParams);
		}

		String error = OIDFJSON.getString(callbackParams.get("error"));
		if (Strings.isNullOrEmpty(error)) {
			throw error("error parameter empty/invalid. When running this test, the tester MUST press 'cancel' on the login screen or deny consent so that an error is returned to the relying party.", callbackParams);
		}

		String expected = "access_denied";
		if (!error.equals(expected)) {
			throw error("error value is incorrect. When running this test, the tester MUST press 'cancel' on the login screen or deny consent so that an error is returned to the relying party.",
				args("expected", expected, "actual", error));
		}

		logSuccess("error parameter is correctly '"+error+"'");

	}

}

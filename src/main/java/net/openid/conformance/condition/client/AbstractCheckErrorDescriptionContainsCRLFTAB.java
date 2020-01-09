package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractCheckErrorDescriptionContainsCRLFTAB extends AbstractCondition {

	protected Environment checkExistCRLFTAB(Environment env, String endpointResponseKey) {
		String errorDescription = env.getString(endpointResponseKey, "error_description");

		if (Strings.isNullOrEmpty(errorDescription)) {
			logSuccess(endpointResponseKey + " did not include optional 'error_description' field");
			return env;
		}
		if (isExistCRLFTAB(errorDescription)) {
			throw error("'error_description' field includes characters CR, LF or TAB, these are not recommended to include", args("error_description", errorDescription, "see", "https://bitbucket.org/openid/connect/issues/1147/certification-rfc6749-must-for"));
		}
		logSuccess(endpointResponseKey + " 'error_description' field does not include CR/LF/TAB", args("error_description", errorDescription));
		return env;
	}

	private boolean isExistCRLFTAB(String str) {
		if (str.contains("\n") || str.contains("\r") || str.contains("\t")) {
			return true;
		}
		return false;
	}
}

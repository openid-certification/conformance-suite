package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractPreAuthorizedCodeFromIssuanceInitiationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "issuance_initiation_request")
	public Environment evaluate(Environment env) {
		String code = env.getString("issuance_initiation_request", "pre-authorized_code");
		if (Strings.isNullOrEmpty(code)) {
			throw error("Couldn't find pre-authorized_code in issuance initiation request");
		}

		env.putString("preauthorizedcode", code);
		logSuccess("Found pre-authorized_code",
			args("pre-authorized_code", code));
		return env;
	}

}

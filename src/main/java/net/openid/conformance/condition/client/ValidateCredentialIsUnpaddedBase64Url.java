package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateCredentialIsUnpaddedBase64Url extends AbstractCheckUnpaddedBase64Url {
	@Override
	@PreEnvironment(strings = "credential")
	public Environment evaluate(Environment env) {
		// as per ISO 18013-7, vp_token is a base64url-encoded-without-padding DeviceResponse data structure as defined in ISO/IEC 18013-5.
		String mdocBase64 = env.getString("credential");

		checkUnpaddedBase64Url(mdocBase64, "credential");

		return env;
	}

}

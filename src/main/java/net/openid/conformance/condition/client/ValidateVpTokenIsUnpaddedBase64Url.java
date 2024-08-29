package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.regex.Pattern;

public class ValidateVpTokenIsUnpaddedBase64Url extends AbstractCondition {
	@Override
	@PreEnvironment(strings = "vp_token")
	public Environment evaluate(Environment env) {
		// as per ISO 18013-7, vp_token is a base64url-encoded-without-padding DeviceResponse data structure as defined in ISO/IEC 18013-5.
		String mdocBase64 = env.getString("vp_token");

		String regex = "[a-zA-Z0-9_-]";
		for (int i = 0; i < mdocBase64.length(); i++) {
			char character = mdocBase64.charAt(i);
			if (!Pattern.matches(regex, String.valueOf(character))) {
				throw error("Returned vp_token is invalid because it contains characters not permitted in unpadded base64url",
					args("bad_character", "'"+character+"'"));
			}
		}

		logSuccess("vp_token contains only characters permitted in unpadded base64url");

		return env;
	}

}

package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPICIBAValidateRtHash extends AbstractValidateHash {

	@Override
	@PreEnvironment(required = { "token_endpoint_response", "rt_hash" } )
	public Environment evaluate(Environment env) {
		return super.validateHash(env,"rt_hash","rt_hash");
	}

	@Override
	protected String getBaseStringBasedOnType(Environment env, String hashName) {

		String baseString = null;

		switch (hashName) {
			case "rt_hash":
				baseString = env.getString("token_endpoint_response", "refresh_token");
				if (Strings.isNullOrEmpty(baseString)) {
					throw error("Could not get refresh_token in token_endpoint_response object...");
				}
				break;
			default:
				throw error("Invalid HashName(" + hashName + ")");
		}

		return baseString;
	}
}

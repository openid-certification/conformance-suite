package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWEUtil;

public class ValidateEncryptedRequestObjectHasKid extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {
		String alg = env.getString("authorization_request_object", "jwe_header.alg");
		if(JWEUtil.isSymmetricJWEAlgorithm(alg)) {
			logSuccess("skipping KID check for symmetric alg " + alg);
		} else {
			String kid = env.getString("authorization_request_object", "jwe_header.kid");
			if (Strings.isNullOrEmpty(kid)) {
				throw error("kid was not found in the encrypted request object header");
			}
			logSuccess("kid was found in the encrypted request object header", args("kid", kid));
		}
		return env;
	}

}

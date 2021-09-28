package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 *  id_token_signed_response_alg
 *  Always PS256
 */
public class FAPIBrazilValidateUserinfoSignedResponseAlg extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");

		String alg = getUserinfoSignedResponseAlg();
		if(alg == null){
			log("userinfo_signed_response_alg was not included in registration request");
			return env;
		}
		if("PS256".equals(alg)) {
			logSuccess("userinfo_signed_response_alg is PS256");
			return env;
		} else {
			throw error("Unexpected userinfo_signed_response_alg", args("alg", alg));
		}
	}
}

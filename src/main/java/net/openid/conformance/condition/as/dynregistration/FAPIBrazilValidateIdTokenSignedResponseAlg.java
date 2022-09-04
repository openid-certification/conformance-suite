package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 *  id_token_signed_response_alg
 *  Always PS256
 */
public class FAPIBrazilValidateIdTokenSignedResponseAlg extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");

		String alg = getIdTokenSignedResponseAlg();
		if(alg==null){
			log("id_token_signed_response_alg was not included in registration request");
			return env;
		}
		if("PS256".equals(alg)) {
			logSuccess("id_token_signed_response_alg is PS256");
			return env;
		} else {
			throw error("Unexpected id_token_signed_response_alg", args("alg", alg));
		}
	}
}

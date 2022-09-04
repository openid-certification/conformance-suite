package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWSUtil;

/**
 *  userinfo_signed_response_alg
 *  OPTIONAL. JWS alg algorithm [JWA] REQUIRED for signing UserInfo Responses. If this is specified,
 *  the response will be JWT [JWT] serialized, and signed using JWS. The default, if omitted,
 *  is for the UserInfo Response to return the Claims as a UTF-8 encoded JSON object using the
 *  application/json content-type.
 *
 *   Please note: This condition does not validate if the current test actually supports the alg,
 *   it will fail later anyway
 */
public class ValidateUserinfoSignedResponseAlg extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");

		String alg = getUserinfoSignedResponseAlg();

		if(JWSUtil.isValidJWSAlgorithm(alg)) {
			logSuccess("userinfo_signed_response_alg is one of the known algorithms",
						args("alg", alg));
			return env;
		}
		throw error("Unexpected userinfo_signed_response_alg", args("alg", alg));
	}
}

package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWSUtil;

/**
 *  request_object_signing_alg
 *  OPTIONAL. JWS [JWS] alg algorithm [JWA] that MUST be used for signing Request Objects
 *  sent to the OP. All Request Objects from this Client MUST be rejected, if not signed
 *  with this algorithm. Request Objects are described in Section 6.1 of OpenID Connect
 *  Core 1.0 [OpenID.Core]. This algorithm MUST be used both when the Request Object is
 *  passed by value (using the request parameter) and when it is passed by reference
 *  (using the request_uri parameter). Servers SHOULD support RS256. The value none MAY
 *  be used. The default, if omitted, is that any algorithm supported by the OP and the
 *  RP MAY be used.
 *
 *   Please note: This condition does not validate if the current test actually supports the alg,
 *   it will fail later anyway
 */
public class ValidateRequestObjectSigningAlg extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");

		String alg = getRequestObjectSigningAlg();
		if(alg==null) {
			logSuccess("request_object_signing_alg is not set");
			return env;
		}
		if("none".equals(alg)) {
			logSuccess("request_object_signing_alg is 'none'");
			return env;
		}

		if(JWSUtil.isValidJWSAlgorithm(alg)) {
			logSuccess("request_object_signing_alg is one of the known algorithms",
						args("alg", alg));
			return env;
		}
		throw error("Unexpected request_object_signing_alg", args("alg", alg));
	}
}

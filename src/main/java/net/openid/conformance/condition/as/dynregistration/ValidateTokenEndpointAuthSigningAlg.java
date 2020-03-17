package net.openid.conformance.condition.as.dynregistration;

import com.nimbusds.jose.JWSAlgorithm;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 *  token_endpoint_auth_signing_alg
 *  OPTIONAL. JWS [JWS] alg algorithm [JWA] that MUST be used for signing the JWT [JWT]
 *  used to authenticate the Client at the Token Endpoint for the private_key_jwt and
 *  client_secret_jwt authentication methods. All Token Requests using these authentication
 *  methods from this Client MUST be rejected, if the JWT is not signed with this algorithm.
 *  Servers SHOULD support RS256. The value none MUST NOT be used. The default, if omitted,
 *  is that any algorithm supported by the OP and the RP MAY be used.
 *
 */
public class ValidateTokenEndpointAuthSigningAlg extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");

		String alg = getTokenEndpointAuthSigningAlg();
		if(alg == null) {
			logSuccess("token_endpoint_auth_signing_alg is not set");
			return env;
		}
		if("none".equals(alg)) {
			throw error("'none' cannot be used for client authentication");
		}

		String clientAuthType = getTokenEndpointAuthMethod();
		if("client_secret_jwt".equals(clientAuthType)) {
			JWSAlgorithm jwsAlgorithm = JWSAlgorithm.parse(alg);
			if(!JWSAlgorithm.Family.HMAC_SHA.contains(jwsAlgorithm)) {
				throw error("Invalid algorithm for client_secret_jwt", args("alg", alg));
			}
			logSuccess("token_endpoint_auth_signing_alg is valid",
						args("token_endpoint_auth_signing_alg", alg));
			return env;
		}
		if("private_key_jwt".equals(clientAuthType)) {
			JWSAlgorithm jwsAlgorithm = JWSAlgorithm.parse(alg);
			if(JWSAlgorithm.Family.EC.contains(jwsAlgorithm)
				||JWSAlgorithm.Family.ED.contains(jwsAlgorithm)
				||JWSAlgorithm.Family.RSA.contains(jwsAlgorithm)
			) {
				logSuccess("token_endpoint_auth_signing_alg is valid",
					args("token_endpoint_auth_signing_alg", alg));
				return env;
			} else {
				throw error("Invalid algorithm for private_key_jwt",
							args("token_endpoint_auth_signing_alg", alg));
			}
		}
		logSuccess("token_endpoint_auth_signing_alg is set but it is not applicable to client authentication method",
			args("token_endpoint_auth_signing_alg", alg, "token_endpoint_auth_method", clientAuthType));
		return env;
	}
}

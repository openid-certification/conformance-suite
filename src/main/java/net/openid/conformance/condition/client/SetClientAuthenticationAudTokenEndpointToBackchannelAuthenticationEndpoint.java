package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetClientAuthenticationAudTokenEndpointToBackchannelAuthenticationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client_assertion_claims", "server" })
	@PostEnvironment(required = "client_assertion_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("client_assertion_claims");

		// Note that this deliberately DOESN'T use the mtls aliased token endpoint; see
		// https://bitbucket.org/openid/mobile/issues/203/mtls-aliases-ambiguity-in-private_key_jwt
		String aud = env.getString("server", "token_endpoint");

		claims.addProperty("aud", aud);

		env.putObject("client_assertion_claims", claims);

		logSuccess("Add token_endpoint as aud value to client_assertion_claims - as per section 7.1 of CIBA, 'the OP MUST accept its Issuer Identifier, Token Endpoint URL, or Backchannel Authentication Endpoint URL as values that identify it as an intended audience'", claims);

		return env;
	}
}

package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Instant;

public class CreateClientAuthenticationAssertionClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"client", "server"})
	@PostEnvironment(required = "client_assertion_claims")
	public Environment evaluate(Environment env) {

		String issuer = env.getString("client", "client_id");

		if (Strings.isNullOrEmpty(issuer)) {
			throw error("Couldn't find required configuration element", args("issuer", issuer));
		}

		JsonObject claims = new JsonObject();

		claims.addProperty("iss", issuer);
		claims.addProperty("sub", issuer);

		// This code uses the mtls aliased token endpoint if there is one
		// This is probably not correct, according to this ticket we should always use the non-MTLS one:
		// https://bitbucket.org/openid/mobile/issues/203/mtls-aliases-ambiguity-in-private_key_jwt
		// This probably only matters in FAPI tests, as they are the only case where we need to apply the
		// mtls aliases when using private_key_jwt (due to the requirement for mtls sender constrained access tokens).
		// Arguably the MTLS aliases value is still acceptable when we are sending the assertion to the MTLS aliased
		// token endpoint, but we may want to check that the non-MATLS value is also accepted.
		String audience = env.getString("token_endpoint") != null ?
			env.getString("token_endpoint") : env.getString("server", "token_endpoint");

		if (Strings.isNullOrEmpty(audience)) {
			throw error("Couldn't find required configuration element", args("audience", audience));
		}

		claims.addProperty("aud", audience);
		claims.addProperty("jti", RandomStringUtils.secure().nextAlphanumeric(20));

		Instant iat = Instant.now();
		Instant exp = iat.plusSeconds(60);

		claims.addProperty("nbf", iat.getEpochSecond());
		claims.addProperty("iat", iat.getEpochSecond());
		claims.addProperty("exp", exp.getEpochSecond());

		logSuccess("Created client assertion claims", claims);

		env.putObject("client_assertion_claims", claims);

		return env;

	}
}

package net.openid.conformance.condition.rs;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilValidateJwtSignatureUsingOrganizationJwks extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = { "client_organization_jwks", "parsed_client_request_jwt" })
	public Environment evaluate(Environment env) {

		String jwtString = env.getString("parsed_client_request_jwt", "value");
		JsonObject orgJwks = env.getObject("client_organization_jwks");
		verifyJwsSignature(jwtString, orgJwks, "jwt", true, "client organization");

		return env;
	}
}

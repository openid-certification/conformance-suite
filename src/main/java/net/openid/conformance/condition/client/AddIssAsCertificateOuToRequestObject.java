package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddIssAsCertificateOuToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		String ou = env.getString("certificate_subject", "ou");

		requestObjectClaims.addProperty("iss", ou);

		log("Added iss value based on TLS certificate organizational unit to request object claims", args("iss", ou));

		return env;
	}
}

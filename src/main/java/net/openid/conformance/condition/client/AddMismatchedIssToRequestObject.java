package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddMismatchedIssToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"request_object_claims", "client"})
	@PostEnvironment(required = {"request_object_claims"})
	public Environment evaluate(Environment env) {
		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		String clientId = env.getString("client", "client_id");
		String mismatchedIss = clientId + "-mismatched";

		requestObjectClaims.addProperty("iss", mismatchedIss);

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added iss to request object claims that deliberately does not match client_id; " +
			"the wallet must ignore the iss claim per OID4VP section 5",
			args("iss", mismatchedIss, "client_id", clientId));

		return env;
	}

}

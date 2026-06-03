package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

public class RemoveConnectIdCiba3DSPaymentAuthorizationDetailsFromRequestObject
	extends AddConnectIdCiba3DSPaymentAuthorizationDetailsToRequestObject {

	@Override
	public Environment evaluate(Environment env) {
		super.evaluate(env);

		JsonObject requestObjectClaims = env.getObject("request_object_claims");
		requestObjectClaims.remove("authorization_details");
		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Removed authorization_details from request object claims",
			args("request_object_claims", requestObjectClaims));

		return env;
	}
}

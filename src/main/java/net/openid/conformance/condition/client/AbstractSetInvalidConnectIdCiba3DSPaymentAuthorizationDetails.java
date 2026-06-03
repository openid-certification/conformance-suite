package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractSetInvalidConnectIdCiba3DSPaymentAuthorizationDetails
	extends AddConnectIdCiba3DSPaymentAuthorizationDetailsToRequestObject {

	@Override
	public Environment evaluate(Environment env) {
		super.evaluate(env);

		JsonObject requestObjectClaims = env.getObject("request_object_claims");
		JsonObject authorizationDetails = requestObjectClaims.getAsJsonArray("authorization_details")
			.get(0)
			.getAsJsonObject();

		modifyAuthorizationDetails(requestObjectClaims, authorizationDetails);

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Made ConnectID 3DS payment authorization_details invalid",
			args("authorization_details", requestObjectClaims.get("authorization_details"),
				"request_object_claims", requestObjectClaims));

		return env;
	}

	protected abstract void modifyAuthorizationDetails(JsonObject requestObjectClaims, JsonObject authorizationDetails);
}

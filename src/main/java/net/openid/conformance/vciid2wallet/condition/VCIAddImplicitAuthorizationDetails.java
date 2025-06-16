package net.openid.conformance.vciid2wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class VCIAddImplicitAuthorizationDetails extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject richAuthorizationRequest = new JsonObject();
		JsonArray implicitAuthorizationDetails = new JsonArray();
		richAuthorizationRequest.add("rar", implicitAuthorizationDetails);
		env.putObject("rich_authorization_request", richAuthorizationRequest);

		log("Preparing implicit rich_authorization_request authorization_details", richAuthorizationRequest);

		return env;
	}
}

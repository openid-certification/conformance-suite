package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.text.RandomStringGenerator;

public class ConnectIdAddPurposeToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		Integer purposeLength = env.getInteger("requested_purpose_length");

		if (purposeLength == null) {
			purposeLength = 280; // default to a purpose of length 280
		}

		// Generator for a string containing only non-ASCII (Hangul Jano) characters to force UTF8 encoding.
		RandomStringGenerator generator = new RandomStringGenerator.Builder().withinRange(0x1100, 0x01159).get();
		String purpose= generator.generate(purposeLength);
		authorizationEndpointRequest.addProperty("purpose", purpose);

		logSuccess("Added purpose parameter to request", args("purpose length", purposeLength, "request", authorizationEndpointRequest));

		return env;

	}

}

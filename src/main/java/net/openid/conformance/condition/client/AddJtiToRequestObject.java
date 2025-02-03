package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class AddJtiToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	@PostEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {
		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		// RFC 7519 doesn't contain any restrictions on jti other than it "is a case-sensitive string",
		// so this is a very conservative choice of value
		final String jti = RandomStringUtils.secure().nextAlphanumeric(20);
		requestObjectClaims.addProperty("jti", jti);

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added jti to request object claims", args(
			"jti", jti)
		);

		return env;
	}
}

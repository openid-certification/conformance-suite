package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.common.AbstractInvalidateJwsSignature;
import net.openid.conformance.testmodule.Environment;

public class InvalidateSoftwareStatementSignature extends AbstractInvalidateJwsSignature {

	@Override
	@PreEnvironment(required = "software_statement_assertion")
	@PostEnvironment(required = "software_statement_assertion")
	public Environment evaluate(Environment env) {
		String environmentKey = "software_statement_assertion";

		String assertion = env.getString(environmentKey, "value");

		String invalidJwtString = invalidateSignatureString(environmentKey, assertion);

		JsonObject o = env.getObject(environmentKey);
		o.addProperty("value", invalidJwtString);

		log("Made the "+environmentKey+" signature invalid", args(environmentKey, invalidJwtString));

		return env;
	}

}

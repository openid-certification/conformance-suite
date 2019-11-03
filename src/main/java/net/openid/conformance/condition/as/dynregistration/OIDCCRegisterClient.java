package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.commons.lang3.RandomStringUtils;

import java.net.URI;
import java.net.URISyntaxException;

public class OIDCCRegisterClient extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request"})
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("dynamic_registration_request");

		client.addProperty("client_id", "client_" + RandomStringUtils.randomAlphanumeric(10));
		//TODO add other properties?
		env.putObject("client", client);
		logSuccess("Registered client", args("client", client));
		return env;
	}
}

package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.commons.lang3.RandomStringUtils;

public class AddRandomSuffixToIssuerInServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server", strings = {"issuer"})
	@PostEnvironment(required = "server", strings = {"issuer", "discoveryUrl"})
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		String currentIssuer = OIDFJSON.getString(server.get("issuer"));
		String newIssuer = currentIssuer + RandomStringUtils.secure().nextAlphanumeric(10);
		server.addProperty("issuer", newIssuer);
		env.putObject("server", server);
		env.putString("issuer", newIssuer);
		env.putString("discoveryUrl", newIssuer + "/.well-known/openid-configuration");


		log("Added random suffix to issuer value in server configuration", args("issuer", newIssuer));

		return env;
	}
}

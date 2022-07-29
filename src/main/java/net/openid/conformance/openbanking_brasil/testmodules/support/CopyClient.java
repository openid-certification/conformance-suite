package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CopyClient extends AbstractCondition {
	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("client").deepCopy();
		env.putObject("client_copy", client);
		logSuccess("Client was copied", args("Client", client));
		return env;
	}
}

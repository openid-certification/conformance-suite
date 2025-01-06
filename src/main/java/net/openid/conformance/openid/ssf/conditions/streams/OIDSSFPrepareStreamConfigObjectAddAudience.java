package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFPrepareStreamConfigObjectAddAudience extends AbstractOIDSSFPrepareStreamConfigObject {

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonObject streamConfig = getStreamConfig(env);

		String audience = getAudience(env);
		streamConfig.addProperty("audience", audience);

		logSuccess("Added 'audience' to stream configuration", args("config", streamConfig, "audience", audience));
		return env;
	}

	protected String getAudience(Environment env) {
		return env.getString("config", "ssf.stream.audience");
	}
}

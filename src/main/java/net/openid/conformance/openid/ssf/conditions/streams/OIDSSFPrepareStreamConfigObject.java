package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFPrepareStreamConfigObject extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject streamConfig = new JsonObject();
		streamConfig.addProperty("description", "Stream for OIDF Conformance Test-Suite");
		env.putObject("ssf","stream.config", streamConfig);

		logSuccess("Prepared stream config object", args("config", streamConfig));

		return env;
	}
}

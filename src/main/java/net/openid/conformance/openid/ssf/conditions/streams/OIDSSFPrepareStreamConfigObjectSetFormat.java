package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFPrepareStreamConfigObjectSetFormat extends AbstractOIDSSFPrepareStreamConfigObject {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject streamConfig = getStreamConfig(env);

		String format = getFormat(env);
		streamConfig.addProperty("format", format);

		logSuccess("Added 'format' to stream configuration", args("config", streamConfig, "format", format));

		return env;
	}

	protected String getFormat(Environment env) {
		return "iss_sub";
	}
}

package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractOIDSSFPrepareStreamConfigObject extends AbstractCondition {

	protected JsonObject getStreamConfig(Environment env) {
		return env.getElementFromObject("ssf", "stream.config").getAsJsonObject();
	}
}

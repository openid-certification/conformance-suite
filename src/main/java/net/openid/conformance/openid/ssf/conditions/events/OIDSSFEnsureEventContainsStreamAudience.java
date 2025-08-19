package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class OIDSSFEnsureEventContainsStreamAudience extends AbstractCondition {

	@PreEnvironment(required = {"ssf", "set_token"})
	@Override
	public Environment evaluate(Environment env) {

		JsonElement setAudienceElement = env.getElementFromObject("set_token", "claims.aud");
		if (setAudienceElement == null) {
			throw error("Could not find required 'aud' claim in verification token");
		}
		Set<String> setAudience = audienceToSet(setAudienceElement);

		JsonElement streamAudienceElement = env.getElementFromObject("ssf", "stream.aud");
		if (streamAudienceElement == null) {
			throw error("Could not find required 'aud' claim in stream configuration");
		}
		Set<String> streamAudience = audienceToSet(streamAudienceElement);

		if (!streamAudience.containsAll(setAudience)) {
			throw error("SET token audience does not match stream audience",
				args("stream_audience", streamAudience, "token_audience", setAudience));
		}

		logSuccess("SET token audience matches stream audience",
			args("stream_audience", streamAudience, "token_audience", setAudience));

		return env;
	}

	@NotNull
	private static Set<String> audienceToSet(JsonElement audienceJsonElement) {
		Set<String> audience;
		if (audienceJsonElement.isJsonArray()) {
			audience = Set.copyOf(OIDFJSON.convertJsonArrayToList(audienceJsonElement.getAsJsonArray()));
		} else {
			audience = Set.of(OIDFJSON.getString(audienceJsonElement));
		}
		return audience;
	}
}

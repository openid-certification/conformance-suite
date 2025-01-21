package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.web.client.RestTemplate;

public class FetchFreshIdTokenHintIfHintValueIsNotConfigured extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config"})
	public Environment evaluate(Environment env) {

		String obtainIdTokenUrl = env.getString("config", "client.obtain_id_token");
		String hintType = env.getString("config", "client.hint_type");
		String freshIdToken = null;
		String currentlyConfiguredHintValue = env.getString("config", "client.hint_value");
		if (Strings.isNullOrEmpty(currentlyConfiguredHintValue) && obtainIdTokenUrl != null && "id_token_hint".equals(hintType)) {
			try {
				RestTemplate restTemplate = createRestTemplate(env);
				String jsonString = restTemplate.getForObject(obtainIdTokenUrl, String.class, ImmutableMap.of());
				freshIdToken = OIDFJSON.getString(JsonParser.parseString(jsonString).getAsJsonObject().get("id_token"));
				env.putString("config", "client.hint_value", freshIdToken);
				logSuccess("Fetched a fresh id token", args("obtain_id_token", obtainIdTokenUrl, "id_token", freshIdToken));
			} catch (Exception e) {
				throw error("Failed to obtain fresh id token", e);
			}
		} else {
			logSuccess("Using configured hint_value", args("hint_type", hintType, hintType, currentlyConfiguredHintValue));
		}

		return env;
	}
}

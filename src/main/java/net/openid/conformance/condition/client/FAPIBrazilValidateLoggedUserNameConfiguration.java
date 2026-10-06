package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.BrazilConsent;

public class FAPIBrazilValidateLoggedUserNameConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		env.removeNativeValue("brazil_logged_user_name");
		JsonElement configuredName = env.getElementFromObject("config", "resource.brazilLoggedUserName");
		String name = null;
		if (configuredName != null && !configuredName.isJsonNull()) {
			if (!configuredName.isJsonPrimitive() || !configuredName.getAsJsonPrimitive().isString()) {
				throw error("'Logged-in user name' in the 'Resource' section in the test configuration must be a string");
			}
			name = OIDFJSON.getString(configuredName);
		}
		if (name == null || name.isBlank()) {
			String cnpj = env.getString("config", "resource.brazilCnpj");
			if (cnpj != null && !cnpj.isBlank()) {
				log("Corporate (PJ) CIBA consents require a logged-in user name. Set 'Logged-in user name' in the 'Resource' section in the test configuration; the request will omit it while this field is empty.");
			} else {
				logSuccess("No logged-in user name configured");
			}
			return env;
		}
		if (name.codePointCount(0, name.length()) > BrazilConsent.MAX_LOGGED_USER_NAME_LENGTH) {
			throw error("'Logged-in user name' in the 'Resource' section in the test configuration must not exceed "
				+ BrazilConsent.MAX_LOGGED_USER_NAME_LENGTH + " characters");
		}
		String cpf = env.getString("config", "resource.brazilCpf");
		if (cpf == null || cpf.isBlank()) {
			throw error("'brazilCpf' in the 'Resource' section in the test configuration is required when 'Logged-in user name' is provided");
		}
		env.putString("brazil_logged_user_name", name);
		logSuccess("Logged-in user name configuration is valid");
		return env;
	}
}

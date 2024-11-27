package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class CheckForInvalidCharsInNonce extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"nonce"})
	public Environment evaluate(Environment env) {

		List<String> invalidCharacters = new ArrayList<>();
		String       nonce = env.getString("nonce");

		if (Strings.isNullOrEmpty(nonce)) {
			throw error("nonce is empty");
		}
		// Ensure the nonce contains only URL safe characters.
		for (int i = 0; i < nonce.length(); i++) {
			String charAsString = String.valueOf(nonce.charAt(i));

			if (! charAsString.matches("[A-Za-z0-9\\-_\\.~]")) {
				if (! invalidCharacters.contains(charAsString)) {
					invalidCharacters.add(charAsString);
				}
			}
		}

		if (! invalidCharacters.isEmpty()) {
			throw error("Non URL safe characters found in nonce. This may introduce interoperability issues.",
				args("nonce", nonce, "invalid_chars", invalidCharacters));
		}

		logSuccess("Nonce contains only URL safe characters");
		return env;
	}
}

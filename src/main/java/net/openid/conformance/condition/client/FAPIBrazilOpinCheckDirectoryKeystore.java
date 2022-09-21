package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilOpinCheckDirectoryKeystore extends AbstractCondition {
	private final String BRAZIL_DIRECTORY_KEYSTORE = "https://keystore.sandbox.directory.opinbrasil.com.br/";

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		String keystore = getStringFromEnvironment(env, "config", "directory.keystore",
			"Directory Keystore base in test configuration");

		if (!keystore.equals(BRAZIL_DIRECTORY_KEYSTORE)) {
			throw error("Testing for Brazil certification must be done using the Brazil directory. If you do not have access to the directory an example client is available in the conformance suite instructions.",
				args("directory_keystore", keystore,
					"expected", BRAZIL_DIRECTORY_KEYSTORE));
		}

		logSuccess("Directory keystore matches the Brazil directory.", args("actual", keystore));

		return env;

	}

}

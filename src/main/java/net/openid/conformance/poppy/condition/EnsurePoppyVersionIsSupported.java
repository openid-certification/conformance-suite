package net.openid.conformance.poppy.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.poppy.PoppyConstants;
import net.openid.conformance.testmodule.Environment;

public class EnsurePoppyVersionIsSupported extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "poppy_version")
	public Environment evaluate(Environment env) {
		String version = env.getString("poppy_version");
		if (!PoppyConstants.PROTOCOL_VERSION.equals(version)) {
			throw error("The Poppy-Version header is not a version this Company serves, so the Company answers with version_unsupported (section 3.4). Before 1.0 only an identical major.minor is compatible.",
				args("poppy_version", version, "supported", PoppyConstants.PROTOCOL_VERSION));
		}
		logSuccess("The Poppy-Version header names a supported version", args("poppy_version", version));
		return env;
	}
}

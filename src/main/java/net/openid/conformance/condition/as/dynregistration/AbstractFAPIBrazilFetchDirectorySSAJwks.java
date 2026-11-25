package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.testmodule.Environment;

/**
 * The Brazil directories publish the software statement signing keys under the configured keystore base.
 */
public abstract class AbstractFAPIBrazilFetchDirectorySSAJwks extends AbstractFetchDirectorySSAJwks {

	@Override
	protected String getJwksUri(Environment env) {
		return env.getString("config", "directory.keystore") + getJwksPath();
	}

	protected abstract String getJwksPath();
}

package net.openid.conformance.condition.as.dynregistration;

import com.google.common.base.Strings;
import net.openid.conformance.testmodule.Environment;

/**
 * The CMF Directorio publishes the software statement signing keys at a single JWKS URI, which the tester configures.
 */
public class CMFChileFetchDirectorySSAJwks extends AbstractFetchDirectorySSAJwks {

	@Override
	protected String getJwksUri(Environment env) {
		String jwksUri = env.getString("config", "directory.ssa_jwks_uri");
		if (Strings.isNullOrEmpty(jwksUri)) {
			throw error("'Directory software statement JWKS URI' field is missing from the 'Directory' section in the test configuration");
		}
		return jwksUri;
	}
}

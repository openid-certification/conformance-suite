package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * The CMF Directorio issues the software statement from a single endpoint, which the tester configures.
 */
public class CMFChileCallDirectorySoftwareStatementEndpointWithBearerToken extends AbstractCallDirectorySoftwareStatementEndpointWithBearerToken {

	@Override
	@PreEnvironment(required = { "directory_access_token", "config" })
	@PostEnvironment(required = { "software_statement_assertion" })
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}

	@Override
	protected String getSoftwareStatementEndpoint(Environment env) {
		String endpoint = env.getString("config", "directory.software_statement_endpoint");
		if (Strings.isNullOrEmpty(endpoint)) {
			throw error("'Directory software statement endpoint' field is missing from the 'Directory' section in the test configuration");
		}
		return endpoint;
	}

}

package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * The Brazil directory serves each software statement at a URL built from the organisation and
 * software ids in the client's mTLS certificate subject.
 */
public class FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken extends AbstractCallDirectorySoftwareStatementEndpointWithBearerToken {

	@Override
	@PreEnvironment(required = { "directory_access_token", "config", "certificate_subject" })
	@PostEnvironment(required = {  "software_statement_assertion" })
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}

	@Override
	protected String getSoftwareStatementEndpoint(Environment env) {
		// https://matls-api.sandbox.directory.openbankingbrasil.org.br/organisations/${ORGID}/softwarestatements/${SSID}/assertion
		String apibase = env.getString("config", "directory.apibase");
		if (Strings.isNullOrEmpty(apibase)) {
			throw error("directory.apibase missing from test configuration");
		}
		if (!apibase.endsWith("/")) {
			apibase += "/";
		}
		String ou = env.getString("certificate_subject", "ou");
		if (Strings.isNullOrEmpty(ou)) {
			throw error("'ou' not found in TLS certificate subject");
		}
		String cn = env.getString("certificate_subject", "brazil_software_id");
		if (Strings.isNullOrEmpty(cn)) {
			throw error("'cn' not found in TLS certificate subject");
		}

		return "%sorganisations/%s/softwarestatements/%s/assertion".formatted(apibase, ou, cn);
	}

}

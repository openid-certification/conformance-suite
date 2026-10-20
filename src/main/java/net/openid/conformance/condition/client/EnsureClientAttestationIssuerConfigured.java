package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Requires the 'Client Attestation Issuer' when client attestation is the client authentication
 * method: it is the issuer of the attestations the suite mints or expects, so without it the
 * attestation cannot be built or checked. The legacy {@code vci.client_attestation_issuer} key is
 * read as a fallback for a configuration that reaches here without ConfigMigration having moved it.
 */
public class EnsureClientAttestationIssuerConfigured extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		String issuer = env.getString("config", "client_attestation.issuer");
		if (issuer == null) {
			issuer = env.getString("config", "vci.client_attestation_issuer");
		}
		// the form keeps a cleared field as ""
		if (issuer == null || issuer.isBlank()) {
			throw error("'Client Attestation Issuer' field is missing from the 'Client Attestation' section in the test"
				+ " configuration; it is required when client_attestation is the client authentication method");
		}
		logSuccess("A 'Client Attestation Issuer' is configured", args("issuer", issuer));
		return env;
	}
}

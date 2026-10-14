package net.openid.conformance.condition.client.lote;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Raises a finding when no 'LoTE signing certificate' is configured: the lists of trusted
 * entities are then trusted on the strength of the certificate they carry themselves, so a
 * substituted list would go unnoticed. Meant to be called at WARNING.
 */
public class WarnIfLoteSigningCertificateNotConfigured extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		String configuredPem = env.getString("config", LoteSlot.SIGNING_CERTIFICATE_CONFIG_KEY);
		if (configuredPem != null && !configuredPem.isBlank()) {
			logSuccess("A '" + LoteSlot.SIGNING_CERTIFICATE_FIELD_LABEL + "' is configured; the lists of trusted entities are checked against it");
			return env;
		}
		throw error("No '" + LoteSlot.SIGNING_CERTIFICATE_FIELD_LABEL + "' is set in the '" + LoteSlot.SIGNING_CERTIFICATE_SECTION_LABEL
			+ "' section of the test configuration, so the signer of each list of trusted entities is only checked"
			+ " against the certificate the list itself carries. Configure the scheme operator's sealing certificate"
			+ " to detect a substituted list.");
	}
}

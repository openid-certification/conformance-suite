package net.openid.conformance.condition.client.lote;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Raises a finding when no 'LoTE signing certificate' is configured: the lists of trusted
 * entities are then trusted on the strength of the certificate they carry themselves, so a
 * substituted list would go unnoticed. The suite fetches the lists with its usual HTTP client,
 * which does not verify the server's TLS certificate, so the configured sealing certificate is
 * the only thing that ties a fetched list to its scheme operator. Meant to be called at WARNING.
 */
public class WarnIfLoteSigningCertificateNotConfigured extends AbstractCondition {

	/** Set once this has run: the field applies to every list, so it is reported once per test, not once per list. */
	public static final String REPORTED = "lote_signing_certificate_reported";

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		env.putString(REPORTED, "true");
		if (LoteSlot.configuredSigningCertificatePem(env) != null) {
			logSuccess("A '" + LoteSlot.SIGNING_CERTIFICATE_FIELD_LABEL + "' is configured; the lists of trusted entities are checked against it");
			return env;
		}
		throw error("No '" + LoteSlot.SIGNING_CERTIFICATE_FIELD_LABEL + "' is set in the '" + LoteSlot.SIGNING_CERTIFICATE_SECTION_LABEL
			+ "' section of the test configuration, so the signer of each list of trusted entities is only checked"
			+ " against the certificate the list itself carries; the suite does not verify the TLS certificate of the"
			+ " server it fetches the lists from either. Configure the scheme operator's sealing certificate to detect"
			+ " a substituted list.");
	}
}

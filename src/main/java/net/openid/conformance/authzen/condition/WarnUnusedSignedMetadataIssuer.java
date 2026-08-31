package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Point out a 'Signed Metadata Issuer' ({@code pdp.metadata_issuer}) that the run cannot use.
 *
 * <p>The field names the party expected to attest to the discovery metadata, so it only means
 * anything when the metadata carries a {@code signed_metadata} JWT. When it does,
 * {@link ValidatePDPMetadataIssuer} checks the value and {@link ValidatePDPSignedMetadataIss}
 * compares it against the {@code iss} claim; when it does not, the field is simply ignored — and a
 * tester who filled it in was expecting signed metadata that never arrived. Saying so is the point
 * of this condition: the silence would otherwise look like success.
 *
 * <p>This is not a PDP conformance failure — {@code signed_metadata} is OPTIONAL (§9.1.3) — so the
 * caller invokes it as a WARNING.
 */
public class WarnUnusedSignedMetadataIssuer extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config"})
	public Environment evaluate(Environment env) {
		if (!SignedMetadataIssuerConfig.isConfigured(env)) {
			logSuccess("The PDP's discovery metadata contains no `signed_metadata`, and no 'Signed Metadata Issuer' "
				+ "is configured to attest to one");
			return env;
		}

		throw error("A 'Signed Metadata Issuer' is configured in the test configuration, but the PDP's discovery "
			+ "metadata contains no `signed_metadata` for it to apply to, so the field has been ignored. Either the "
			+ "PDP is not publishing the signed metadata you expected, or the field should be left blank.",
			args("metadata_issuer", SignedMetadataIssuerConfig.element(env)));
	}
}

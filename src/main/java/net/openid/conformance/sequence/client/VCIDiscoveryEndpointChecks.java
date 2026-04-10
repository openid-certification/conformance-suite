package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CheckDiscEndpointClientAttestationSigningAlgValuesSupported;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsAttestJwtClientAuth;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.vci10issuer.condition.CheckForUnexpectedParametersInAuthorizationServerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIAuthorizationServerMetadataValidation;
import net.openid.conformance.vci10issuer.condition.VCIEnsureAuthorizationDetailsTypesSupportedContainOpenIdCredentialIfScopeIsMissing;

/**
 * Shared VCI authorization server metadata checks. Reads the authorization
 * server metadata from the standard {@code server} environment key and assumes
 * {@code vci.credential_issuer_metadata} is populated.
 *
 * <p>Used by {@code VCIIssuerMetadataTest#checkAuthServerMetadata} and by
 * {@code FAPI2SPFinalDiscoveryEndpointVerification} when the selected profile
 * is VCI.
 */
public class VCIDiscoveryEndpointChecks extends AbstractConditionSequence {

	private final boolean clientAttestation;

	public VCIDiscoveryEndpointChecks(boolean clientAttestation) {
		this.clientAttestation = clientAttestation;
	}

	@Override
	public void evaluate() {
		callAndStopOnFailure(VCIAuthorizationServerMetadataValidation.class,
			ConditionResult.FAILURE, "OID4VCI-1FINAL-12.2.3", "OID4VCI-1FINAL-12.3");
		callAndContinueOnFailure(CheckForUnexpectedParametersInAuthorizationServerMetadata.class,
			ConditionResult.WARNING, "OID4VCI-1FINAL-12.2.3", "OID4VCI-1FINAL-12.3");

		if (clientAttestation) {
			callAndContinueOnFailure(EnsureServerConfigurationSupportsAttestJwtClientAuth.class,
				ConditionResult.WARNING, "OAuth2-ATCA07-13.4");
			callAndContinueOnFailure(CheckDiscEndpointClientAttestationSigningAlgValuesSupported.class,
				ConditionResult.FAILURE, "OAuth2-ATCA07-10.1");
		}

		callAndContinueOnFailure(VCIEnsureAuthorizationDetailsTypesSupportedContainOpenIdCredentialIfScopeIsMissing.class,
			ConditionResult.FAILURE, "OID4VCI-1FINAL-12.2.4-2.11.2.2");
	}
}

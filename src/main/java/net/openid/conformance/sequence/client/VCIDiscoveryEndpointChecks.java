package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.vci10issuer.condition.CheckForUnexpectedParametersInAuthorizationServerMetadata;
import net.openid.conformance.vci10issuer.condition.VCIAuthorizationServerMetadataValidation;
import net.openid.conformance.vci10issuer.condition.VCIEnsureAuthorizationDetailsTypesSupportedContainOpenIdCredentialIfScopeIsMissing;
import net.openid.conformance.vci10issuer.condition.VCIValidateAuthorizationServerLocalesSyntax;
import net.openid.conformance.vci10issuer.condition.VCIValidateAuthorizationServerScopesSupportedSyntax;

/**
 * Shared VCI authorization server metadata checks. Reads the authorization
 * server metadata from the standard {@code server} environment key and assumes
 * {@code vci.credential_issuer_metadata} is populated.
 */
public class VCIDiscoveryEndpointChecks extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(VCIAuthorizationServerMetadataValidation.class,
			ConditionResult.FAILURE, "OID4VCI-1FINAL-12.2.3", "OID4VCI-1FINAL-12.3");
		callAndContinueOnFailure(CheckForUnexpectedParametersInAuthorizationServerMetadata.class,
			ConditionResult.WARNING, "OID4VCI-1FINAL-12.2.3", "OID4VCI-1FINAL-12.3");

		callAndContinueOnFailure(VCIEnsureAuthorizationDetailsTypesSupportedContainOpenIdCredentialIfScopeIsMissing.class,
			ConditionResult.FAILURE, "OID4VCI-1FINAL-12.2.4-2.11.2.2");

		callAndContinueOnFailure(VCIValidateAuthorizationServerScopesSupportedSyntax.class,
			ConditionResult.FAILURE, "RFC6749-3.3");

		callAndContinueOnFailure(VCIValidateAuthorizationServerLocalesSyntax.class,
			ConditionResult.FAILURE, "RFC8414-2");
	}
}

package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CheckDiscEndpointAllEndpointsAreHttps;
import net.openid.conformance.condition.client.CheckDiscEndpointLocalesCanonicalCasing;
import net.openid.conformance.condition.client.CheckDiscEndpointLocalesSyntax;
import net.openid.conformance.condition.client.CheckDiscEndpointScopesSupportedSyntax;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInServerMetadata;
import net.openid.conformance.condition.client.ValidateServerMetadataAgainstSchema;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.vci10issuer.condition.VCIEnsureAuthorizationDetailsTypesSupportedContainOpenIdCredentialIfScopeIsMissing;

/**
 * Shared VCI authorization server metadata checks. Reads the authorization
 * server metadata from the standard {@code server} environment key and assumes
 * {@code vci.credential_issuer_metadata} is populated.
 */
public class VCIDiscoveryEndpointChecks extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(ValidateServerMetadataAgainstSchema.class,
			ConditionResult.FAILURE, "OID4VCI-1FINAL-12.2.3", "OID4VCI-1FINAL-12.3");
		callAndContinueOnFailure(CheckForUnexpectedParametersInServerMetadata.class,
			ConditionResult.WARNING, "OID4VCI-1FINAL-12.2.3", "OID4VCI-1FINAL-12.3");

		callAndContinueOnFailure(CheckDiscEndpointAllEndpointsAreHttps.class,
			ConditionResult.FAILURE, "RFC8414-2");

		callAndContinueOnFailure(VCIEnsureAuthorizationDetailsTypesSupportedContainOpenIdCredentialIfScopeIsMissing.class,
			ConditionResult.FAILURE, "OID4VCI-1FINAL-12.2.4-2.11.2.2");

		callAndContinueOnFailure(CheckDiscEndpointScopesSupportedSyntax.class,
			ConditionResult.FAILURE, "RFC6749-3.3");

		callAndContinueOnFailure(CheckDiscEndpointLocalesSyntax.class,
			ConditionResult.FAILURE, "RFC8414-2");

		callAndContinueOnFailure(CheckDiscEndpointLocalesCanonicalCasing.class,
			ConditionResult.WARNING, "RFC8414-2");
	}
}

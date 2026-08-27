package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddCdrXCdsClientHeadersToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddCdrXvToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIEndUserPresentFalseToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIEndUserPresentTrueToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIpV6FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsCDRAcrClaim;
import net.openid.conformance.condition.client.FAPIAuCdrCheckDiscEndpointClaimsSupported;
import net.openid.conformance.condition.client.RemoveCdrXCdsClientHeadersFromResourceEndpointRequest;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.CDRAuthorizationEndpointSetup;

import java.util.function.Supplier;

/**
 * Profile behavior for Consumer Data Right Australia, as amended by Consultation
 * Draft 210 (adoption of FAPI 2.0).
 * Requires mTLS everywhere, CDR-specific headers on resource endpoints,
 * and CDR authorization endpoint setup.
 */
public class ConsumerDataRightAuProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public Class<? extends ConditionSequence> getProfileAuthorizationEndpointSetupSteps() {
		return CDRAuthorizationEndpointSetup.class;
	}

	@Override
	public ConditionSequence addResourceEndpointProfileHeaders(boolean isSecondClient) {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				// CDR requires x-fapi-end-user-present on all authenticated resource server endpoints
				if (isSecondClient) {
					// customer-not-present call; x-cds-client-headers must not be sent
					callAndStopOnFailure(AddFAPIEndUserPresentFalseToResourceEndpointRequest.class, "CDR-http-headers");
				} else {
					callAndStopOnFailure(AddFAPIEndUserPresentTrueToResourceEndpointRequest.class, "CDR-http-headers");
					// CDR requires this header for customer present calls
					callAndStopOnFailure(AddCdrXCdsClientHeadersToResourceEndpointRequest.class, "CDR-http-headers");
					callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
					callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class,
						"CID-SP-4.2-12", "CDR-http-headers");
				}

				callAndStopOnFailure(AddCdrXvToResourceEndpointRequest.class, "CDR-http-headers");
			}
		};
	}

	@Override
	public ConditionSequence addAlternateResourceEndpointProfileHeaders() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				// exercise a customer-not-present call; x-cds-client-headers is only sent when the customer is present
				callAndStopOnFailure(AddFAPIEndUserPresentFalseToResourceEndpointRequest.class, "CDR-http-headers");
				callAndStopOnFailure(RemoveCdrXCdsClientHeadersFromResourceEndpointRequest.class, "CDR-http-headers");
				// x-fapi-customer-ip-address is no longer part of the CDR standards, but Data Holders
				// are still required to ignore it, so keep sending it
				callAndStopOnFailure(AddIpV6FapiCustomerIpAddressToResourceEndpointRequest.class, "CDR-http-headers");
			}
		};
	}

	@Override
	public Supplier<? extends ConditionSequence> getProfileSpecificDiscoveryChecks() {
		return DiscoveryEndpointChecks::new;
	}

	public static class DiscoveryEndpointChecks extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			// claims parameter support is required in Australia
			callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, ConditionResult.FAILURE, "OIDCD-3", "CID-IDA-5.1");
			callAndContinueOnFailure(FAPIAuCdrCheckDiscEndpointClaimsSupported.class, ConditionResult.FAILURE);
			callAndContinueOnFailure(CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode.class, ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureServerConfigurationSupportsCDRAcrClaim.class, ConditionResult.WARNING);
		}
	}
}

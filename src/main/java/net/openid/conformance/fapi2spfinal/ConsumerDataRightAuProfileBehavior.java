package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddCdrXCdsClientHeadersToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddCdrXvToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIpV4FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.ValidateIdTokenEncrypted;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.CDRAuthorizationEndpointSetup;

/**
 * Profile behavior for Consumer Data Right Australia.
 * Requires mTLS everywhere, CDR-specific headers on resource endpoints,
 * encrypted id_tokens, and CDR authorization endpoint setup.
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
	public ConditionSequence validateIdTokenEncryption() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(ValidateIdTokenEncrypted.class,
					ConditionResult.FAILURE, "CDR-tokens");
			}
		};
	}

	@Override
	public ConditionSequence addResourceEndpointProfileHeaders(boolean isSecondClient) {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				// CDR requires auth date for all authenticated resource server endpoints
				callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "CDR-http-headers");

				if (!isSecondClient) {
					callAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "CDR-http-headers");
					// CDR requires this header when the x-fapi-customer-ip-address header is present
					callAndStopOnFailure(AddCdrXCdsClientHeadersToResourceEndpointRequest.class, "CDR-http-headers");
					callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
					callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class,
						"CID-SP-4.2-12", "CDR-http-headers");
				}

				callAndStopOnFailure(AddCdrXvToResourceEndpointRequest.class, "CDR-http-headers");
			}
		};
	}
}

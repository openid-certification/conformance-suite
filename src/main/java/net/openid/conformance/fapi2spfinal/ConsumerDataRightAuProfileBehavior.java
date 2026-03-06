package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddCdrXCdsClientHeadersToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddCdrXvToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.ValidateIdTokenEncrypted;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.CDRAuthorizationEndpointSetup;

/**
 * Australian Consumer Data Right (CDR) profile behavior.
 */
public class ConsumerDataRightAuProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public Class<? extends ConditionSequence> profileAuthorizationEndpointSetupSteps() {
		return CDRAuthorizationEndpointSetup.class;
	}

	@Override
	public void addResourceEndpointProfileHeaders(AbstractFAPI2SPFinalServerTestModule module, boolean isSecondClient) {
		if (isSecondClient) {
			// CDR requires this header for all authenticated resource server endpoints
			module.doCallAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "CDR-http-headers");
		} else {
			module.defaultAddResourceEndpointProfileHeaders(isSecondClient);
			// CDR requires this header when the x-fapi-customer-ip-address header is present
			module.doCallAndStopOnFailure(AddCdrXCdsClientHeadersToResourceEndpointRequest.class, "CDR-http-headers");
		}
		module.doCallAndStopOnFailure(AddCdrXvToResourceEndpointRequest.class, "CDR-http-headers");
	}

	@Override
	public void validateIdTokenEncryption(AbstractFAPI2SPFinalServerTestModule module) {
		module.doCallAndContinueOnFailure(ValidateIdTokenEncrypted.class, ConditionResult.FAILURE, "CDR-tokens");
	}
}

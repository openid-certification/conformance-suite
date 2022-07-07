package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.account.v2.AccountIdentificationResponseValidatorV2;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OperationalLimitsCallResource extends AbstractConditionSequence {
	@Override
	public void evaluate() {
		callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-3");

		callAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-4");

		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-5");

		callAndStopOnFailure(CallProtectedResource.class, "FAPI1-BASE-6.2.1-1", "FAPI1-BASE-6.2.1-3");
		callAndStopOnFailure(EnsureResponseCodeWas200.class);

		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		callAndContinueOnFailure(EnsureResourceResponseReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-9", "FAPI1-BASE-6.2.1-10");

		callAndContinueOnFailure(AccountIdentificationResponseValidatorV2.class, Condition.ConditionResult.FAILURE);

	}
}

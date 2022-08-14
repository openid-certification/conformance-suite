package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.ConditionSequenceRepeater;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.resourcesAPI.v2.ResourcesResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.ResourcesApiPollingTimeout;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.TestTimedOut;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

public class ResourceApiV2PollingSteps extends AbstractConditionSequence {

	@Override
	public void evaluate() {

		call(exec().startBlock("Polling Resources API"));

		ConditionSequenceRepeater repeatSequence;
		repeatSequence = new ConditionSequenceRepeater(() -> getPreCallProtectedResourceSequence()
			.then(getPollingSequence()))
			.untilTrue("200Ok_or_differentCode_found")
			.times(4)
			.trailingPause(30)
			.onTimeout(sequenceOf(
				condition(EnsureResponseCodeWas202.class),
					condition(ResourcesApiPollingTimeout.class)));
		repeatSequence.run();
	}

	protected ConditionSequence getPreCallProtectedResourceSequence() {
		return sequenceOf(
			condition(CreateEmptyResourceEndpointRequestHeaders.class),
			condition(AddFAPIAuthDateToResourceEndpointRequest.class),
			condition(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class),
			condition(CreateRandomFAPIInteractionId.class),
			condition(AddFAPIInteractionIdToResourceEndpointRequest.class),
			condition(CallProtectedResource.class)
		);
	}

	protected ConditionSequence getPollingSequence() {
		ConditionSequence conditionSequence = sequenceOf(
			condition(VerifyResourcePollingSequenceStopCondition.class));
		return conditionSequence;
	}
}

package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.openid.conformance.ConditionSequenceRepeater;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.frontchannel.BrowserControl;
import net.openid.conformance.info.ImageService;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openbanking_brasil.resourcesAPI.v2.ResourcesResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.ResourcesApiPollingTimeout;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.TestTimedOut;
import net.openid.conformance.runner.TestExecutionManager;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class ResourceApiV2PollingSteps extends AbstractConditionSequence {

	private final TestInstanceEventLog eventLog;
	private final TestInfoService testInfo;
	private final TestExecutionManager executionManager;
	private String id;
	private Environment env;


	public ResourceApiV2PollingSteps(Environment env, String id, TestInstanceEventLog eventLog, TestInfoService testInfo, TestExecutionManager executionManager) {
		this.id = id;
		this.eventLog = eventLog;
		this.testInfo = testInfo;
		this.executionManager = executionManager;
		this.env = env;
	}

	@Override
	public void evaluate() {

		ConditionSequenceRepeater repeatSequence;
		repeatSequence = new ConditionSequenceRepeater(env, id, eventLog, testInfo, executionManager,
			() -> getPreCallProtectedResourceSequence()
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

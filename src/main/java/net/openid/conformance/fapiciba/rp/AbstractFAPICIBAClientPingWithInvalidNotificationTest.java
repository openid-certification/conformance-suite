package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;

public abstract class AbstractFAPICIBAClientPingWithInvalidNotificationTest extends AbstractFAPICIBAClientTest {

	@Override
	protected void sendPingRequestAndVerifyResponse() {
		rejectFurtherClientInteractions();
		callAndStopOnFailure(getPingNotificationCondition(), Condition.ConditionResult.FAILURE, "CIBA-10.2");
		verifyPingResponse();
	}

	protected abstract Class<? extends Condition> getPingNotificationCondition();

	protected void verifyPingResponse() {
		// The response status is not defined for every invalid-notification case.
	}

	@Override
	protected void pingRequestComplete() {
		markPingResponseValidated();
		startWaitingForTimeout();
		setStatus(Status.WAITING);
	}
}

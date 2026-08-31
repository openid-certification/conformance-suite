package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.variant.FAPICIBAProfile;

public abstract class AbstractFAPICIBAClientPingWithInvalidNotificationTest extends AbstractFAPICIBAClientTest {

	@Override
	protected void sendPingRequestAndVerifyResponse() {
		if (shouldRejectFurtherClientInteractionsWhileWaitingForTimeout()) {
			rejectFurtherClientInteractions();
		}
		callAndStopOnFailure(getPingNotificationCondition(), Condition.ConditionResult.FAILURE, "CIBA-10.2");
		verifyPingResponse();
	}

	@Override
	protected boolean shouldRejectFurtherClientInteractionsWhileWaitingForTimeout() {
		return FAPICIBAProfile.OPENBANKING_BRAZIL.equals(profile);
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

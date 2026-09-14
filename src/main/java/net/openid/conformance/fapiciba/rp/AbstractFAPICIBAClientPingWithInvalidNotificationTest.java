package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.variant.FAPICIBAProfile;

public abstract class AbstractFAPICIBAClientPingWithInvalidNotificationTest extends AbstractFAPICIBAClientTest {

	@Override
	protected void sendPingRequestAndVerifyResponse() {
		if (shouldRejectFurtherClientInteractionsWhileWaitingForTimeout()) {
			rejectFurtherClientInteractions();
		}
		callAndStopOnFailure(getPingNotificationCondition(), Condition.ConditionResult.FAILURE,
			getPingNotificationRequirements());
		verifyPingResponse();
	}

	protected String[] getPingNotificationRequirements() {
		return new String[] { "CIBA-10.2" };
	}

	@Override
	protected boolean shouldRejectFurtherClientInteractionsWhileWaitingForTimeout() {
		// CIBA Core 10.1 permits a ping-mode client to poll. OFBR 6.3.4.1 restricts polling to a
		// fallback after notification failure detection, so only the Brazil profile is shut down here.
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

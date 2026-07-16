package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-ping-duplicate-notification-test",
	displayName = "FAPI-CIBA-ID1: Client test - duplicate ping notification is handled idempotently",
	summary = "The client completes a Brazil CIBA ping flow, including token redemption and the protected resource " +
		"call. The authorization server then repeats the valid ping notification for the same auth_req_id. The client " +
		"must not redeem the auth_req_id or call the protected resource a second time.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = CIBAMode.class, values = {"poll"})
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "connectid_au"})
public class FAPICIBAClientPingDuplicateNotificationTest extends AbstractFAPICIBAClientTest {

	private static final String FIRST_RESOURCE_ENDPOINT_CALL_COMPLETE = "first_resource_endpoint_call_complete";
	private static final String DUPLICATE_PING_SCHEDULED = "duplicate_ping_scheduled";

	@Override
	protected void pingRequestComplete() {
		markPingResponseValidated();
		scheduleDuplicatePingWhenFirstFlowIsComplete();
	}

	@Override
	protected void resourceEndpointCallComplete() {
		env.putBoolean(FIRST_RESOURCE_ENDPOINT_CALL_COMPLETE, true);
		scheduleDuplicatePingWhenFirstFlowIsComplete();
	}

	private void scheduleDuplicatePingWhenFirstFlowIsComplete() {
		boolean resourceCallComplete = Boolean.TRUE.equals(env.getBoolean(FIRST_RESOURCE_ENDPOINT_CALL_COMPLETE));
		boolean duplicatePingScheduled = Boolean.TRUE.equals(env.getBoolean(DUPLICATE_PING_SCHEDULED));

		if (resourceCallComplete && clientPingResponseValidated() && !duplicatePingScheduled) {
			env.putBoolean(DUPLICATE_PING_SCHEDULED, true);
			scheduleDuplicatePing();
		}
		setStatus(Status.WAITING);
	}

	protected void scheduleDuplicatePing() {
		getTestExecutionManager().runInBackground(() -> {
			Thread.sleep(1000L);
			setStatus(Status.RUNNING);
			call(exec().startBlock("OP repeats the completed ping notification"));
			sendDuplicatePingRequestAndVerifyResponse();
			call(exec().endBlock());
			setStatus(Status.WAITING);
			return "done";
		});
	}

	protected void sendDuplicatePingRequestAndVerifyResponse() {
		rejectFurtherClientInteractions();
		callAndStopOnFailure(PingClientNotificationEndpoint.class, Condition.ConditionResult.FAILURE,
			"CIBA-10.2", "BrazilCIBA-6.2.8", "BrazilCIBA-6.3.4");
		callAndStopOnFailure(VerifyPingHttpResponseStatusCodeIsNot3XX.class, Condition.ConditionResult.FAILURE,
			"CIBA-10.2");
		callAndContinueOnFailure(VerifyPingHttpResponseStatusCodeIs204.class, Condition.ConditionResult.WARNING,
			"CIBA-10.2");
		startWaitingForTimeout();
	}
}

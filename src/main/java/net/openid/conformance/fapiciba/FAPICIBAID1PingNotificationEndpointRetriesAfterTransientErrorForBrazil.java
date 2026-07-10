package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.EnsureNotificationEndpointWasRetried;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.atomic.AtomicInteger;

@PublishTestModule(
	testName = "fapi-ciba-id1-ping-notification-endpoint-retries-after-transient-error-for-brazil",
	displayName = "FAPI-CIBA-ID1: Brazil ping notification is retried after a transient endpoint error",
	summary = "The client's notification endpoint returns HTTP 503 for the first valid ping notification. The authorization server must retry delivery, after which the endpoint returns HTTP 204 and the flow completes normally.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = CIBAMode.class, values = {"poll"})
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "connectid_au"})
public class FAPICIBAID1PingNotificationEndpointRetriesAfterTransientErrorForBrazil extends AbstractFAPICIBAID1 {

	private static final String NOTIFICATION_ENDPOINT_CALL_COUNT = "notification_endpoint_call_count";
	private final AtomicInteger notificationEndpointCallCount = new AtomicInteger();

	@Override
	protected Object handlePingCallback(JsonObject requestParts) {
		int callCount = notificationEndpointCallCount.incrementAndGet();

		if (callCount == 1) {
			setStatus(Status.RUNNING);
			verifyNotificationCallback(requestParts);
			setStatus(Status.WAITING);

			return new ResponseEntity<Object>(
				"Temporary failure from the CIBA notification endpoint.",
				HttpStatus.SERVICE_UNAVAILABLE
			);
		}

		if (callCount == 2) {
			return super.handlePingCallback(requestParts);
		}

		return new ResponseEntity<Object>("", HttpStatus.NO_CONTENT);
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {
		env.putInteger(NOTIFICATION_ENDPOINT_CALL_COUNT, notificationEndpointCallCount.get());
		callAndStopOnFailure(EnsureNotificationEndpointWasRetried.class, "BrazilCIBA-6.2.8");
		super.processNotificationCallback(requestParts);
	}
}

package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AddRequestedExp60sToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.EnsureNotificationEndpointWasRetried;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@PublishTestModule(
	testName = "fapi-ciba-id1-ping-notification-endpoint-retries-after-transient-error-for-brazil",
	displayName = "FAPI-CIBA-ID1: Brazil ping notification is retried after a transient endpoint error",
	summary = "This test requests a 60-second authentication lifetime and returns HTTP 503 for the first valid ping notification. The authorization server must retry delivery, after which the endpoint returns HTTP 204 and the flow completes normally.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = CIBAMode.class, values = {"poll"})
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "connectid_au"})
public class FAPICIBAID1PingNotificationEndpointRetriesAfterTransientErrorForBrazil extends AbstractFAPICIBAID1 {

	private static final String NOTIFICATION_ENDPOINT_CALL_COUNT = "notification_endpoint_call_count";
	private final AtomicInteger notificationEndpointCallCount = new AtomicInteger();
	private final Object notificationEndpointCallCountLock = new Object();
	private Instant authenticationRequestExpiresAt;

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();
		// Bound the no-retry verdict while leaving time for authentication and retry backoff.
		callAndStopOnFailure(AddRequestedExp60sToAuthorizationEndpointRequest.class,
			"CIBA-7.1", "BrazilCIBA-6.3.7");
	}

	@Override
	protected void performValidateAuthorizationResponse() {
		super.performValidateAuthorizationResponse();

		Integer expiresIn = env.getInteger("backchannel_authentication_endpoint_response", "expires_in");
		if (expiresIn != null) {
			authenticationRequestExpiresAt = Instant.now().plusSeconds(expiresIn);
		}
	}

	@Override
	protected Object handlePingCallback(JsonObject requestParts) {
		synchronized (notificationEndpointCallCountLock) {
			int callCount = notificationEndpointCallCount.incrementAndGet();

			if (callCount == 1) {
				setStatus(Status.RUNNING);
				verifyNotificationCallback(requestParts);
				setStatus(Status.WAITING);
				scheduleRetryAssertion();

				return new ResponseEntity<Object>(
					"Temporary failure from the CIBA notification endpoint.",
					HttpStatus.SERVICE_UNAVAILABLE
				);
			}

			if (callCount == 2) {
				return super.handlePingCallback(requestParts);
			}

			validateAdditionalPingCallback(requestParts);
			return new ResponseEntity<Object>("", HttpStatus.NO_CONTENT);
		}
	}

	private void validateAdditionalPingCallback(JsonObject requestParts) {
		// HTTP request threads do not own the test lock. Validate the duplicate on a worker only if
		// the test is still waiting; the main retry flow may already be running or finished.
		getTestExecutionManager().tryRunInBackground(() -> {
			if (!setStatusRunningIfWaiting()) {
				return "done";
			}

			verifyNotificationCallback(requestParts);
			setStatus(Status.WAITING);
			return "done";
		});
	}

	private void scheduleRetryAssertion() {
		if (authenticationRequestExpiresAt == null) {
			return;
		}

		getTestExecutionManager().scheduleInBackground(() -> {
			if (notificationEndpointCallCount.get() != 1 || !setStatusRunningIfWaiting()) {
				return "done";
			}

			int callCount;
			synchronized (notificationEndpointCallCountLock) {
				callCount = notificationEndpointCallCount.get();
			}
			if (callCount != 1) {
				setStatus(Status.WAITING);
				return "done";
			}

			env.putInteger(NOTIFICATION_ENDPOINT_CALL_COUNT, callCount);
			callAndStopOnFailure(EnsureNotificationEndpointWasRetried.class, "BrazilCIBA-6.2.8");
			return "done";
		}, secondsUntilAuthenticationRequestExpires(), TimeUnit.SECONDS);
	}

	private long secondsUntilAuthenticationRequestExpires() {
		long remainingMillis = Duration.between(Instant.now(), authenticationRequestExpiresAt).toMillis();
		return Math.max(0, (remainingMillis + 999) / 1000);
	}

	@Override
	protected void processNotificationCallback(JsonObject requestParts) {
		env.putInteger(NOTIFICATION_ENDPOINT_CALL_COUNT, notificationEndpointCallCount.get());
		callAndStopOnFailure(EnsureNotificationEndpointWasRetried.class, "BrazilCIBA-6.2.8");
		super.processNotificationCallback(requestParts);
	}
}

package net.openid.conformance.vci10wallet;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@PublishTestModule(
	testName = "oid4vci-1_0-wallet-test-credential-issuance-notification",
	displayName = "OID4VCI 1.0: Wallet Test Credential Issuance with Notification",
	summary = """
		This test case validates the credential issuance flow for a wallet using an emulated issuer, as defined in the OpenID for Verifiable Credential Issuance (OpenID4VCI) 1.0 specification.\
		It begins by emulating a Credential Issuer and an OAuth 2.0 Authorization Server. (See the 'issuer' in the exported variables).\

		It supports two flows: a) wallet-initiated flow and b) issuer-initiated flow.

		Wallet-initiated Flow:
		In the wallet-initiated flow, it expects an authorization request via Pushed Authorization Requests (PAR), followed by a token request, for which an access token is returned.\

		Issuer-initiated flow:
		In the issuer-initiated flow, the emulated issuer generates a URL (and a QR code) for a credential offer with an issuer state. That URL is expected to be visited by the wallet.\
		It expects an authorization request containing the issuer state using Pushed Authorization Requests (PAR) and a token request for which an access token is returned.\

		Once the access token has been obtained, the test expects a call to the nonce and credential endpoints. After successfully validating the credential request, a credential will be generated. \
		After the credential has been delivered the test waits for the wallet to call the notification endpoint per OID4VCI 1.0 §11. \
		Once the configured maxWaitForNotificationSeconds has elapsed the test decides its outcome: \
		if a notification was received the test completes successfully, otherwise it is marked as skipped because this variant's assertions cannot be evaluated without a notification.

		Depending on the variant configuration, we support immediate or deferred credential issuance, and plain or encrypted credential responses.

		Note that if the vci_grant_type=pre_authorization_code is used, you can use 123456 as the tx_code.
		""",
	profile = "OID4VCI-1_0"
)
public class VCIWalletTestCredentialIssuanceWithNotification extends AbstractVCIWalletTest {

	/**
	 * Set to true once the wallet has received at least one credential from either the
	 * credential endpoint (HTTP 200) or the deferred credential endpoint. Used by the
	 * grace-period timer to decide whether we ever reached the point where a notification
	 * would make sense.
	 */
	private final AtomicBoolean credentialSent = new AtomicBoolean(false);

	/**
	 * Set to true when the wallet has posted a valid notification via the notification
	 * endpoint. Read by the grace-period timer to decide between finish and skip.
	 */
	private final AtomicBoolean notificationReceived = new AtomicBoolean(false);

	/**
	 * Guards the one-shot "decide test outcome" path so that the grace-period timer only
	 * fires {@code fireTestFinished} / {@code fireTestSkipped} once, even if multiple
	 * credential-delivery events happen (e.g. a deferred flow where the initial credential
	 * endpoint call and a subsequent deferred endpoint call each trigger
	 * {@code onCredentialSent}).
	 */
	private final AtomicBoolean outcomeDecided = new AtomicBoolean(false);

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {

		notificationsSupportEnabled = true;

		super.configure(config, baseUrl, externalUrlOverride, baseMtlsUrl);
	}

	/**
	 * Instead of the default "schedule {@code fireTestFinished} after a grace period" behavior,
	 * this variant schedules a grace-period timer that — when it expires — decides the test
	 * outcome based on whether the wallet called the notification endpoint during the wait.
	 *
	 * <p>The test stays in {@link Status#WAITING} throughout the grace period so that any
	 * trailing HTTP requests from the wallet (including a late notification or a second
	 * credential endpoint call) can be handled normally. The first credential delivery wins
	 * the scheduling race; subsequent credential-delivery events are recorded but do not
	 * reschedule the timer.
	 */
	@Override
	protected void onCredentialSent() {
		// Always drop back into WAITING — credentialEndpoint set status to RUNNING at its
		// entry, and we MUST transition out before the HTTP handler returns or
		// TestDispatcher.checkLockReleased() trips the "test left as RUNNING" safety check.
		// This applies on every invocation of the hook, even the second/third call from a
		// retry or a deferred endpoint, not just the first one that schedules the timer.
		setStatus(Status.WAITING);

		if (!credentialSent.compareAndSet(false, true)) {
			eventLog.log(getName(), "Additional credential delivered to wallet; the grace-period "
				+ "timer from the first delivery is already running and will decide the test outcome.");
			return;
		}

		eventLog.log(getName(), "Credential delivered to wallet. Waiting up to "
			+ maxWaitForNotificationSeconds + " seconds for the wallet to call the notification "
			+ "endpoint before deciding the test outcome.");

		// Mirror the pattern used by AbstractVCIWalletTest.resourceEndpointCallComplete and
		// startWaitingForTimeout: sleep on a background thread, then check status on waking
		// up. Only transition to RUNNING if the test is still in WAITING — a safety net
		// against late races with concurrent HTTP handling.
		getTestExecutionManager().scheduleInBackground(() -> {
			if (outcomeDecided.compareAndSet(false, true) && getStatus() == Status.WAITING) {
				setStatus(Status.RUNNING);
				if (notificationReceived.get()) {
					eventLog.log(getName(), "Grace period expired. Notification endpoint was called; "
						+ "completing the test successfully.");
					fireTestFinished();
				} else {
					fireTestSkipped("The wallet did not call the notification endpoint within "
						+ maxWaitForNotificationSeconds + " seconds after the credential was delivered. "
						+ "This variant's assertions cannot be evaluated without a notification, so the "
						+ "test is being skipped.");
				}
			}
			return null;
		}, maxWaitForNotificationSeconds, TimeUnit.SECONDS);
	}

	/**
	 * Records that a notification arrived. Deliberately does NOT transition the test status
	 * or call {@code fireTestFinished} — the grace-period timer scheduled by
	 * {@link #onCredentialSent()} is the single decision point so that any trailing
	 * wallet requests during the grace period can still be handled normally.
	 *
	 * <p>Any of the three spec-valid event values ({@code credential_accepted},
	 * {@code credential_failure}, {@code credential_deleted}) count for this test's
	 * "wallet engaged with the notification endpoint" assertion.
	 */
	@Override
	protected void onNotificationReceived(String event, String notificationId) {
		if (notificationReceived.compareAndSet(false, true)) {
			eventLog.log(getName(),
				Map.of("event", event, "notification_id", notificationId,
					"msg",
					"Received notification event '" + event
						+ "' (notification_id=" + notificationId
						+ ") from wallet. Waiting for the grace-period timer to decide the test outcome."));
		} else {
			eventLog.log(getName(),
				Map.of("event", event, "notification_id", notificationId,
					"msg",
					"Received additional notification event '" + event
						+ "' (notification_id=" + notificationId
						+ ") from wallet (already recorded a prior notification)."));
		}
	}
}

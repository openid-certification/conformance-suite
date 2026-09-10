package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFFindingCondition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateStreamSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils.StreamSubjectOperation;
import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@PublishTestModule(
	testName = "openid-ssf-receiver-access-token-expiry",
	displayName = "OpenID Shared Signals Framework: Test Receiver Access Token Expiry Handling",
	summary = """
		This test verifies that the receiver copes with the expiry of its access token.
		The test generates a dynamic transmitter whose authorization server issues access tokens with a lifetime of only 20 seconds: the CAEP Interop Profile requires short-lived access tokens (2.7.1) and transmitters verify the expiration of the access tokens they are presented (2.7.2). The receiver obtains a token, creates a stream and requests its verification. With PUSH delivery the verification event is pushed only once the first token has expired, so the receiver's next request to the transmitter (typically deleting the stream) is made after the expiry; with POLL delivery the receiver's polls run into the expiry. After the first token expired the transmitter also delivers one ordinary event for the first subject listed in the 'SSF valid SubjectId' field.
		A request carrying an expired access token is rejected with HTTP 401 and a WWW-Authenticate challenge with error 'invalid_token' (RFC 6750 3.1). The receiver must then obtain a new access token from the authorization server and repeat the request; obtaining a new token before the old one expires is accepted as well. Presenting the expired token repeatedly without obtaining a new one fails the test.
		The testsuite expects to observe the following interactions:
		 * obtain an access token
		 * create a stream
		 * verify the stream
		 * once the first access token expired (after 20 seconds): obtain a new access token, after a 401 response or proactively, and continue with it
		 * acknowledge the event delivered after the expiry
		 * delete the stream, using the new access token
		""",
	profile = "OIDSSF"
)
@VariantNotApplicable(parameter = SsfAuthMode.class, values = "static")
public class OIDSSFReceiverAccessTokenExpiryTest extends AbstractOIDSSFReceiverTestModule {

	static final int ACCESS_TOKEN_LIFETIME_SECONDS = 20;

	/** Rejections of an expired token without a new token being obtained that fail the test. */
	private static final int EXPIRED_TOKEN_REJECTIONS_BEFORE_FAILURE = 3;

	/** How long the test keeps running for a receiver that keeps presenting its expired token. */
	private static final int FINISH_AFTER_FAILURE_SECONDS = 120;

	/** Time after the first expiry to wait for a receiver that deleted the stream early to come back. */
	private static final int POST_EXPIRY_GRACE_SECONDS = 10;

	volatile String createdStreamId;

	volatile String verificationStreamId;

	volatile String deletedStreamId;

	final AtomicInteger tokensIssued = new AtomicInteger();

	/** Expiry (epoch seconds) of the first access token issued to the receiver. */
	volatile long firstTokenExpiresAt = -1;

	/** Set by the expiry watcher once the first access token has expired. */
	volatile boolean firstTokenExpired;

	/** The receiver obtained a further token without having had an expired one rejected first. */
	volatile boolean tokenRefreshedWithoutRejection;

	/** The receiver obtained a further token after an expired one was rejected. */
	volatile boolean tokenObtainedAfterRejection;

	final AtomicInteger expiredTokenRejections = new AtomicInteger();

	volatile int tokensIssuedAtFirstRejection = -1;

	volatile long keepsPresentingExpiredTokenRecordedAt = -1;

	/** An authorized request after the first expiry, necessarily made with a newer token. */
	volatile boolean continuedWithNewToken;

	/** Push streams whose delivery is held back until the first access token expired. */
	final Set<String> heldPushStreams = ConcurrentHashMap.newKeySet();

	final AtomicBoolean postExpiryEventGenerated = new AtomicBoolean();

	volatile String postExpiryEventJti;

	volatile boolean postExpiryEventAcked;

	@Override
	public void start() {
		super.start();
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 5, TimeUnit.SECONDS);
	}

	@Override
	protected int getAccessTokenLifetimeSeconds() {
		return ACCESS_TOKEN_LIFETIME_SECONDS;
	}

	@Override
	protected boolean isFinished() {
		if (keepsPresentingExpiredTokenRecordedAt >= 0) {
			return isStreamDeleted() || now() - keepsPresentingExpiredTokenRecordedAt >= FINISH_AFTER_FAILURE_SECONDS;
		}
		if (!isStreamDeleted() || !firstTokenExpired) {
			// the expiry watcher fires even when the receiver deleted the stream early
			return false;
		}
		if (expiredTokenRejections.get() > 0 || tokenRefreshedWithoutRejection) {
			return isPostExpiryEventResolved();
		}
		// Deleted before the expiry and silent since: leave the receiver a moment to come back
		// with the expired token, then finish and grade the missing observation.
		return now() >= firstTokenExpiresAt + POST_EXPIRY_GRACE_SECONDS;
	}

	private boolean isStreamDeleted() {
		return createdStreamId != null && createdStreamId.equals(deletedStreamId);
	}

	private boolean isPostExpiryEventResolved() {
		if (!postExpiryEventGenerated.get()) {
			return true;
		}
		String jti = postExpiryEventJti;
		return jti == null || postExpiryEventAcked || getResolvedWithoutAckJtis().contains(jti);
	}

	private static long now() {
		return Instant.now().getEpochSecond();
	}

	@Override
	public void fireTestFinished() {
		gradeTokenExpiryHandling();
		super.fireTestFinished();
	}

	private void gradeTokenExpiryHandling() {
		int rejections = expiredTokenRejections.get();
		if (rejections > 0) {
			if (!tokenObtainedAfterRejection && keepsPresentingExpiredTokenRecordedAt < 0) {
				callAndContinueOnFailure(new OIDSSFFindingCondition(
						"The receiver's expired access token was rejected " + rejections + " time(s) with 401 invalid_token, but the receiver never obtained a new access token afterwards. "
							+ "After such a rejection a receiver must obtain a fresh access token from the authorization server and repeat the request."),
					Condition.ConditionResult.FAILURE, "CAEPIOP-2.7.1", "RFC6750-3.1");
			}
		} else if (!tokenRefreshedWithoutRejection) {
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"The receiver's handling of an expired access token could not be observed: it made no request after its first access token expired (" + ACCESS_TOKEN_LIFETIME_SECONDS + " s lifetime) "
						+ "and obtained no further token. Keep the stream open until the event delivered after the token expiry was acknowledged, then delete it."),
				Condition.ConditionResult.FAILURE, "CAEPIOP-2.7.1", "CAEPIOP-2.7.2");
		}

		if (!continuedWithNewToken && (tokenObtainedAfterRejection || tokenRefreshedWithoutRejection)) {
			eventLog.log(getName(), "The receiver obtained a new access token but made no authorized request after its first token expired, so the use of the new token was not observed");
		}

		String jti = postExpiryEventJti;
		if (jti != null && !postExpiryEventAcked) {
			if (getUndeliveredEventJtis().contains(jti)) {
				eventLog.log(getName(), args("msg", "The receiver deleted the stream before the event generated after the token expiry was delivered", "jti", jti));
			} else if (getUnresolvedEventJtis().contains(jti)) {
				callAndContinueOnFailure(new OIDSSFFindingCondition(
						"The receiver retrieved the event delivered after the token expiry (jti " + jti + ") but never acknowledged it before deleting the stream. "
							+ "Accepted SETs must be acknowledged via 'ack' on POLL delivery."),
					Condition.ConditionResult.FAILURE, "RFC8936-2.4");
			}
		}

		if (!isStreamDeleted()) {
			eventLog.log(getName(), "The receiver did not delete the stream; the test finished after " + FINISH_AFTER_FAILURE_SECONDS + " s.");
		}
	}

	@Override
	protected void onAccessTokenIssued(String accessToken, JsonObject tokenRecord) {
		int count = tokensIssued.incrementAndGet();
		JsonElement expiresAtEl = tokenRecord.get("expires_at");
		long expiresAt = expiresAtEl != null ? OIDFJSON.getLong(expiresAtEl) : now() + ACCESS_TOKEN_LIFETIME_SECONDS;

		if (count == 1) {
			firstTokenExpiresAt = expiresAt;
			eventLog.log(getName(), args(
				"msg", "Issued the receiver's first access token with a lifetime of " + ACCESS_TOKEN_LIFETIME_SECONDS + " s; requests presenting it after the expiry are rejected with 401",
				"expires_at", expiresAt));
			scheduleTask(() -> {
				onFirstTokenExpired();
				return "done";
			}, (int) Math.max(1, expiresAt - now() + 1), TimeUnit.SECONDS);
			return;
		}

		eventLog.log(getName(), args("msg", "Issued access token #" + count + " to the receiver", "expires_at", expiresAt));
		if (expiredTokenRejections.get() > 0) {
			if (!tokenObtainedAfterRejection) {
				tokenObtainedAfterRejection = true;
				callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Receiver obtained a new access token after its expired token was rejected with 401 invalid_token"),
					Condition.ConditionResult.FAILURE, "CAEPIOP-2.7.1", "RFC6750-3.1");
			}
		} else if (!tokenRefreshedWithoutRejection) {
			tokenRefreshedWithoutRejection = true;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Receiver obtained a new access token without waiting for the expired one to be rejected"),
				Condition.ConditionResult.FAILURE, "CAEPIOP-2.7.1");
		}
	}

	@Override
	protected void onExpiredAccessTokenRejected(String path) {
		int count = expiredTokenRejections.incrementAndGet();
		if (count == 1) {
			tokensIssuedAtFirstRejection = tokensIssued.get();
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Rejected the receiver's expired access token on '" + path + "' with 401 and a WWW-Authenticate invalid_token challenge; the receiver must now obtain a new access token and repeat the request"),
				Condition.ConditionResult.FAILURE, "CAEPIOP-2.7.2", "RFC6750-3.1");
		} else {
			eventLog.log(getName(), args("msg", "Rejected an expired access token again (rejection #" + count + ")", "path", path));
		}
		if (count >= EXPIRED_TOKEN_REJECTIONS_BEFORE_FAILURE
			&& tokensIssued.get() == tokensIssuedAtFirstRejection
			&& keepsPresentingExpiredTokenRecordedAt < 0) {
			keepsPresentingExpiredTokenRecordedAt = now();
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"The receiver presented its expired access token " + count + " times without obtaining a new one. "
						+ "After a 401 response with an invalid_token challenge a receiver must obtain a fresh access token from the authorization server and repeat the request; "
						+ "short-lived access tokens expire during normal operation."),
				Condition.ConditionResult.FAILURE, "CAEPIOP-2.7.1", "RFC6750-3.1");
		}
	}

	private void onFirstTokenExpired() {
		firstTokenExpired = true;
		eventLog.log(getName(), args("msg", "The receiver's first access token has expired", "expired_at", firstTokenExpiresAt));
		List<String> released = new ArrayList<>(heldPushStreams);
		heldPushStreams.clear();
		for (String streamId : released) {
			if (OIDSSFStreamUtils.getStreamConfig(env, streamId) != null) {
				eventLog.log(getName(), args("msg", "Releasing the push delivery held until the token expiry", "stream_id", streamId));
				super.schedulePushDelivery(streamId);
			}
		}
		generatePostExpiryEventWhenReady();
	}

	/**
	 * Push delivery is held until the first access token expired, so a push receiver's next
	 * request to the transmitter (acknowledging via 202 is not one) happens after the expiry.
	 */
	@Override
	protected void schedulePushDelivery(String streamId) {
		if (!firstTokenExpired) {
			if (heldPushStreams.add(streamId)) {
				eventLog.log(getName(), args("msg", "Holding the push delivery for the stream until the receiver's first access token expired", "stream_id", streamId));
			}
			return;
		}
		super.schedulePushDelivery(streamId);
	}

	/** Any authorized request after the first expiry proves the receiver carries a newer token. */
	private void noteAuthorizedRequest() {
		if (!continuedWithNewToken && tokensIssued.get() >= 2 && firstTokenExpiresAt > 0 && now() >= firstTokenExpiresAt) {
			continuedWithNewToken = true;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Receiver continued its requests with an access token issued after the first one expired"),
				Condition.ConditionResult.FAILURE, "CAEPIOP-2.7.1");
		}
	}

	@Override
	protected void afterStreamCreation(String streamId, JsonObject createResult, JsonElement error) {
		noteAuthorizedRequest();
		if (createResult == null || error != null || streamId == null) {
			return;
		}
		createdStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream creation for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
	}

	@Override
	protected void afterStreamLookup(String streamId, JsonObject lookupResult, JsonElement error) {
		noteAuthorizedRequest();
	}

	@Override
	protected void onStatusStatusLookup(String streamId, JsonObject statusOpResult) {
		noteAuthorizedRequest();
	}

	@Override
	protected void onStreamStatusUpdateSuccess(String streamId, JsonElement result) {
		noteAuthorizedRequest();
	}

	@Override
	protected void afterStreamUpdate(String streamId, JsonObject updateResult, JsonElement error) {
		noteAuthorizedRequest();
	}

	@Override
	protected void afterStreamReplace(String streamId, JsonObject replaceResult, JsonElement error) {
		noteAuthorizedRequest();
	}

	@Override
	protected void afterStreamSubjectChange(StreamSubjectOperation operation, String streamId, JsonObject result, JsonElement error) {
		noteAuthorizedRequest();
	}

	@Override
	protected void afterPushDeliverySuccess(String streamId, OIDSSFSecurityEvent event) {
		if (SsfEvents.isVerificationEvent(event.type()) && verificationStreamId == null) {
			verificationStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via PUSH delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
			generatePostExpiryEventWhenReady();
			return;
		}
		if (event.jti().equals(postExpiryEventJti)) {
			postExpiryEventAcked = true;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Receiver accepted the event delivered after the token expiry via PUSH delivery (jti=" + event.jti() + ")"), Condition.ConditionResult.FAILURE);
		}
	}

	@Override
	protected void onStreamEventAcknowledged(String streamId, String jti, OIDSSFSecurityEvent event) {
		noteAuthorizedRequest();
		if (SsfEvents.isVerificationEvent(event.type()) && verificationStreamId == null) {
			verificationStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via POLL delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
			scheduleAfterStreamVerification(this::generatePostExpiryEventWhenReady);
			return;
		}
		if (jti.equals(postExpiryEventJti)) {
			postExpiryEventAcked = true;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Receiver acknowledged the event delivered after the token expiry via POLL delivery (jti=" + jti + ")"), Condition.ConditionResult.FAILURE);
		}
	}

	/**
	 * Generates one ordinary event once the first token expired and the stream is verified,
	 * whichever happens last, so the receiver has an event to retrieve or acknowledge after the
	 * expiry.
	 */
	private void generatePostExpiryEventWhenReady() {
		String streamId = createdStreamId;
		if (!firstTokenExpired || streamId == null || verificationStreamId == null) {
			return;
		}
		if (!postExpiryEventGenerated.compareAndSet(false, true)) {
			return;
		}
		JsonObject streamConfig = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfig == null) {
			eventLog.log(getName(), args("msg", "The receiver deleted the stream before an event could be generated after the token expiry", "stream_id", streamId));
			return;
		}
		String eventType = selectSubjectEventType(streamConfig);
		List<JsonObject> subjects = getEventSubjects();
		if (eventType == null || subjects.isEmpty()) {
			eventLog.log(getName(), args("msg", "The stream delivers no event type that identifies a subject; no event is generated after the token expiry", "stream_id", streamId));
			return;
		}
		SsfEvent event = generateSsfEventExample(eventType, now());
		callAndContinueOnFailure(new OIDSSFGenerateStreamSET(eventStore, streamId, subjects.get(0), event, (sid, jti) -> postExpiryEventJti = jti),
			Condition.ConditionResult.WARNING, event.requirements().toArray(new String[0]));
		if (OIDSSFStreamUtils.isPushDelivery(streamConfig)) {
			schedulePushDelivery(streamId);
		}
	}

	/** The first delivered event type that is about a subject rather than about the stream itself. */
	private static String selectSubjectEventType(JsonObject streamConfig) {
		JsonElement eventsDelivered = streamConfig.get("events_delivered");
		if (eventsDelivered == null || !eventsDelivered.isJsonArray()) {
			return null;
		}
		return OIDFJSON.convertJsonArrayToList(eventsDelivered.getAsJsonArray()).stream()
			.filter(eventType -> !SsfEvents.SSF_EVENT_TYPES.contains(eventType))
			.findFirst()
			.orElse(null);
	}

	@Override
	protected void afterStreamDeletion(String streamId, JsonObject deleteResult, JsonElement error) {
		noteAuthorizedRequest();
		if (error != null || streamId == null) {
			// deletion failed (e.g. 404 for an unknown or already-deleted stream) - do not
			// record it as the successful deletion or reset previously recorded state
			return;
		}
		deletedStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream deletion for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.5");
	}
}

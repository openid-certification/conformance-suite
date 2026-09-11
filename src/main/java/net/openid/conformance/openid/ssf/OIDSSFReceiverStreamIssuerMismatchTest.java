package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFFindingCondition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.conditions.streams.AbstractOIDSSFGenerateStreamSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils.StreamSubjectOperation;
import net.openid.conformance.testmodule.PublishTestModule;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-stream-issuer-mismatch",
	displayName = "OpenID Shared Signals Framework: Test Receiver checks the issuer of a created stream",
	summary = """
		This test verifies that the receiver checks the 'iss' of the stream configuration it created.
		The test generates a dynamic transmitter and waits for a receiver to register a stream. The create response carries an 'iss' that is not the issuer the receiver obtained the transmitter configuration from.
		SSF 1.0 8.1.1.1: "The Receiver MUST check the response and confirm that the iss value matches the Issuer from which it received the Transmitter Configuration data."
		SSF defines no way for a receiver to report a rejected stream, so the test can only detect a receiver that uses the stream regardless: a receiver that requests its verification, polls it, reads its status, updates it or changes its subjects fails the test. Reading the stream configuration again is not a use. A receiver that refuses the stream sends nothing further, or deletes the stream; either passes. The test finishes once the stream is deleted, or 60 seconds after its creation.
		The testsuite expects to observe the following interactions:
		 * create a stream (the response carries a foreign 'iss')
		 * request a stream verification; a receiver that checks the 'iss' refuses, and nothing reaches the transmitter
		 * optionally delete the stream
		""",
	profile = "OIDSSF"
)
public class OIDSSFReceiverStreamIssuerMismatchTest extends AbstractOIDSSFReceiverTestModule {

	/** How long an abandoned stream is watched before the test finishes. */
	protected static final int ABANDONED_STREAM_TIMEOUT_SECONDS = 60;

	volatile String createdStreamId;

	volatile long createdAt = -1;

	volatile String deletedStreamId;

	/** Set once the receiver used the stream despite the foreign issuer; graded at that moment. */
	volatile boolean proceeded;

	@Override
	public void start() {
		super.start();
		String issuer = env.getString("ssf", "issuer");
		String foreignIssuer = (issuer != null ? issuer : "https://not-this-transmitter.example") + "/not-this-transmitter";
		env.putString("ssf", "create_response_issuer_override", foreignIssuer);
		eventLog.log(getName(), args("msg", "Stream configurations created in this test carry a foreign 'iss'", "iss", foreignIssuer));
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 5, TimeUnit.SECONDS);
	}

	@Override
	protected boolean isFinished() {
		if (createdStreamId == null) {
			return false;
		}
		if (createdStreamId.equals(deletedStreamId)) {
			return true;
		}
		return Instant.now().getEpochSecond() >= createdAt + ABANDONED_STREAM_TIMEOUT_SECONDS;
	}

	@Override
	protected void afterStreamCreation(String streamId, JsonObject createResult, JsonElement error) {
		if (createResult == null || error != null || streamId == null) {
			return;
		}
		createdStreamId = streamId;
		createdAt = Instant.now().getEpochSecond();
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream creation for stream_id=" + streamId + "; the response carries a foreign 'iss'"),
			Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
	}

	@Override
	protected void afterStreamDeletion(String streamId, JsonObject deleteResult, JsonElement error) {
		if (error != null || streamId == null) {
			return;
		}
		deletedStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream deletion for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.5");
	}

	@Override
	protected void afterStreamLookup(String streamId, JsonObject lookupResult, JsonElement error) {
		eventLog.log(getName(), args("msg", "The receiver read the stream configuration again; a re-read is not a use of the stream", "stream_id", streamId));
	}

	@Override
	protected AbstractOIDSSFGenerateStreamSET createVerificationSetGenerator(String streamId) {
		gradeProceeded("requested a verification event for");
		return super.createVerificationSetGenerator(streamId);
	}

	@Override
	protected ResponseEntity<?> handleStreamPollingRequest(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		gradeProceeded("polled");
		return super.handleStreamPollingRequest(path, req, res, session, requestParts);
	}

	@Override
	protected void onStatusStatusLookup(String streamId, JsonObject statusOpResult) {
		gradeProceeded("read the status of");
	}

	@Override
	protected void onStreamStatusUpdateSuccess(String streamId, JsonElement result) {
		gradeProceeded("updated the status of");
	}

	@Override
	protected void afterStreamUpdate(String streamId, JsonObject updateResult, JsonElement error) {
		gradeProceeded("updated");
	}

	@Override
	protected void afterStreamReplace(String streamId, JsonObject replaceResult, JsonElement error) {
		gradeProceeded("replaced");
	}

	@Override
	protected void afterStreamSubjectChange(StreamSubjectOperation operation, String streamId, JsonObject result, JsonElement error) {
		gradeProceeded("changed the subjects of");
	}

	private void gradeProceeded(String howUsed) {
		if (proceeded) {
			return;
		}
		proceeded = true;
		callAndContinueOnFailure(new OIDSSFFindingCondition(
				"The receiver " + howUsed + " a stream whose 'iss' is not the issuer it obtained the transmitter configuration from. "
					+ "A receiver must check the 'iss' of the created stream configuration against the transmitter's issuer and must not use a stream that fails that check."),
			Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
	}

	@Override
	public void fireTestFinished() {
		if (createdStreamId != null && !proceeded) {
			// SSF gives the receiver no way to report a rejected stream, so not using it is all
			// the suite can observe; the check itself stays unobservable
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("The receiver did not use the stream whose 'iss' does not match the transmitter's issuer"
					+ (createdStreamId.equals(deletedStreamId)
						? " and deleted it"
						: " within " + ABANDONED_STREAM_TIMEOUT_SECONDS + " seconds of its creation. The check itself is not observable; only a receiver that uses such a stream can fail this test")),
				Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
		}
		super.fireTestFinished();
	}
}

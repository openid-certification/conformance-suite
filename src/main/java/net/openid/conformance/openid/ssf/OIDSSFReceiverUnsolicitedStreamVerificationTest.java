package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

@PublishTestModule(
	testName = "openid-ssf-receiver-unsolicited-stream-verification",
	displayName = "OpenID Shared Signals Framework: Test Receiver Transmitter-Initiated Stream Verification",
	summary = """
		This test verifies that the receiver correctly handles a transmitter-initiated
		(aka 'unsolicited') stream verification event — one that carries no 'state' claim
		and is delivered by the transmitter without a prior verification request from the receiver.

		The test generates a dynamic transmitter and waits for a receiver to register a stream.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * receive a transmitter-initiated verification event (delivered immediately by the
		   transmitter after stream creation, with no 'state' claim)
		 * acknowledge the verification event (202 Accepted on PUSH per RFC 8935 2.2,
		   or explicit ack on POLL)
		 * delete the stream

		Success requires the receiver to have processed specifically the stateless
		verification event delivered by the transmitter. A solicited verification event
		triggered by the receiver calling /verify does not satisfy this test — its
		acknowledgment is ignored and the test will time out if no stateless event is
		processed.

		See SSF 1.0 8.1.4 the 'state' member is optional in a stream verification event,
		and a transmitter MAY deliver a verification event at any time after stream creation.
		""",
	profile = "OIDSSF"
)
public class OIDSSFReceiverUnsolicitedStreamVerificationTest extends OIDSSFReceiverStreamVerificationTest {

	@Override
	protected boolean shouldDeliverUnsolicitedStreamVerificationAfterStreamCreation() {
		return true;
	}

	@Override
	protected void afterStreamVerification(String streamId, OIDSSFSecurityEvent verificationEvent) {
		// Per SSF 1.0 §8.1.4.2, a transmitter-initiated (unsolicited) verification
		// event MUST NOT carry a 'state' claim. This test specifically exercises
		// that path, so we only record success when the receiver acknowledged the
		// stateless event we pushed at stream creation — not a solicited response
		// the receiver may have also received by calling /verify.
		if (verificationEventHasStateClaim(verificationEvent)) {
			eventLog.log(getName(), args(
				"msg", "Ignoring solicited verification event — this test requires the "
					+ "transmitter-initiated (stateless) verification event delivered at stream creation",
				"stream_id", streamId,
				"jti", verificationEvent.jti()));
			return;
		}

		super.afterStreamVerification(streamId, verificationEvent);
	}

	protected boolean verificationEventHasStateClaim(OIDSSFSecurityEvent verificationEvent) {
		try {
			JsonObject tokenJson = JWTUtil.jwtStringToJsonObjectForEnvironment(verificationEvent.securityEventToken());
			JsonObject claims = tokenJson.getAsJsonObject("claims");
			if (claims == null) {
				return false;
			}
			JsonObject events = claims.getAsJsonObject("events");
			if (events == null) {
				return false;
			}
			JsonElement verificationEl = events.get(SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE);
			if (verificationEl == null || !verificationEl.isJsonObject()) {
				return false;
			}
			return verificationEl.getAsJsonObject().has("state");
		} catch (ParseException e) {
			return false;
		}
	}
}

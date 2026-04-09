package net.openid.conformance.openid.ssf;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-receiver-unsolicited-stream-verification",
	displayName = "OpenID Shared Signals Framework: Test Receiver Unsolicited Stream Verification",
	summary = """
		This test verifies that the receiver correctly handles an unsolicited stream
		verification event — one that carries no 'state' claim and is delivered by the
		transmitter without a prior verification request from the receiver.

		The test generates a dynamic transmitter and waits for a receiver to register a stream.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * receive an unsolicited verification event (delivered immediately by the transmitter
		   after stream creation, with no 'state' claim)
		 * acknowledge the verification event (202 Accepted on PUSH per RFC 8935 §2.2,
		   or explicit ack on POLL)
		 * delete the stream

		See SSF 1.0 §8.1.4 — the 'state' member is optional in a stream verification event,
		and a transmitter MAY deliver a verification event at any time after stream creation.
		""",
	profile = "OIDSSF"
)
public class OIDSSFReceiverUnsolicitedStreamVerificationTest extends OIDSSFReceiverStreamVerificationTest {

	@Override
	protected boolean shouldDeliverUnsolicitedStreamVerificationAfterStreamCreation() {
		return true;
	}
}

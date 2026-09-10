package net.openid.conformance.openid.ssf;

import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCallPollEndpoint;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "openid-ssf-transmitter-stream-verification-long-poll",
	displayName = "OpenID Shared Signals Framework: Stream Verification via long poll (returnImmediately=false)",
	summary = """
		This test verifies stream verification via a long poll. RFC 8936 2.5: "If no SETs are
		available at the time of the request, the SET Transmitter SHALL delay responding until a SET
		is available or the timeout interval has elapsed unless the poll request parameter
		'returnImmediately' is present with the value 'true'."
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * trigger a verification event
		 * retrieve the verification event with poll requests carrying "returnImmediately": false
		 * validate the verification event

		A transmitter that rejects "returnImmediately": false, that holds the request beyond the
		test suite's HTTP timeout or that keeps answering with an empty 'sets' object fails this test.
		Transmitter-initiated verification events (without 'state') are accepted per SSF 1.0 8.1.4;
		the test succeeds once a verification event carrying the expected 'state' is found and validated.
		""",
	profile = "OIDSSF"
)
@VariantNotApplicable(parameter = SsfDeliveryMode.class, values = "push")
public class OIDSSFTransmitterStreamVerificationLongPollTest extends AbstractOIDSSFTransmitterStreamVerificationTest {

	@Override
	protected void performVerification() {
		// every poll of this module is a long poll; the transmitter decides the timeout interval
		env.putString("ssf", "poll.return_immediately", "false");

		if (!pollForSolicitedVerificationEvent("LONG_POLL", OIDSSFCallPollEndpoint.PollMode.POLL_ONLY)) {
			Integer pollStatus = env.getInteger("resource_endpoint_response_full", "status");
			if (pollStatus != null && pollStatus != 200) {
				throw new TestFailureException(getId(),
					"The transmitter rejected the long poll request (\"returnImmediately\": false) with HTTP " + pollStatus
						+ ". RFC 8936 2.2 defines returnImmediately as OPTIONAL with the default false, and 2.5 requires the "
						+ "transmitter to hold such a request until a SET is available or its timeout interval elapses; "
						+ "a transmitter that only supports short polling does not conform to RFC 8936.");
			}
			throw new TestFailureException(getId(),
				"Long poll requests (\"returnImmediately\": false) did not return a solicited verification event "
					+ "(with matching 'state') within the polling window. RFC 8936 2.5 requires the transmitter to hold "
					+ "the request until a SET is available or its timeout interval elapses, and SSF 1.0 8.1.4.2 requires "
					+ "it to deliver the verification event.");
		}
	}
}

package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFFindingCondition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCallPollEndpoint;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractReceivedSETs;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.VariantNotApplicable;

import java.util.LinkedHashSet;
import java.util.Set;

@PublishTestModule(
	testName = "openid-ssf-transmitter-stream-verification-set-errs",
	displayName = "OpenID Shared Signals Framework: Report a retrieved SET via setErrs",
	summary = """
		This test verifies that the transmitter's poll endpoint accepts an error report for a delivered SET.
		RFC 8936 2.4.4 lets a receiver "use the setErrs member to communicate the errors in the following poll
		request", and 2.6 requires such a request to carry a Content-Language header; the transmitter answers
		it like any poll request (2.5).
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * trigger a verification event
		 * retrieve the verification event via POLL_ONLY (without acknowledging) and validate it
		 * report the retrieved SET(s) via 'setErrs' in a request with 'maxEvents' 0 and a Content-Language header,
		   and validate the response (200 with an empty 'sets' object)
		 * poll once more and note whether the reported SET is offered again (RFC 8936 does not say whether a
		   SET reported via setErrs may be delivered again, so this is only logged)
		""",
	profile = "OIDSSF"
)
@VariantNotApplicable(parameter = SsfDeliveryMode.class, values = "push")
public class OIDSSFTransmitterStreamVerificationSetErrsTest extends AbstractOIDSSFTransmitterStreamVerificationTest {

	@Override
	protected void performVerification() {
		if (!pollForSolicitedVerificationEvent("POLL_ONLY", OIDSSFCallPollEndpoint.PollMode.POLL_ONLY)) {
			throw new TestFailureException(getId(),
				"Poll responses did not contain a solicited verification event (with matching 'state') within the polling window");
		}

		Set<String> reportedJtis = new LinkedHashSet<>(env.getElementFromObject("ssf", "poll.sets").getAsJsonObject().keySet());

		eventLog.runBlock("Report the retrieved SET(s) via setErrs", () -> {
			env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.REPORT_ERRORS.name());
			callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "RFC8936-2.4.4", "RFC8936-2.6");
			env.mapKey("ssf_polling_response", "resource_endpoint_response_full");
			validatePollResponse();
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("The transmitter accepted the error report for " + reportedJtis.size() + " SET(s): " + reportedJtis),
				Condition.ConditionResult.FAILURE, "RFC8936-2.4.4");
		});

		eventLog.runBlock("Poll once more after the error report", () -> {
			env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.POLL_ONLY.name());
			callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "OIDSSF-6.1.2", "RFC8936-2.4");
			env.mapKey("ssf_polling_response", "resource_endpoint_response_full");
			validatePollResponse();
			callAndStopOnFailure(OIDSSFExtractReceivedSETs.class);

			Set<String> offeredAgain = new LinkedHashSet<>(env.getElementFromObject("ssf", "poll.sets").getAsJsonObject().keySet());
			offeredAgain.retainAll(reportedJtis);
			if (offeredAgain.isEmpty()) {
				callAndContinueOnFailure(new OIDSSFLogSuccessCondition("The SET(s) reported via setErrs were not offered again"),
					Condition.ConditionResult.INFO, "RFC8936-2.4.4");
			} else {
				callAndContinueOnFailure(new OIDSSFFindingCondition("The transmitter offered SET(s) again that the receiver had reported via setErrs: " + offeredAgain
						+ ". RFC 8936 does not say whether a reported SET may be delivered again; noted for information."),
					Condition.ConditionResult.INFO, "RFC8936-2.4.4");
			}
			// SETs still offered are acknowledged so the stream is left clean
			if (!offeredAgain.isEmpty()) {
				env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.ACKNOWLEDGE_ONLY.name());
				callAndContinueOnFailure(OIDSSFCallPollEndpoint.class, Condition.ConditionResult.INFO, "RFC8936-2.4.2");
			}
		});
	}
}

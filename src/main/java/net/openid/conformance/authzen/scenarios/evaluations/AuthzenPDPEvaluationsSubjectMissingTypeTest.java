package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-subject-missing-type",
	displayName = "AuthZEN Evaluations API - Section 5.1: Top-level subject default missing type -- expect HTTP 400",
	summary = "Section 5.1 missing required sub-field in a batch (certification scenario 3.1 and 5.4.2). The top-level `subject` default has no `type`, and the only evaluation names only its resource, so it inherits the incomplete subject. Section 10.1.1 requires a 'Bad Request' when a required attribute is omitted. The AuthZEN working group settled this in openid/authzen#763: an evaluation that does not resolve to a complete, well-formed subject, action and resource makes the whole request malformed, under every evaluation semantic and even when the other evaluations are complete (revised certification scenario 6.4.1 and 6.4.4). The false decision Section 7.1.2.1 allows under execute_all is for an evaluation the PDP attempts on a well-formed request and cannot complete, which the fixture gives the harness no way to provoke. The PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationsSubjectMissingTypeTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsSubjectMissingTypeTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "id": "alice" },
			"action": { "name": "read" },
			"evaluations": [
				{ "resource": { "type": "record", "id": "record-1" } }
			]
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected boolean sendRawRequest() {
		return true;
	}

	@Override
	protected int getExpectedHttpStatusCode() {
		return 400;
	}

	@Override
	protected String[] additionalStatusRequirements() {
		// Section 5.1: a Subject has two REQUIRED keys, type and id; Section 7.1.1: the defaults evaluations inherit.
		// Section 7.2.1: an error that pertains to the entire payload is a 4XX.
		return new String[] {"AUTHZEN-5.1", "AUTHZEN-7.1.1", "AUTHZEN-7.2.1"};
	}
}

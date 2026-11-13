package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-evaluation-is-string",
	displayName = "AuthZEN Evaluations API - Section 7.1: Evaluation item is a string -- expect HTTP 400",
	summary = "Section 7.1 invalid field type in a batch (certification scenario 3.1 and 5.4.6). The only item of `evaluations` is a string instead of an object, although the top level supplies a complete subject, action and resource. Section 7.1 makes each item an object. The AuthZEN working group settled this in openid/authzen#763: an evaluation that does not resolve to a complete, well-formed subject, action and resource makes the whole request malformed, under every evaluation semantic and even when the other evaluations are complete (revised certification scenario 6.4.1 and 6.4.4). The false decision Section 7.1.2.1 allows under execute_all is for an evaluation the PDP attempts on a well-formed request and cannot complete, which the fixture gives the harness no way to provoke. The PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationsEvaluationIsStringTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsEvaluationIsStringTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" },
			"evaluations": [
				"record-1"
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
		// Section 7.1: "a list of objects, each typed as the object as defined in the Access Evaluation Request".
		// Section 7.2.1: an error that pertains to the entire payload is a 4XX.
		return new String[] {"AUTHZEN-7.1", "AUTHZEN-7.2.1"};
	}
}

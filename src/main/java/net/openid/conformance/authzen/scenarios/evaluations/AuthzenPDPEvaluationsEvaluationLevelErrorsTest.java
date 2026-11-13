package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-evaluation-level-errors",
	displayName = "AuthZEN Evaluations API - Sections 7.1.1 and 10.1.1: Evaluation with no resource at any level -- expect HTTP 400",
	summary = "Section 7.1.1 evaluation-level error under the execute_all semantic, which the request sets explicitly. The second evaluation is empty and there is no top-level resource for it to inherit. Section 7.1.1 requires a key omitted from an evaluation to be provided at the top level, and Section 10.1.1 requires a 'Bad Request' when a required attribute is omitted. "
		+ "The AuthZEN working group settled this in openid/authzen#763: an evaluation that does not resolve to a complete, well-formed subject, action and resource makes the whole request malformed, under every evaluation semantic and even when the other evaluations are complete (revised certification scenario 6.4.1 and 6.4.4). The false decision Section 7.1.2.1 allows under execute_all is for an evaluation the PDP attempts on a well-formed request and cannot complete, which the fixture gives the harness no way to provoke. The PDP MUST return HTTP 400; HTTP 200 with the second evaluation denoted by a false decision fails.\n" + AuthzenPDPEvaluationsEvaluationLevelErrorsTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsEvaluationLevelErrorsTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"options": { "evaluations_semantic": "execute_all" },
			"evaluations": [
				{ "resource": { "type": "record", "id": "record-1" } },
				{}
			]
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected int getExpectedHttpStatusCode() {
		return 400;
	}

	@Override
	protected String[] additionalStatusRequirements() {
		// Section 7.1.1: a key omitted from an evaluation "MUST be provided as a top-level key".
		// Section 7.2.1: an error that pertains to the entire payload is a 4XX.
		return new String[] {"AUTHZEN-7.1.1", "AUTHZEN-7.2.1"};
	}
}

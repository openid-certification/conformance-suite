package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-incomplete-entity",
	displayName = "AuthZEN Evaluations API - Sections 7.1.1 and 10.1.1: Evaluation with an incomplete resource -- expect HTTP 400",
	summary = "Section 7.1.1 evaluation-level error under the execute_all semantic, which the request sets explicitly. The top level supplies a complete subject, action and resource, so the first evaluation, which is empty, inherits all three and is valid. The second supplies a `resource` that replaces the top-level default and has no `id`, so a required attribute is omitted, and Section 10.1.1 requires a 'Bad Request' when a required attribute is omitted. "
		+ "The AuthZEN working group settled this in openid/authzen#763: an evaluation that does not resolve to a complete, well-formed subject, action and resource makes the whole request malformed, under every evaluation semantic and even when the other evaluations are complete (revised certification scenario 6.4.1, whose payload this is). "
		+ "The PDP MUST return HTTP 400; HTTP 200 with the second evaluation denoted by a false decision fails.\n" + AuthzenPDPEvaluationsIncompleteEntityTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsIncompleteEntityTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" },
			"options": { "evaluations_semantic": "execute_all" },
			"evaluations": [
				{},
				{ "resource": { "type": "record" } }
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
	protected boolean sendRawRequest() {
		return true;
	}

	@Override
	protected String[] additionalStatusRequirements() {
		// Section 5.2: a Resource has two REQUIRED keys, type and id; Section 7.1.1: an evaluation's
		// resource replaces the top-level default.
		return new String[] {"AUTHZEN-5.2", "AUTHZEN-7.1.1"};
	}
}

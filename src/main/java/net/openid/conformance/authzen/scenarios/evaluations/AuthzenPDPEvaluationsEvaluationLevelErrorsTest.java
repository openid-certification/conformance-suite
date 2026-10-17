package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.Set;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-evaluation-level-errors",
	displayName = "AuthZEN Evaluations API - Sections 7.1.2.1 and 7.2.1: Evaluation-level errors",
	summary = "Section 7.1.2.1 evaluation-level errors under the execute_all semantic, which the request sets explicitly (certification scenario 6.4.1). The second evaluation is empty and there is no top-level resource for it to inherit. Certification scenario 6.4.1 expects HTTP 200 with the second evaluation returned as a decision-false item: Section 7.2.1 handles an error in an individual evaluation in the payload, where the decision defaults to closed (false) and the context may describe the error. Section 7.1 only allows the top-level resource to be omitted when every evaluation carries one, and Section 10.1.1 requires a 'Bad Request' when a required attribute is omitted, so HTTP 400 is also accepted. The PDP MUST NOT permit the second evaluation.\n" + AuthzenPDPEvaluationsEvaluationLevelErrorsTest.payload,
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
	protected Set<Integer> getAcceptableHttpStatusCodes() {
		return Set.of(200, 400);
	}

	@Override
	protected String getExpectedEvaluationsResponseJson() {
		return """
			{
				"evaluations": [
					{ "decision": true },
					{ "decision": false }
				]
			}
			""";
	}

	@Override
	protected String[] additionalStatusRequirements() {
		// Section 7.1.1: a key omitted from an evaluation "MUST be provided as a top-level key".
		// Section 7.2.1: an error that pertains to the entire payload is a 4XX.
		return new String[] {"AUTHZEN-7.1.1", "AUTHZEN-7.2.1"};
	}

	@Override
	protected String[] additionalDecisionsRequirements() {
		// Section 7.1.2.1: under execute_all "any failure can be denoted by "decision": false";
		// Section 7.2.1: the decision for an evaluation in error defaults to closed (false).
		return new String[] {"AUTHZEN-7.1.2.1", "AUTHZEN-7.2.1"};
	}
}

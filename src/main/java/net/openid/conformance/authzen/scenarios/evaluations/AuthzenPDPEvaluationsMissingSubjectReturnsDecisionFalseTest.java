package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-missing-subject-returns-decision-false",
	displayName = "Authzen Evaluations API - Certification Profile 3.4.1: Missing subject returns decision false",
	summary = "Per certification profile 3.4.1, a single evaluation failure in a batch MUST be reported as a decision-false entry in the response array with HTTP 200, not as a top-level error. The lone evaluation here cannot resolve a subject (none top-level, none per-evaluation), so the PDP MUST return HTTP 200 with `{ \"evaluations\": [{ \"decision\": false }] }`.\n" + AuthzenPDPEvaluationsMissingSubjectReturnsDecisionFalseTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationsMissingSubjectReturnsDecisionFalseTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
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
	protected String getExpectedEvaluationsResponseJson() {
		return """
			{
				"evaluations": [
					{ "decision": false }
				]
			}
			""";
	}
}

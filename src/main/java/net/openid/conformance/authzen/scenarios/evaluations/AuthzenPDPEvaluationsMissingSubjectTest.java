package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.Set;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-missing-subject",
	displayName = "AuthZEN Evaluations API - Section 7.1: Batch with no subject at any level",
	summary = "Section 7.1 batch in which neither the top level nor the only evaluation carries a subject, so the evaluation has no subject to use. Section 7.1 allows the top-level subject to be omitted only when every evaluation carries one, and Section 10.1.1 requires a 'Bad Request' when a required attribute is omitted. Section 7.1.2.1 also lets a failed evaluation be denoted by a false decision. The PDP MUST either reject the request with HTTP 400, or return HTTP 200 with `{ \"evaluations\": [{ \"decision\": false }] }`; it MUST NOT permit the evaluation.\n" + AuthzenPDPEvaluationsMissingSubjectTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsMissingSubjectTest extends AbstractAuthzenPDPEvaluationsTest {

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
	protected Set<Integer> getAcceptableHttpStatusCodes() {
		return Set.of(200, 400);
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

	@Override
	protected String[] additionalStatusRequirements() {
		// Section 7.1.1: a key omitted from an evaluation "MUST be provided as a top-level key".
		return new String[] {"AUTHZEN-7.1.1"};
	}
}

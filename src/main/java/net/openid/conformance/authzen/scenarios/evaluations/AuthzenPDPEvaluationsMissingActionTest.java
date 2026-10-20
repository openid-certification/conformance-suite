package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.Set;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-missing-action",
	displayName = "AuthZEN Evaluations API - Section 7.1: Batch with no action at any level",
	summary = "Section 7.1 batch in which neither the top level nor the only evaluation carries an action, so the evaluation has no action to use (certification scenario 3.1 with 5.4.1). Section 7.1.1 requires a key omitted from an evaluation to be provided at the top level, and Section 10.1.1 requires a 'Bad Request' when a required attribute is omitted. Section 7.1.2.1 also lets a failed evaluation be denoted by a false decision, and Section 7.2.1 handles an error in an individual evaluation in the payload, where the decision defaults to closed (false). As for a batch with no subject, the PDP MUST either reject the request with HTTP 400, or return HTTP 200 with `{ \"evaluations\": [{ \"decision\": false }] }`; it MUST NOT permit the evaluation.\n" + AuthzenPDPEvaluationsMissingActionTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsMissingActionTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
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

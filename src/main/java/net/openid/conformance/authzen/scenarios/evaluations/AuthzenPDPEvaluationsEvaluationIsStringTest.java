package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.Set;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-evaluation-is-string",
	displayName = "AuthZEN Evaluations API - Section 7.1: Evaluation item is a string",
	summary = "Section 7.1 invalid field type in a batch (certification scenario 3.1 and 5.4.6). The only item of `evaluations` is a string instead of an object, although the top level supplies a complete subject, action and resource. Section 7.1 makes each item an object, and Section 7.2.1 lets an error in an individual evaluation be returned in the payload with the decision defaulting to closed (false). Whether a malformed item is a whole-payload error is a question for the working group, so the PDP MUST either reject the request with HTTP 400, or return HTTP 200 with `{ \"evaluations\": [{ \"decision\": false }] }`; it MUST NOT permit the evaluation.\n" + AuthzenPDPEvaluationsEvaluationIsStringTest.payload,
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
		// Section 7.1: "a list of objects, each typed as the object as defined in the Access Evaluation Request".
		// Section 7.2.1: an error that pertains to the entire payload is a 4XX.
		return new String[] {"AUTHZEN-7.1", "AUTHZEN-7.2.1"};
	}

	@Override
	protected String[] additionalDecisionsRequirements() {
		// Section 7.1.2.1: under execute_all "any failure can be denoted by "decision": false";
		// Section 7.2.1: the decision for an evaluation in error defaults to closed (false).
		return new String[] {"AUTHZEN-7.1.2.1", "AUTHZEN-7.2.1"};
	}
}

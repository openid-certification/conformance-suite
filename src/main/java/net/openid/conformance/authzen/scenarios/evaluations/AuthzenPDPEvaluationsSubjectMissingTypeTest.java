package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.Set;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-subject-missing-type",
	displayName = "AuthZEN Evaluations API - Section 5.1: Top-level subject default missing type",
	summary = "Section 5.1 missing required sub-field in a batch (certification scenario 3.1 and 5.4.2). The top-level `subject` default has no `type`, and the only evaluation names only its resource, so it inherits the incomplete subject. "
		+ "The default is an object that lacks or mistypes a member, an error each evaluation can fail on alone; a default that is not an object at all (such as a string subject or context) makes the whole payload malformed and gets HTTP 400 only. Section 10.1.1 requires a 'Bad Request' when a required attribute is omitted, and Section 7.2.1 lets an error in an individual evaluation be returned in the payload with the decision defaulting to closed (false). Which of the two applies to a default every evaluation inherits is a question for the working group, so the PDP MUST either reject the request with HTTP 400, or return HTTP 200 with `{ \"evaluations\": [{ \"decision\": false }] }`; it MUST NOT permit the evaluation.\n" + AuthzenPDPEvaluationsSubjectMissingTypeTest.payload,
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
		// Section 5.1: a Subject has two REQUIRED keys, type and id; Section 7.1.1: the defaults evaluations inherit.
		// Section 7.2.1: an error that pertains to the entire payload is a 4XX.
		return new String[] {"AUTHZEN-5.1", "AUTHZEN-7.1.1", "AUTHZEN-7.2.1"};
	}

	@Override
	protected String[] additionalDecisionsRequirements() {
		// Section 7.1.2.1: under execute_all "any failure can be denoted by "decision": false";
		// Section 7.2.1: the decision for an evaluation in error defaults to closed (false).
		return new String[] {"AUTHZEN-7.1.2.1", "AUTHZEN-7.2.1"};
	}
}

package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.Set;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-action-name-is-number",
	displayName = "AuthZEN Evaluations API - Section 5.3: Top-level action default name is a number",
	summary = "Section 5.3 invalid field type in a batch (certification scenario 3.1 and 5.4.6). The top-level `action` default has a `name` that is a number instead of a string, and the only evaluation names only its resource, so it inherits the malformed action. As for a top-level subject without a type, whether an error in a default every evaluation inherits is a whole-payload error or one in the individual evaluation (Section 7.2.1) is a question for the working group, so the PDP MUST either reject the request with HTTP 400, or return HTTP 200 with `{ \"evaluations\": [{ \"decision\": false }] }`; it MUST NOT permit the evaluation.\n" + AuthzenPDPEvaluationsActionNameIsNumberTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsActionNameIsNumberTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": 123 },
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
		// Section 5.3: an Action's `name` has "a string value"; Section 7.1.1: the defaults evaluations inherit.
		// Section 7.2.1: an error that pertains to the entire payload is a 4XX.
		return new String[] {"AUTHZEN-5.3", "AUTHZEN-7.1.1", "AUTHZEN-7.2.1"};
	}

	@Override
	protected String[] additionalDecisionsRequirements() {
		// Section 7.1.2.1: under execute_all "any failure can be denoted by "decision": false";
		// Section 7.2.1: the decision for an evaluation in error defaults to closed (false).
		return new String[] {"AUTHZEN-7.1.2.1", "AUTHZEN-7.2.1"};
	}
}

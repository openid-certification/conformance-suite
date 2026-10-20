package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-evaluations-is-object",
	displayName = "AuthZEN Evaluations API - Section 7.1: Evaluations is an object -- expect HTTP 400",
	summary = "Section 7.1 invalid field type (certification scenario 5.4.6). `evaluations` is an object instead of an array, so the request is neither a batch nor the backwards-compatible single request. The top level is a complete request (alice reading record-1, permitted by rule 1), so a PDP that ignores the wrongly typed member and answers it as a single request (Section 7.1) returns HTTP 200 and fails. Certification scenario 3.1 applies the error handling of scenario 5.4 to every level, and scenario 6.4.1 keeps a request that is invalid at the top level an HTTP error response. The PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationsEvaluationsIsObjectTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsEvaluationsIsObjectTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" },
			"evaluations": { "resource": { "type": "record", "id": "record-1" } }
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
		// Section 7.1: "The evaluations key is an array".
		return new String[] {"AUTHZEN-7.1"};
	}
}

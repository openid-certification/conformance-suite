package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-options-is-string",
	displayName = "AuthZEN Evaluations API - Section 7.1.2: Options is string -- expect HTTP 400",
	summary = "Section 7.1.2 invalid field type (certification scenario 5.4.6). `options` is a string instead of an object; PDP MUST return HTTP 400. The scenario requires this for any field, and `options` is a member every PDP accepts even if it acts on none of its values (Section 7.1.2), so this does not presuppose support for the evaluation semantics.\n" + AuthzenPDPEvaluationsOptionsIsStringTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsOptionsIsStringTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"evaluations": [
				{ "resource": { "type": "record", "id": "record-1" } }
			],
			"options": "execute_all"
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getExpectedEvaluationsResponseJson() {
		return "{}";
	}

	@Override
	protected int getExpectedHttpStatusCode() {
		return 400;
	}

	@Override
	protected boolean sendRawRequest() {
		return true;
	}
}

package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-subject-properties-is-string",
	displayName = "AuthZEN Evaluation API - Section 5.1: Subject properties is string -- expect HTTP 400",
	summary = "Section 5.1 invalid field type (certification scenario 5.4.6). `subject.properties` is a string instead of an object; PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationSubjectPropertiesIsStringTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationSubjectPropertiesIsStringTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice", "properties": "role=admin" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" }
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
}

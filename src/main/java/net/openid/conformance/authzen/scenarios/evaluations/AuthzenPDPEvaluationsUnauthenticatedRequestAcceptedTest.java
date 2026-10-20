package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.authzen.condition.WarnPDPAcceptsUnauthenticatedRequests;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.PDPAuthType;
import net.openid.conformance.variant.VariantApplicableOnly;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-unauthenticated-request-accepted",
	displayName = "AuthZEN Evaluations API - Section 11.2: PDP answers a request without PEP authentication",
	summary = "Per Section 11.2, \"the PDP SHOULD authenticate the calling PEP\". This module runs only when the test configuration selects no PEP authentication (pdp_auth_type none): it sends a valid fixture request without credentials, checks the fixture result, and reports a warning that the PDP answered an unauthenticated request. Select client_secret_basic, api_key or mtls to test a PDP that authenticates the PEP.\n" + AuthzenPDPEvaluationsUnauthenticatedRequestAcceptedTest.payload,
	profile = "AuthZEN"
)
@VariantApplicableOnly(parameter = PDPAuthType.class, values = {"none"})
public class AuthzenPDPEvaluationsUnauthenticatedRequestAcceptedTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
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
					{ "decision": true }
				]
			}
			""";
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		eventLog.startBlock("Check that the PDP authenticates the PEP");
		try {
			callAndContinueOnFailure(WarnPDPAcceptsUnauthenticatedRequests.class, ConditionResult.WARNING, "AUTHZEN-11.2");
		} finally {
			eventLog.endBlock();
		}
		super.onPostAuthorizationFlowComplete();
	}
}

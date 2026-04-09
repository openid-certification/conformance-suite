package net.openid.conformance.authzen.interop.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.authzen.condition.EnsureDecisionResponseFalse;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-evaluation-21",
	displayName = "Authzen Evaluation API Test 21",
	summary = "Evaluation API test 21 with payload\n" + AuthzenPDPInteropEvaluation21Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropEvaluation21Test extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "CiRmZDI2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
		},
		"action": {
			"name": "can_update_todo"
		},
		"resource": {
			"type": "todo",
			"id": "7240d0db-8ff0-41ec-98b2-34a096273b9f",
			"properties": {
				"ownerID": "rick@the-citadel.com"
			}
		}
	}
	""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected void validateAuthApiEndpointResponse() {
		callAndContinueOnFailure(EnsureDecisionResponseFalse.class, Condition.ConditionResult.FAILURE);
	}
}

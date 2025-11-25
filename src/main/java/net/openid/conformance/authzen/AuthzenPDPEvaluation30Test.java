package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.authzen.condition.EnsureDecisionResponseFalse;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-30",
	displayName = "Authzen Evaluation API Test 30",
	summary = "Evaluation API test 30 with payload\n" + AuthzenPDPEvaluation30Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPEvaluation30Test extends AbstractAuthzenPDPTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "CiRmZDM2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
		},
		"action": {
			"name": "can_update_todo"
		},
		"resource": {
			"type": "todo",
			"id": "7240d0db-8ff0-41ec-98b2-34a096273b9f",
			"properties": {
				"ownerID": "beth@the-smiths.com"
			}
		}
	}
	""";

	@Override
	protected JsonObject parseRequest() {
		return JsonParser.parseString(payload).getAsJsonObject();
	}

	@Override
	protected void validateAuthApiEndpointResponse() {
		callAndContinueOnFailure(EnsureDecisionResponseFalse.class, Condition.ConditionResult.FAILURE);
	}
}

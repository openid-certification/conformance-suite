package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-01",
	displayName = "Authzen Evaluation API Test 01",
	summary = "Evaluation API test 01 with payload\n" + AuthzenPDPEvaluation01Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPEvaluation01Test extends AbstractAuthzenPDPTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "CiRmZDA2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
		},
		"action": {
			"name": "can_read_user"
		},
		"resource": {
			"type": "user",
			"id": "beth@the-smiths.com"
		}
	}
	""";
	@Override
	protected JsonObject parseRequest() {
		return JsonParser.parseString(payload).getAsJsonObject();
	}
}

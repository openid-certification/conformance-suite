package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-10",
	displayName = "Authzen Evaluation API Test 10",
	summary = "Evaluation API test 10 with payload\n" + AuthzenPDPEvaluation10Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPEvaluation10Test extends AbstractAuthzenPDPTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "CiRmZDE2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
		},
		"action": {
			"name": "can_read_user"
		},
		"resource": {
			"type": "user",
			"id": "morty@the-citadel.com"
		}
	}
	""";

	@Override
	protected JsonObject parseRequest() {
		return JsonParser.parseString(payload).getAsJsonObject();
	}
}

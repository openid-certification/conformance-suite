package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-27",
	displayName = "Authzen Evaluation API Test 27",
	summary = "Evaluation API test 27 with payload\n" + AuthzenPDPEvaluation27Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPEvaluation27Test extends AbstractAuthzenPDPTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "CiRmZDM2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
		},
		"action": {
			"name": "can_read_todos"
		},
		"resource": {
			"type": "todo",
			"id": "todo-1"
		}
	}
	""";

	@Override
	protected JsonObject parseRequest() {
		return JsonParser.parseString(payload).getAsJsonObject();
	}
}

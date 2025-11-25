package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-04",
	displayName = "Authzen Evaluation API Test 04",
	summary = "Evaluation API test 04 with payload\n" + AuthzenPDPEvaluation04Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPEvaluation04Test extends AbstractAuthzenPDPTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "CiRmZDA2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
		},
		"action": {
			"name": "can_create_todo"
		},
		"resource": {
			"type": "todo",
			"id": "todo-1"
		}
	}
	""";

	public static final String payload1 = """
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

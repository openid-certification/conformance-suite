package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateIdTokenACRClaimAgainstRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateIdTokenACRClaimAgainstRequest cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateIdTokenACRClaimAgainstRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_caseGoodEmpty() {

		env.putObject("authorization_endpoint_request", new JsonObject());

		env.putObject("id_token", new JsonObject());

		cond.execute(env);

	}

	@Test
	public void testEvaluate_caseBadEmpty() {
		assertThrows(ConditionError.class, () -> {
			String request =
				"""
						{
						  "claims": {
						    "id_token": {
						      "acr": {
						        "value": "urn:openbanking:psd2:sca",
						        "essential": true
						      }
						    }
						  }
						}""";
			JsonObject req = JsonParser.parseString(request).getAsJsonObject();
			env.putObject("authorization_endpoint_request", req);

			env.putObject("id_token", new JsonObject());

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_caseSingleGood() {
		String request =
				"""
						{
						  "claims": {
						    "id_token": {
						      "acr": {
						        "value": "urn:openbanking:psd2:sca",
						        "essential": true
						      }
						    }
						  }
						}""";
		JsonObject req = JsonParser.parseString(request).getAsJsonObject();
		env.putObject("authorization_endpoint_request", req);

		JsonObject idToken = JsonParser.parseString("{\"claims\": {\"acr\": \"urn:openbanking:psd2:sca\"}}").getAsJsonObject();
		env.putObject("id_token", idToken);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_caseSingleBad() {
		assertThrows(ConditionError.class, () -> {
			String request =
				"""
						{
						  "claims": {
						    "id_token": {
						      "acr": {
						        "value": "urn:openbanking:psd2:ca",
						        "essential": true
						      }
						    }
						  }
						}""";

			JsonObject req = JsonParser.parseString(request).getAsJsonObject();
			env.putObject("authorization_endpoint_request", req);

			JsonObject idToken = JsonParser.parseString("{\"claims\": {\"acr\": \"urn:mace:incommon:iap:silver\"}}").getAsJsonObject();
			env.putObject("id_token", idToken);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_caseArrayGood() {
		String request =
				"""
						{
						  "claims": {
						    "id_token": {
						      "acr": {
						        "values": [
						          "urn:openbanking:psd2:sca",
						          "urn:openbanking:psd2:ca"
						        ],
						        "essential": true
						      }
						    }
						  }
						}""";

		JsonObject req = JsonParser.parseString(request).getAsJsonObject();
		env.putObject("authorization_endpoint_request", req);

		JsonObject idToken = JsonParser.parseString("{\"claims\": {\"acr\": \"urn:openbanking:psd2:sca\"}}").getAsJsonObject();
		env.putObject("id_token", idToken);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_caseArrayBad() {
		assertThrows(ConditionError.class, () -> {
			String request =
				"""
						{
						  "claims": {
						    "id_token": {
						      "acr": {
						        "values": [
						          "urn:openbanking:psd2:sca",
						          "urn:openbanking:psd2:ca"
						        ],
						        "essential": true
						      }
						    }
						  }
						}""";

			JsonObject req = JsonParser.parseString(request).getAsJsonObject();
			env.putObject("authorization_endpoint_request", req);

			JsonObject idToken = JsonParser.parseString("{\"claims\": {\"acr\": \"urn:openbanking:psd2:s\"}}").getAsJsonObject();
			env.putObject("id_token", idToken);

			cond.execute(env);

		});

	}
}

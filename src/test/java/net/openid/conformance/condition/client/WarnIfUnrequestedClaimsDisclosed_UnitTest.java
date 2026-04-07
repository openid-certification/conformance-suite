package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
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
public class WarnIfUnrequestedClaimsDisclosed_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private WarnIfUnrequestedClaimsDisclosed cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new WarnIfUnrequestedClaimsDisclosed();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * SD-JWT disclosures are JSON arrays encoded as strings: ["salt", "claim_name", "claim_value"]
	 */
	private JsonArray createDisclosures(String... claimNames) {
		JsonArray disclosures = new JsonArray();
		for (String claimName : claimNames) {
			// Each disclosure is a JSON array serialized as a string: ["salt", "claim_name", "value"]
			JsonArray disclosure = new JsonArray();
			disclosure.add("_sd_salt_" + claimName);
			disclosure.add(claimName);
			disclosure.add("some_value");
			disclosures.add(disclosure.toString());
		}
		return disclosures;
	}

	private void setupEnvironment(String dcqlJson, String credentialId, String... disclosedClaimNames) {
		env.putString("credential_id", credentialId);
		env.putObject("dcql_query", JsonParser.parseString(dcqlJson).getAsJsonObject());

		JsonObject sdjwt = new JsonObject();
		sdjwt.add("disclosures", createDisclosures(disclosedClaimNames));
		env.putObject("sdjwt", sdjwt);
	}

	@Test
	public void testEvaluate_onlyRequestedClaimsDisclosedPasses() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "claims": [
			        {"path": ["given_name"]},
			        {"path": ["family_name"]}
			      ]
			    }
			  ]
			}
			""";
		setupEnvironment(dcql, "my_credential", "given_name", "family_name");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_unrequestedClaimDisclosedThrowsError() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "claims": [
			        {"path": ["given_name"]}
			      ]
			    }
			  ]
			}
			""";
		setupEnvironment(dcql, "my_credential", "given_name", "email");

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_noClaimsInDcqlQuerySkipsCheck() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt"
			    }
			  ]
			}
			""";
		setupEnvironment(dcql, "my_credential", "given_name", "family_name", "email");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_emptyClaimsArraySkipsCheck() {
		// When claims array is present but empty, requestedClaims will be empty
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "claims": []
			    }
			  ]
			}
			""";
		setupEnvironment(dcql, "my_credential", "given_name");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_subsetOfRequestedClaimsPasses() {
		// Disclosing fewer claims than requested is fine
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "claims": [
			        {"path": ["given_name"]},
			        {"path": ["family_name"]},
			        {"path": ["email"]}
			      ]
			    }
			  ]
			}
			""";
		setupEnvironment(dcql, "my_credential", "given_name");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_credentialIdNotFoundSkipsCheck() {
		// When credential_id does not match any DCQL entry, requestedClaims
		// will be empty and the check is skipped
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "different_credential",
			      "format": "dc+sd-jwt",
			      "claims": [
			        {"path": ["given_name"]}
			      ]
			    }
			  ]
			}
			""";
		setupEnvironment(dcql, "my_credential", "given_name", "email");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_multipleUnrequestedClaimsThrowsError() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "claims": [
			        {"path": ["given_name"]}
			      ]
			    }
			  ]
			}
			""";
		setupEnvironment(dcql, "my_credential", "given_name", "email", "phone_number");

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_multipleCredentialsMatchesCorrectOne() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "first_credential",
			      "format": "dc+sd-jwt",
			      "claims": [
			        {"path": ["email"]}
			      ]
			    },
			    {
			      "id": "second_credential",
			      "format": "dc+sd-jwt",
			      "claims": [
			        {"path": ["given_name"]},
			        {"path": ["family_name"]}
			      ]
			    }
			  ]
			}
			""";
		setupEnvironment(dcql, "second_credential", "given_name", "family_name");

		cond.execute(env);
	}
}

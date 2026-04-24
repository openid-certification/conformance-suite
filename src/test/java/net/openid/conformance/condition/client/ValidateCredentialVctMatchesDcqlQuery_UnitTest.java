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
public class ValidateCredentialVctMatchesDcqlQuery_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateCredentialVctMatchesDcqlQuery cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateCredentialVctMatchesDcqlQuery();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void setupEnvironment(String vct, String credentialId, String dcqlJson) {
		JsonObject claims = new JsonObject();
		claims.addProperty("vct", vct);
		env.putObject("sdjwt", "credential.claims", claims);
		env.putString("credential_id", credentialId);
		env.putObject("dcql_query", JsonParser.parseString(dcqlJson).getAsJsonObject());
	}

	@Test
	public void testEvaluate_matchingVctPasses() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential",
			          "https://example.com/other_credential"
			        ]
			      }
			    }
			  ]
			}
			""";
		setupEnvironment("https://example.com/identity_credential", "my_credential", dcql);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_matchingVctSecondValuePasses() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential",
			          "https://example.com/other_credential"
			        ]
			      }
			    }
			  ]
			}
			""";
		setupEnvironment("https://example.com/other_credential", "my_credential", dcql);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_nonMatchingVctFails() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      }
			    }
			  ]
			}
			""";
		setupEnvironment("https://example.com/wrong_credential", "my_credential", dcql);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingVctValuesInMetaSkipsCheck() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "some_other_field": "value"
			      }
			    }
			  ]
			}
			""";
		setupEnvironment("https://example.com/identity_credential", "my_credential", dcql);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingMetaFieldSkipsCheck() {
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
		setupEnvironment("https://example.com/identity_credential", "my_credential", dcql);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_credentialIdNotFoundInDcqlFails() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "different_credential",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      }
			    }
			  ]
			}
			""";
		setupEnvironment("https://example.com/identity_credential", "my_credential", dcql);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingVctClaimInCredentialFails() {
		// Set up credential without vct claim
		JsonObject claims = new JsonObject();
		claims.addProperty("iss", "https://example.com");
		env.putObject("sdjwt", "credential.claims", claims);
		env.putString("credential_id", "my_credential");

		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/identity_credential"
			        ]
			      }
			    }
			  ]
			}
			""";
		env.putObject("dcql_query", JsonParser.parseString(dcql).getAsJsonObject());

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingCredentialsArrayInDcqlFails() {
		String dcql = """
			{
			  "credential_sets": []
			}
			""";
		setupEnvironment("https://example.com/identity_credential", "my_credential", dcql);

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
			      "meta": {
			        "vct_values": [
			          "https://example.com/first_type"
			        ]
			      }
			    },
			    {
			      "id": "second_credential",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "https://example.com/second_type"
			        ]
			      }
			    }
			  ]
			}
			""";
		setupEnvironment("https://example.com/second_type", "second_credential", dcql);

		cond.execute(env);
	}
}

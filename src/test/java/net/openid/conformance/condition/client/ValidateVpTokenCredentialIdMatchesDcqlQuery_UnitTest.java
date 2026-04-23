package net.openid.conformance.condition.client;

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
public class ValidateVpTokenCredentialIdMatchesDcqlQuery_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateVpTokenCredentialIdMatchesDcqlQuery cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateVpTokenCredentialIdMatchesDcqlQuery();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void setupEnvironment(String credentialId, String dcqlJson) {
		env.putString("credential_id", credentialId);
		env.putObject("dcql_query", JsonParser.parseString(dcqlJson).getAsJsonObject());
	}

	@Test
	public void testEvaluate_matchingCredentialIdPasses() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": ["https://example.com/identity_credential"]
			      }
			    }
			  ]
			}
			""";
		setupEnvironment("my_credential", dcql);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_nonMatchingCredentialIdFails() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "expected_credential",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": ["https://example.com/identity_credential"]
			      }
			    }
			  ]
			}
			""";
		setupEnvironment("wrong_credential", dcql);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_matchesAmongMultipleCredentials() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "first_credential",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": ["https://example.com/first_type"]
			      }
			    },
			    {
			      "id": "second_credential",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": ["https://example.com/second_type"]
			      }
			    }
			  ]
			}
			""";
		setupEnvironment("second_credential", dcql);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingCredentialsArrayFails() {
		String dcql = """
			{
			  "credential_sets": []
			}
			""";
		setupEnvironment("my_credential", dcql);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_emptyCredentialsArrayFails() {
		String dcql = """
			{
			  "credentials": []
			}
			""";
		setupEnvironment("my_credential", dcql);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_credentialWithoutIdFieldFails() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": ["https://example.com/identity_credential"]
			      }
			    }
			  ]
			}
			""";
		setupEnvironment("my_credential", dcql);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}
}

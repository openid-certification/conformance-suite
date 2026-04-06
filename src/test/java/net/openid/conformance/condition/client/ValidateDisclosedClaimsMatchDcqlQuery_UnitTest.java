package net.openid.conformance.condition.client;

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
public class ValidateDisclosedClaimsMatchDcqlQuery_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateDisclosedClaimsMatchDcqlQuery cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateDisclosedClaimsMatchDcqlQuery();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void setupEnvironment(String decodedJson, String credentialId, String dcqlJson) {
		JsonObject decoded = JsonParser.parseString(decodedJson).getAsJsonObject();
		env.putObject("sdjwt", "decoded", decoded);
		env.putString("credential_id", credentialId);
		env.putObject("dcql_query", JsonParser.parseString(dcqlJson).getAsJsonObject());
	}

	@Test
	public void testEvaluate_allRequestedClaimsPresentPasses() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": ["https://example.com/identity_credential"]
			      },
			      "claims": [
			        {"path": ["given_name"]},
			        {"path": ["family_name"]}
			      ]
			    }
			  ]
			}
			""";
		String decoded = """
			{
			  "given_name": "John",
			  "family_name": "Doe",
			  "vct": "https://example.com/identity_credential",
			  "iss": "https://example.com"
			}
			""";
		setupEnvironment(decoded, "my_credential", dcql);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingRequestedClaimFails() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": ["https://example.com/identity_credential"]
			      },
			      "claims": [
			        {"path": ["given_name"]},
			        {"path": ["family_name"]},
			        {"path": ["email"]}
			      ]
			    }
			  ]
			}
			""";
		String decoded = """
			{
			  "given_name": "John",
			  "family_name": "Doe",
			  "vct": "https://example.com/identity_credential"
			}
			""";
		setupEnvironment(decoded, "my_credential", dcql);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_noClaimsInDcqlSkipsCheck() {
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
		String decoded = """
			{
			  "given_name": "John",
			  "vct": "https://example.com/identity_credential"
			}
			""";
		setupEnvironment(decoded, "my_credential", dcql);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_emptyClaimsArraySkipsCheck() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": ["https://example.com/identity_credential"]
			      },
			      "claims": []
			    }
			  ]
			}
			""";
		String decoded = """
			{
			  "given_name": "John",
			  "vct": "https://example.com/identity_credential"
			}
			""";
		setupEnvironment(decoded, "my_credential", dcql);

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
			      "claims": [
			        {"path": ["given_name"]}
			      ]
			    }
			  ]
			}
			""";
		String decoded = """
			{
			  "given_name": "John"
			}
			""";
		setupEnvironment(decoded, "my_credential", dcql);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingCredentialsArrayFails() {
		String dcql = """
			{
			  "credential_sets": []
			}
			""";
		String decoded = """
			{
			  "given_name": "John"
			}
			""";
		setupEnvironment(decoded, "my_credential", dcql);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingDecodedSdJwtFails() {
		// Set up sdjwt object without decoded field
		JsonObject sdjwt = new JsonObject();
		JsonObject credential = new JsonObject();
		JsonObject claims = new JsonObject();
		claims.addProperty("vct", "https://example.com/identity_credential");
		credential.add("claims", claims);
		sdjwt.add("credential", credential);
		env.putObject("sdjwt", sdjwt);
		env.putString("credential_id", "my_credential");

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
		env.putObject("dcql_query", JsonParser.parseString(dcql).getAsJsonObject());

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_extraDisclosedClaimsStillPasses() {
		// Extra claims beyond what was requested should not cause failure here
		// (that is checked by WarnIfUnrequestedClaimsDisclosed)
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
		String decoded = """
			{
			  "given_name": "John",
			  "family_name": "Doe",
			  "email": "john@example.com"
			}
			""";
		setupEnvironment(decoded, "my_credential", dcql);

		cond.execute(env);
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
		String decoded = """
			{
			  "given_name": "John",
			  "family_name": "Doe"
			}
			""";
		setupEnvironment(decoded, "second_credential", dcql);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_nestedRequestedClaimPresentPasses() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "claims": [
			        {"path": ["address", "street_address"]}
			      ]
			    }
			  ]
			}
			""";
		String decoded = """
			{
			  "address": {
			    "street_address": "123 Main St",
			    "locality": "London"
			  }
			}
			""";
		setupEnvironment(decoded, "my_credential", dcql);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_nestedRequestedClaimMissingFails() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "claims": [
			        {"path": ["address", "street_address"]}
			      ]
			    }
			  ]
			}
			""";
		String decoded = """
			{
			  "address": {
			    "locality": "London"
			  }
			}
			""";
		setupEnvironment(decoded, "my_credential", dcql);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}

package net.openid.conformance.condition.client;

import com.authlete.sd.Disclosure;
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
public class CheckOnlyRequestedClaimsDisclosed_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckOnlyRequestedClaimsDisclosed cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckOnlyRequestedClaimsDisclosed();
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
		JsonObject decoded = new JsonObject();
		for (String disclosedClaimName : disclosedClaimNames) {
			decoded.addProperty(disclosedClaimName, "some_value");
		}
		setupEnvironment(dcqlJson, credentialId, decoded.toString(), createDisclosures(disclosedClaimNames));
	}

	private void setupEnvironment(String dcqlJson, String credentialId, String decodedJson, JsonArray disclosures) {
		setupEnvironment(dcqlJson, credentialId, decodedJson, disclosures, null);
	}

	private void setupEnvironment(String dcqlJson, String credentialId, String decodedJson, JsonArray disclosures, JsonObject credential) {
		env.putString("credential_id", credentialId);
		env.putObject("dcql_query", JsonParser.parseString(dcqlJson).getAsJsonObject());

		JsonObject sdjwt = new JsonObject();
		sdjwt.add("decoded", JsonParser.parseString(decodedJson).getAsJsonObject());
		sdjwt.add("disclosures", disclosures);
		if (credential != null) {
			sdjwt.add("credential", credential);
		}
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
	public void testEvaluate_noClaimsInDcqlQueryWithoutDisclosuresPasses() {
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
		setupEnvironment(dcql, "my_credential", "{}", new JsonArray());

		cond.execute(env);
	}

	@Test
	public void testEvaluate_noClaimsInDcqlQueryWithDisclosuresThrowsError() {
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

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_emptyClaimsArrayWithoutDisclosuresPasses() {
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
		setupEnvironment(dcql, "my_credential", "{}", new JsonArray());

		cond.execute(env);
	}

	@Test
	public void testEvaluate_emptyClaimsArrayWithDisclosuresThrowsError() {
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

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
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

	@Test
	public void testEvaluate_nestedRequestedDisclosurePasses() {
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
			    "street_address": "123 Main St"
			  }
			}
			""";
		JsonArray disclosures = new JsonArray();
		disclosures.add("[\"salt1\", \"address\", {\"_sd\": [\"digest1\"]}]");
		disclosures.add("[\"salt2\", \"street_address\", \"123 Main St\"]");

		setupEnvironment(dcql, "my_credential", decoded, disclosures);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_unrequestedNestedDisclosureThrowsError() {
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
		JsonArray disclosures = new JsonArray();
		disclosures.add("[\"salt1\", \"address\", {\"_sd\": [\"digest1\", \"digest2\"]}]");
		disclosures.add("[\"salt2\", \"street_address\", \"123 Main St\"]");
		disclosures.add("[\"salt3\", \"locality\", \"London\"]");

		setupEnvironment(dcql, "my_credential", decoded, disclosures);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_orphanArrayElementDisclosureThrowsError() {
		// An array element disclosure is sent without any disclosure or JWT body referencing
		// its digest; the wallet has leaked the value with no parent to bind it to.
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
			  "given_name": "Jean"
			}
			""";

		String givenNameDisclosure = "[\"salt1\", \"given_name\", \"Jean\"]";
		String orphanArrayDisclosure = "[\"salt2\", \"FR\"]";

		JsonArray disclosures = new JsonArray();
		disclosures.add(givenNameDisclosure);
		disclosures.add(orphanArrayDisclosure);

		// JWT body has no digest references covering the orphan disclosure
		JsonObject credential = new JsonObject();

		setupEnvironment(dcql, "my_credential", decoded, disclosures, credential);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_arrayElementReferencedByParentDisclosurePasses() {
		// nationalities is requested; the wallet sends both the parent object disclosure
		// (which references the array element's digest via {"...": digest}) and the array
		// element itself. The array element is reachable, so no orphan.
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "claims": [
			        {"path": ["nationalities"]}
			      ]
			    }
			  ]
			}
			""";
		String decoded = """
			{
			  "nationalities": ["FR"]
			}
			""";

		Disclosure arrayElement = new Disclosure("salt1", null, "FR");
		Disclosure parent = new Disclosure("salt2", "nationalities",
			java.util.List.of(arrayElement.toArrayElement()));

		JsonArray disclosures = new JsonArray();
		disclosures.add(parent.getJson());
		disclosures.add(arrayElement.getJson());

		setupEnvironment(dcql, "my_credential", decoded, disclosures, new JsonObject());

		cond.execute(env);
	}

	@Test
	public void testEvaluate_descendantDisclosurePasses() {
		// OID4VP §7.3: DCQL path ["address"] selects the address claim with its sub-claims.
		// A wallet disclosing the nested "street_address" is fulfilling the request, not
		// over-disclosing, so this must pass.
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "claims": [
			        {"path": ["address"]}
			      ]
			    }
			  ]
			}
			""";
		String decoded = """
			{
			  "address": {
			    "street_address": "123 Main St"
			  }
			}
			""";
		JsonArray disclosures = new JsonArray();
		disclosures.add("[\"salt1\", \"address\", {\"_sd\": [\"digest1\"]}]");
		disclosures.add("[\"salt2\", \"street_address\", \"123 Main St\"]");

		setupEnvironment(dcql, "my_credential", decoded, disclosures);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_arrayElementReferencedByJwtBodyPasses() {
		// The credential JWT body itself contains the {"...": digest} reference. The array
		// element disclosure is reachable from the body, so no orphan.
		Disclosure arrayElement = new Disclosure("salt1", null, "FR");

		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "claims": [
			        {"path": ["nationalities"]}
			      ]
			    }
			  ]
			}
			""";
		String decoded = """
			{
			  "nationalities": ["FR"]
			}
			""";

		JsonArray disclosures = new JsonArray();
		disclosures.add(arrayElement.getJson());

		String credentialJson = String.format(
			"{\"nationalities\": [{\"...\": \"%s\"}]}", arrayElement.digest());
		JsonObject credential = JsonParser.parseString(credentialJson).getAsJsonObject();

		setupEnvironment(dcql, "my_credential", decoded, disclosures, credential);

		cond.execute(env);
	}
}

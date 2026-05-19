package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckOnlyRequestedMdocElementsDisclosed_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckOnlyRequestedMdocElementsDisclosed cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckOnlyRequestedMdocElementsDisclosed();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void setupEnvironment(String dcqlJson, String credentialId,
		Map<String, String[]> disclosedElementsByNamespace) {
		env.putString("credential_id", credentialId);
		env.putObject("dcql_query", JsonParser.parseString(dcqlJson).getAsJsonObject());

		JsonObject disclosed = new JsonObject();
		for (Map.Entry<String, String[]> e : disclosedElementsByNamespace.entrySet()) {
			JsonArray elements = new JsonArray();
			for (String elem : e.getValue()) {
				elements.add(elem);
			}
			disclosed.add(e.getKey(), elements);
		}

		JsonObject mdoc = new JsonObject();
		mdoc.addProperty("docType", "org.iso.18013.5.1.mDL");
		mdoc.add("disclosed_elements", disclosed);
		env.putObject("mdoc", mdoc);
	}

	@Test
	public void testEvaluate_onlyRequestedElementsDisclosedPasses() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "mso_mdoc",
			      "claims": [
			        {"path": ["org.iso.18013.5.1", "given_name"]},
			        {"path": ["org.iso.18013.5.1", "family_name"]}
			      ]
			    }
			  ]
			}
			""";
		Map<String, String[]> disclosed = new LinkedHashMap<>();
		disclosed.put("org.iso.18013.5.1", new String[]{"given_name", "family_name"});
		setupEnvironment(dcql, "my_credential", disclosed);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_unrequestedElementDisclosedThrowsError() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "mso_mdoc",
			      "claims": [
			        {"path": ["org.iso.18013.5.1", "given_name"]}
			      ]
			    }
			  ]
			}
			""";
		Map<String, String[]> disclosed = new LinkedHashMap<>();
		disclosed.put("org.iso.18013.5.1", new String[]{"given_name", "family_name"});
		setupEnvironment(dcql, "my_credential", disclosed);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_elementInUnrequestedNamespaceThrowsError() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "mso_mdoc",
			      "claims": [
			        {"path": ["org.iso.18013.5.1", "given_name"]}
			      ]
			    }
			  ]
			}
			""";
		Map<String, String[]> disclosed = new LinkedHashMap<>();
		disclosed.put("org.iso.18013.5.1", new String[]{"given_name"});
		disclosed.put("org.iso.18013.5.1.aamva", new String[]{"organ_donor"});
		setupEnvironment(dcql, "my_credential", disclosed);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_noClaimsInDcqlWithoutDisclosuresPasses() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "mso_mdoc"
			    }
			  ]
			}
			""";
		Map<String, String[]> disclosed = new LinkedHashMap<>();
		setupEnvironment(dcql, "my_credential", disclosed);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_noClaimsInDcqlWithDisclosuresThrowsError() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "mso_mdoc"
			    }
			  ]
			}
			""";
		Map<String, String[]> disclosed = new LinkedHashMap<>();
		disclosed.put("org.iso.18013.5.1", new String[]{"given_name"});
		setupEnvironment(dcql, "my_credential", disclosed);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_credentialIdNotInDcqlIsSilentlySkipped() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "other_credential",
			      "format": "mso_mdoc",
			      "claims": [
			        {"path": ["org.iso.18013.5.1", "given_name"]}
			      ]
			    }
			  ]
			}
			""";
		Map<String, String[]> disclosed = new LinkedHashMap<>();
		disclosed.put("org.iso.18013.5.1", new String[]{"given_name"});
		setupEnvironment(dcql, "my_credential", disclosed);

		cond.execute(env);
	}
}

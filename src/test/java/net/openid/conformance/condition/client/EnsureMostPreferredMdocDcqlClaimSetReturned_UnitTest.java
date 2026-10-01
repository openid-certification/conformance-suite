package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureMostPreferredMdocDcqlClaimSetReturned_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureMostPreferredMdocDcqlClaimSetReturned cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureMostPreferredMdocDcqlClaimSetReturned();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.WARNING);
	}

	private void setupEnvironment(String dcqlJson, String... disclosedElements) {
		env.putString("credential_id", "my_credential");
		env.putObject("dcql_query", JsonParser.parseString(dcqlJson).getAsJsonObject());

		JsonObject disclosed = new JsonObject();
		disclosed.add("org.iso.18013.5.1", OIDFJSON.convertListToJsonArray(List.of(disclosedElements)));
		JsonObject mdoc = new JsonObject();
		mdoc.add("disclosed_elements", disclosed);
		env.putObject("mdoc", mdoc);
	}

	@Test
	public void testEvaluate_firstOptionReturnedPasses() {
		setupEnvironment(DcqlTestFixtures.AGE_OVER_18_MDOC_DCQL, "age_over_18");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_laterOptionReturnedFails() {
		setupEnvironment(DcqlTestFixtures.AGE_OVER_18_MDOC_DCQL, "age_in_years");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_lastOptionReturnedFails() {
		setupEnvironment(DcqlTestFixtures.AGE_OVER_18_MDOC_DCQL, "birth_date");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_noOptionReturnedIsLeftToOtherChecks() {
		setupEnvironment(DcqlTestFixtures.AGE_OVER_18_MDOC_DCQL, "family_name");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_noClaimSetsPasses() {
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
		setupEnvironment(dcql, "family_name");

		cond.execute(env);
	}
}

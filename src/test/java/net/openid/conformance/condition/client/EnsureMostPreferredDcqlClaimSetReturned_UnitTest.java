package net.openid.conformance.condition.client;

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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureMostPreferredDcqlClaimSetReturned_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureMostPreferredDcqlClaimSetReturned cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureMostPreferredDcqlClaimSetReturned();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.WARNING);
	}

	private void setupEnvironment(String decodedJson, String dcqlJson) {
		env.putObject("sdjwt", "decoded", JsonParser.parseString(decodedJson).getAsJsonObject());
		env.putString("credential_id", "my_credential");
		env.putObject("dcql_query", JsonParser.parseString(dcqlJson).getAsJsonObject());
	}

	@Test
	public void testEvaluate_firstOptionReturnedPasses() {
		setupEnvironment("{\"age_equal_or_over\": {\"18\": true}}", DcqlTestFixtures.AGE_OVER_18_SD_JWT_DCQL);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_laterOptionReturnedFails() {
		setupEnvironment("{\"age_in_years\": 46}", DcqlTestFixtures.AGE_OVER_18_SD_JWT_DCQL);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_lastOptionReturnedFails() {
		setupEnvironment("{\"birthdate\": \"1980-05-23\"}", DcqlTestFixtures.AGE_OVER_18_SD_JWT_DCQL);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_noOptionReturnedIsLeftToOtherChecks() {
		setupEnvironment("{\"given_name\": \"John\"}", DcqlTestFixtures.AGE_OVER_18_SD_JWT_DCQL);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_noClaimSetsPasses() {
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
		setupEnvironment("{\"family_name\": \"Doe\"}", dcql);

		cond.execute(env);
	}
}

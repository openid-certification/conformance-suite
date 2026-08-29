package net.openid.conformance.condition.client;

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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateDisclosedClaimsMatchDcqlQueryExceptPicture_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateDisclosedClaimsMatchDcqlQueryExceptPicture cond;

	private static final String DCQL_WITH_PICTURE = """
		{
		  "credentials": [
		    {
		      "id": "my_credential",
		      "format": "dc+sd-jwt",
		      "meta": {
		        "vct_values": ["urn:eudi:pid:1"]
		      },
		      "claims": [
		        {"path": ["given_name"]},
		        {"path": ["family_name"]},
		        {"path": ["picture"]}
		      ]
		    }
		  ]
		}
		""";

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateDisclosedClaimsMatchDcqlQueryExceptPicture();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void setupEnvironment(String decodedJson) {
		JsonObject decoded = JsonParser.parseString(decodedJson).getAsJsonObject();
		env.putObject("sdjwt", "decoded", decoded);
		env.putString("credential_id", "my_credential");
		env.putObject("dcql_query", JsonParser.parseString(DCQL_WITH_PICTURE).getAsJsonObject());
	}

	@Test
	public void testEvaluate_missingPicturePasses() {
		String decoded = """
			{
			  "given_name": "John",
			  "family_name": "Doe",
			  "vct": "urn:eudi:pid:1"
			}
			""";
		setupEnvironment(decoded);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_picturePresentPasses() {
		String decoded = """
			{
			  "given_name": "John",
			  "family_name": "Doe",
			  "picture": "data:image/jpeg;base64,abc123",
			  "vct": "urn:eudi:pid:1"
			}
			""";
		setupEnvironment(decoded);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingOtherClaimStillFails() {
		String decoded = """
			{
			  "given_name": "John",
			  "picture": "data:image/jpeg;base64,abc123",
			  "vct": "urn:eudi:pid:1"
			}
			""";
		setupEnvironment(decoded);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}
}

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
public class ValidateMdocDocTypeMatchesDcqlQuery_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateMdocDocTypeMatchesDcqlQuery cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateMdocDocTypeMatchesDcqlQuery();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void setupEnvironment(String dcqlJson, String credentialId, String docType) {
		env.putString("credential_id", credentialId);
		env.putObject("dcql_query", JsonParser.parseString(dcqlJson).getAsJsonObject());
		JsonObject mdoc = new JsonObject();
		if (docType != null) {
			mdoc.addProperty("docType", docType);
		}
		mdoc.add("disclosed_elements", new JsonObject());
		env.putObject("mdoc", mdoc);
	}

	@Test
	public void testEvaluate_docTypeMatchesPasses() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "mso_mdoc",
			      "meta": {"doctype_value": "org.iso.18013.5.1.mDL"}
			    }
			  ]
			}
			""";
		setupEnvironment(dcql, "my_credential", "org.iso.18013.5.1.mDL");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_docTypeMismatchThrowsError() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "mso_mdoc",
			      "meta": {"doctype_value": "org.iso.18013.5.1.mDL"}
			    }
			  ]
			}
			""";
		setupEnvironment(dcql, "my_credential", "org.iso.7367.1.mVRC");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingDocTypeThrowsError() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "mso_mdoc",
			      "meta": {"doctype_value": "org.iso.18013.5.1.mDL"}
			    }
			  ]
			}
			""";
		setupEnvironment(dcql, "my_credential", null);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_noMetaSkipsValidation() {
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
		setupEnvironment(dcql, "my_credential", "org.iso.18013.5.1.mDL");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_noDoctypeValueSkipsValidation() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "mso_mdoc",
			      "meta": {}
			    }
			  ]
			}
			""";
		setupEnvironment(dcql, "my_credential", "org.iso.18013.5.1.mDL");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_credentialIdNotInDcqlThrowsError() {
		String dcql = """
			{
			  "credentials": [
			    {
			      "id": "other_credential",
			      "format": "mso_mdoc",
			      "meta": {"doctype_value": "org.iso.18013.5.1.mDL"}
			    }
			  ]
			}
			""";
		setupEnvironment(dcql, "my_credential", "org.iso.18013.5.1.mDL");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}

package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
public class CheckDCQLQueryCredentialFormatMatchesTestConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckDCQLQueryCredentialFormatMatchesTestConfiguration cond;

	@BeforeEach
	public void setUp() {
		cond = new CheckDCQLQueryCredentialFormatMatchesTestConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void setDcqlQuery(String format) {
		JsonObject credential = new JsonObject();
		credential.addProperty("format", format);
		credential.addProperty("id", "cred1");
		JsonArray credentials = new JsonArray();
		credentials.add(credential);
		JsonObject dcql = new JsonObject();
		dcql.add("credentials", credentials);
		env.putObject("dcql_query", dcql);
	}

	@Test
	public void testEvaluate_sdJwtMatches() {
		env.putString("credential_format", "sd_jwt_vc");
		setDcqlQuery("dc+sd-jwt");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_mdocMatches() {
		env.putString("credential_format", "iso_mdl");
		setDcqlQuery("mso_mdoc");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_mismatch() {
		assertThrows(ConditionError.class, () -> {
			env.putString("credential_format", "sd_jwt_vc");
			setDcqlQuery("mso_mdoc");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_unknownConfiguredFormat() {
		assertThrows(ConditionError.class, () -> {
			env.putString("credential_format", "unknown_format");
			setDcqlQuery("dc+sd-jwt");
			cond.execute(env);
		});
	}
}

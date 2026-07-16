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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FAPIBrazilAddSoftwareStatementRedirectUrisToDynamicRegistrationRequest_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private FAPIBrazilAddSoftwareStatementRedirectUrisToDynamicRegistrationRequest condition;

	@BeforeEach
	public void setUp() {
		condition = new FAPIBrazilAddSoftwareStatementRedirectUrisToDynamicRegistrationRequest();
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putObject("dynamic_registration_request", new JsonObject());
	}

	@Test
	public void copiesAllSoftwareStatementRedirectUris() {
		putSoftwareStatementClaims("{\"software_redirect_uris\":["
			+ "\"https://client.example/callback\",\"https://client.example/other\"]}");

		condition.execute(env);

		assertThat(env.getObject("dynamic_registration_request").getAsJsonArray("redirect_uris"))
			.containsExactly(
				JsonParser.parseString("\"https://client.example/callback\""),
				JsonParser.parseString("\"https://client.example/other\""));
	}

	@Test
	public void rejectsMissingRedirectUris() {
		putSoftwareStatementClaims("{}");

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsNonArrayRedirectUris() {
		putSoftwareStatementClaims("{\"software_redirect_uris\":\"https://client.example/callback\"}");

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsEmptyRedirectUris() {
		putSoftwareStatementClaims("{\"software_redirect_uris\":[]}");

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsNonStringOrEmptyRedirectUriEntries() {
		putSoftwareStatementClaims("{\"software_redirect_uris\":[\"\",42]}");

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	private void putSoftwareStatementClaims(String claimsJson) {
		JsonObject softwareStatement = new JsonObject();
		softwareStatement.add("claims", JsonParser.parseString(claimsJson).getAsJsonObject());
		env.putObject("software_statement_assertion", softwareStatement);
	}
}

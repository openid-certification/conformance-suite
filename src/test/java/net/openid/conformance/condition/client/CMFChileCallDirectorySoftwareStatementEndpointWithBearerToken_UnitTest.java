package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class CMFChileCallDirectorySoftwareStatementEndpointWithBearerToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CMFChileCallDirectorySoftwareStatementEndpointWithBearerToken cond;

	@BeforeEach
	public void setUp() {
		cond = new CMFChileCallDirectorySoftwareStatementEndpointWithBearerToken();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putObject("directory_access_token", JsonParser.parseString("{\"value\":\"token\",\"type\":\"Bearer\"}").getAsJsonObject());
	}

	@Test
	public void testGetSoftwareStatementEndpoint_configured() {
		env.putObject("config", JsonParser.parseString("{\"directory\":{\"software_statement_endpoint\":\"https://directorio.example.cl/ssa\"}}").getAsJsonObject());

		assertThat(cond.getSoftwareStatementEndpoint(env)).isEqualTo("https://directorio.example.cl/ssa");
	}

	@Test
	public void testEvaluate_missingSoftwareStatementEndpoint() {
		env.putObject("config", new JsonObject());

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Directory software statement endpoint'");
	}

	@Test
	public void testEvaluate_notBearerToken() {
		env.putObject("config", JsonParser.parseString("{\"directory\":{\"software_statement_endpoint\":\"https://directorio.example.cl/ssa\"}}").getAsJsonObject());
		env.putObject("directory_access_token", JsonParser.parseString("{\"value\":\"token\",\"type\":\"DPoP\"}").getAsJsonObject());

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("not a bearer token");
	}
}

package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CMFChileSoftwareStatementConditions_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		env = new Environment();
	}

	private <T extends AbstractCondition> T condition(T cond) {
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		return cond;
	}

	private void putSoftwareStatementClaims(String claimsJson) {
		JsonObject statement = new JsonObject();
		statement.add("claims", JsonParser.parseString(claimsJson));
		env.putObject("software_statement", statement);
	}

	@Test
	public void testFetchJwks_missingJwksUri() {
		env.putObject("config", new JsonObject());
		CMFChileFetchDirectorySSAJwks cond = condition(new CMFChileFetchDirectorySSAJwks());

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Directory software statement JWKS URI'");
	}

	@Test
	public void testFetchJwks_configuredJwksUri() {
		env.putObject("config", JsonParser.parseString("{\"directory\":{\"ssa_jwks_uri\":\"https://directorio.example.cl/jwks\"}}").getAsJsonObject());
		CMFChileFetchDirectorySSAJwks cond = condition(new CMFChileFetchDirectorySSAJwks());

		assertThat(cond.getJwksUri(env)).isEqualTo("https://directorio.example.cl/jwks");
	}

	@Test
	public void testSoftwareStatementPresent() {
		env.putObject("dynamic_registration_request", JsonParser.parseString("{\"software_statement\":\"eyJ.a.b\"}").getAsJsonObject());

		assertThatCode(() -> condition(new EnsureDynamicRegistrationRequestContainsSoftwareStatement()).execute(env))
			.doesNotThrowAnyException();
	}

	@Test
	public void testSoftwareStatementMissing() {
		env.putObject("dynamic_registration_request", JsonParser.parseString("{\"redirect_uris\":[\"https://rp.example.cl/cb\"]}").getAsJsonObject());

		assertThatThrownBy(() -> condition(new EnsureDynamicRegistrationRequestContainsSoftwareStatement()).execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("software_statement");
	}

	@Test
	public void testExp_notExpired() {
		putSoftwareStatementClaims("{\"exp\":" + Instant.now().plusSeconds(300).getEpochSecond() + "}");

		assertThatCode(() -> condition(new ValidateSoftwareStatementExp()).execute(env))
			.doesNotThrowAnyException();
	}

	@Test
	public void testExp_expired() {
		putSoftwareStatementClaims("{\"exp\":" + Instant.now().minusSeconds(60).getEpochSecond() + "}");

		assertThatThrownBy(() -> condition(new ValidateSoftwareStatementExp()).execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("expired");
	}

	@Test
	public void testExp_missing() {
		putSoftwareStatementClaims("{}");

		assertThatThrownBy(() -> condition(new ValidateSoftwareStatementExp()).execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("(exp)");
	}

	@Test
	public void testJti_present() {
		putSoftwareStatementClaims("{\"jti\":\"0f1e2d3c\"}");

		assertThatCode(() -> condition(new EnsureSoftwareStatementContainsJti()).execute(env))
			.doesNotThrowAnyException();
	}

	@Test
	public void testJti_missing() {
		putSoftwareStatementClaims("{}");

		assertThatThrownBy(() -> condition(new EnsureSoftwareStatementContainsJti()).execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("(jti)");
	}
}

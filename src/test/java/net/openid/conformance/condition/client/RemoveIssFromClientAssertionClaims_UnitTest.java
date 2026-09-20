package net.openid.conformance.condition.client;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class RemoveIssFromClientAssertionClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private RemoveIssFromClientAssertionClaims cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new RemoveIssFromClientAssertionClaims();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_removesClaim() {
		JsonObject claims = new JsonObject();
		claims.addProperty("iss", "value");
		claims.addProperty("sub", "kept");
		env.putObject("client_assertion_claims", claims);

		cond.execute(env);

		assertThat(env.getObject("client_assertion_claims").has("iss")).isFalse();
		assertThat(env.getString("client_assertion_claims", "sub")).isEqualTo("kept");
	}

	@Test
	public void testEvaluate_missingClaims() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}

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
public class RemoveClientAssertionTypeFromRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private RemoveClientAssertionTypeFromRequest cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new RemoveClientAssertionTypeFromRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_removesOnlyTheType() {
		JsonObject form = new JsonObject();
		form.addProperty("grant_type", "client_credentials");
		form.addProperty("client_assertion", "a.b.c");
		form.addProperty("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
		env.putObject("request_form_parameters", form);

		cond.execute(env);

		JsonObject result = env.getObject("request_form_parameters");
		assertThat(env.getString("request_form_parameters", "grant_type")).isEqualTo("client_credentials");
		assertThat(result.has("client_assertion_type")).isFalse();
		assertThat(env.getString("request_form_parameters", "client_assertion")).isEqualTo("a.b.c");
	}

	@Test
	public void testEvaluate_missingForm() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}

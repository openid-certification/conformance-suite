package net.openid.conformance.condition.client;

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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureIdTokenDoesNotContainPersonalInformationClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureIdTokenDoesNotContainPersonalInformationClaims cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureIdTokenDoesNotContainPersonalInformationClaims();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void addIdTokenClaims(String claimsJson) {
		env.putObject("id_token", JsonParser.parseString("{\"claims\":" + claimsJson + "}").getAsJsonObject());
	}

	@Test
	public void testEvaluate_noPiClaims() {
		addIdTokenClaims("{\"iss\":\"https://example.com\",\"sub\":\"ppid\",\"aud\":\"client\",\"exp\":1,\"iat\":1,\"acr\":\"urn:cds.au:cdr:2\",\"auth_time\":1,\"nonce\":\"n\"}");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_emailClaim() {
		assertThrows(ConditionError.class, () -> {
			addIdTokenClaims("{\"iss\":\"https://example.com\",\"sub\":\"ppid\",\"email\":\"user@example.com\"}");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_nameAndAddressClaims() {
		assertThrows(ConditionError.class, () -> {
			addIdTokenClaims("{\"sub\":\"ppid\",\"name\":\"Jane Doe\",\"address\":{\"country\":\"AU\"}}");
			cond.execute(env);
		});
	}

}

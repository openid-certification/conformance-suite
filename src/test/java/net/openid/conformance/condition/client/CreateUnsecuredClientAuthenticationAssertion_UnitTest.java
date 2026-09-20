package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jwt.PlainJWT;
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
public class CreateUnsecuredClientAuthenticationAssertion_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CreateUnsecuredClientAuthenticationAssertion cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CreateUnsecuredClientAuthenticationAssertion();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_producesAlgNoneJwtWithEmptySignature() throws Exception {
		JsonObject claims = new JsonObject();
		claims.addProperty("iss", "client");
		claims.addProperty("sub", "client");
		claims.addProperty("aud", "https://as.example.com");
		env.putObject("client_assertion_claims", claims);

		cond.execute(env);

		String jwt = env.getString("client_assertion");
		assertThat(jwt).endsWith(".");
		PlainJWT parsed = PlainJWT.parse(jwt);
		assertThat(parsed.getHeader().getAlgorithm()).isEqualTo(Algorithm.NONE);
		assertThat(parsed.getJWTClaimsSet().getSubject()).isEqualTo("client");
	}

	@Test
	public void testEvaluate_missingClaims() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}

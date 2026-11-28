package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidateFederationUrl_UnitTest {

	private final Environment env = new Environment();

	private ValidateFederationUrl cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateFederationUrl();
		cond.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), Condition.ConditionResult.INFO);
	}

	@Test
	public void rejectsAQueryComponent() {
		env.putString("federation_endpoint_url", "https://op.example.org?tenant=a");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("must not contain query parameters"), e.getMessage());
	}
}

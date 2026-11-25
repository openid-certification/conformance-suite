package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidateFederationEndpointUrl_UnitTest {

	private final Environment env = new Environment();

	private ValidateFederationEndpointUrl cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateFederationEndpointUrl();
		cond.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), Condition.ConditionResult.INFO);
	}

	@Test
	public void acceptsPortPathAndQueryComponents() {
		env.putString("federation_endpoint_url", "https://ta.example.org:8443/federation/fetch?tenant=a");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void rejectsAFragmentComponentAlsoWhenAQueryIsPresent() {
		env.putString("federation_endpoint_url", "https://ta.example.org/fetch?tenant=a#part");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("must not contain a fragment component"), e.getMessage());
	}

	@Test
	public void rejectsTheHttpScheme() {
		env.putString("federation_endpoint_url", "http://ta.example.org/fetch");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("must use the https scheme"), e.getMessage());
	}
}

package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class GeneratePoppyDelegatedKey_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private GeneratePoppyDelegatedKey cond;

	@BeforeEach
	public void setUp() {
		cond = new GeneratePoppyDelegatedKey();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void generatesEs256KeyWithThumbprintKid() throws Exception {
		cond.execute(env);
		com.nimbusds.jose.jwk.ECKey key = com.nimbusds.jose.jwk.ECKey.parse(env.getObject("poppy_delegated_key").toString());
		assertTrue(key.isPrivate());
		assertEquals(com.nimbusds.jose.jwk.Curve.P_256, key.getCurve());
		assertEquals(com.nimbusds.jose.JWSAlgorithm.ES256, key.getAlgorithm());
		assertNull(key.getKeyUse());
		assertEquals(key.computeThumbprint().toString(), key.getKeyID());
	}

	@Test
	public void twoCallsDiffer() {
		cond.execute(env);
		String first = env.getObject("poppy_delegated_key").toString();
		cond.execute(env);
		assertNotEquals(first, env.getObject("poppy_delegated_key").toString());
	}
}

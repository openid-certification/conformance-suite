package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarnPDPPublishesNoMetadata_UnitTest {

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnPDPPublishesNoMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnPDPPublishesNoMetadata();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env.putObject("config", JsonParser.parseString("{ \"pdp\": { \"policy_decision_point\": \"https://pdp.example.com/tenant\" } }").getAsJsonObject());
	}

	@Test
	public void metadataNotFound_isFlaggedPointingToThePdpIdentifier() {
		env.putBoolean(GetPDPDynamicServerConfiguration.METADATA_NOT_FOUND, true);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("publishes no metadata"), e.getMessage());
		assertTrue(e.getMessage().contains("check the path of the 'PDP Identifier' in the test configuration"), e.getMessage());
	}

	@Test
	public void metadataFound_succeeds() {
		cond.execute(env);
	}
}

package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
public class VCIDetectTypeMetadataExtends_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIDetectTypeMetadataExtends cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIDetectTypeMetadataExtends();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void putTypeMetadata(String json) {
		JsonObject vci = new JsonObject();
		vci.add("sdjwt_vc_type_metadata", JsonParser.parseString(json).getAsJsonObject());
		env.putObject("vci", vci);
	}

	@Test
	public void noExtends_setsReadyFlag() {
		putTypeMetadata("{\"name\":\"PID\",\"schema\":{\"type\":\"object\"}}");
		cond.execute(env);
		assertEquals("true", env.getString("vci", "sdjwt_vc_type_metadata_chain_ready"));
	}

	@Test
	public void extendsPresent_doesNotSetReadyFlag() {
		putTypeMetadata("{\"name\":\"PID\",\"extends\":\"https://example.com/base\"}");
		cond.execute(env);
		assertNull(env.getString("vci", "sdjwt_vc_type_metadata_chain_ready"));
	}
}

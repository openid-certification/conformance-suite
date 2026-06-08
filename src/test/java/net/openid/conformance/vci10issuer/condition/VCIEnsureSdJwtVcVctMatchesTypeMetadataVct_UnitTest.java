package net.openid.conformance.vci10issuer.condition;

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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VCIEnsureSdJwtVcVctMatchesTypeMetadataVct_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIEnsureSdJwtVcVctMatchesTypeMetadataVct cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureSdJwtVcVctMatchesTypeMetadataVct();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void put(String sdjwtVct, String typeMetadataVct) {
		JsonObject sdjwt = JsonParser.parseString("{\"credential\":{\"claims\":{\"vct\":\"" + sdjwtVct + "\"}}}").getAsJsonObject();
		env.putObject("sdjwt", sdjwt);
		JsonObject vci = new JsonObject();
		JsonObject tm = new JsonObject();
		tm.addProperty("vct", typeMetadataVct);
		vci.add("sdjwt_vc_type_metadata", tm);
		env.putObject("vci", vci);
	}

	@Test
	public void matchingVct_passes() {
		put("https://example.com/pid", "https://example.com/pid");
		cond.execute(env);
	}

	@Test
	public void mismatchedVct_fails() {
		put("https://example.com/pid", "https://example.com/other");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not match"));
	}

	@Test
	public void missingTypeMetadataVct_fails() {
		JsonObject sdjwt = JsonParser.parseString("{\"credential\":{\"claims\":{\"vct\":\"https://example.com/x\"}}}").getAsJsonObject();
		env.putObject("sdjwt", sdjwt);
		JsonObject vci = new JsonObject();
		vci.add("sdjwt_vc_type_metadata", new JsonObject());
		env.putObject("vci", vci);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("no 'vct' property"));
	}

	@Test
	public void missingSdJwtVct_fails() {
		JsonObject sdjwt = JsonParser.parseString("{\"credential\":{\"claims\":{}}}").getAsJsonObject();
		env.putObject("sdjwt", sdjwt);
		JsonObject vci = new JsonObject();
		JsonObject tm = new JsonObject();
		tm.addProperty("vct", "https://example.com/x");
		vci.add("sdjwt_vc_type_metadata", tm);
		env.putObject("vci", vci);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("no 'vct' claim"));
	}
}

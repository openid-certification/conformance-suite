package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCICheckForOldSdJwtFormatInCredentialConfigurations_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private VCICheckForOldSdJwtFormatInCredentialConfigurations cond;

	@BeforeEach
	public void setUp() {
		cond = new VCICheckForOldSdJwtFormatInCredentialConfigurations();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void setMetadata(String format) {
		JsonObject config = new JsonObject();
		config.addProperty("format", format);

		JsonObject credConfigs = new JsonObject();
		credConfigs.add("TestCredential", config);

		JsonObject metadata = new JsonObject();
		metadata.add("credential_configurations_supported", credConfigs);

		JsonObject vci = new JsonObject();
		vci.add("credential_issuer_metadata", metadata);
		env.putObject("vci", vci);
	}

	@Test
	public void testEvaluate_newFormat() {
		setMetadata("dc+sd-jwt");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_mdocFormat() {
		setMetadata("mso_mdoc");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_oldFormat() {
		assertThrows(ConditionError.class, () -> {
			setMetadata("vc+sd-jwt");
			cond.execute(env);
		});
	}
}

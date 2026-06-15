package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class EnsureMdocDocTypeMatchesCredentialConfiguration_UnitTest {

	private EnsureMdocDocTypeMatchesCredentialConfiguration cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new EnsureMdocDocTypeMatchesCredentialConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private void putCredential(String docType) {
		env.putString("mdoc_doctype", docType);
	}

	private void putCredentialConfiguration(String doctype) {
		JsonObject config = new JsonObject();
		config.addProperty("format", "mso_mdoc");
		if (doctype != null) {
			config.addProperty("doctype", doctype);
		}
		env.putObject("vci_credential_configuration", config);
	}

	@Test
	public void testEvaluate_passesWhenDocTypeMatches() throws Exception {
		putCredential("org.iso.18013.5.1.mDL");
		putCredentialConfiguration("org.iso.18013.5.1.mDL");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenDocTypeDiffers() throws Exception {
		putCredential("eu.europa.ec.eudi.pid.1");
		putCredentialConfiguration("org.iso.18013.5.1.mDL");

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not match"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenConfigurationHasNoDoctype() throws Exception {
		putCredential("org.iso.18013.5.1.mDL");
		putCredentialConfiguration(null);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("doctype"), e.getMessage());
	}
}

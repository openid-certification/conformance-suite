package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

@ExtendWith(MockitoExtension.class)
public class EnsureMdocDeviceSignedElementsEmpty_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureMdocDeviceSignedElementsEmpty cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureMdocDeviceSignedElementsEmpty();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void setupEnvironment(JsonObject deviceSignedElements) {
		JsonObject mdoc = new JsonObject();
		mdoc.addProperty("docType", "org.iso.18013.5.1.mDL");
		if (deviceSignedElements != null) {
			mdoc.add("device_signed_elements", deviceSignedElements);
		}
		env.putObject("mdoc", mdoc);
	}

	@Test
	public void testEvaluate_emptyDeviceSignedElementsPasses() {
		setupEnvironment(new JsonObject());

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingDeviceSignedElementsThrowsError() {
		setupEnvironment(null);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_deviceSignedElementThrowsError() {
		JsonObject deviceSigned = new JsonObject();
		JsonArray elements = new JsonArray();
		elements.add("given_name");
		deviceSigned.add("org.iso.18013.5.1", elements);
		setupEnvironment(deviceSigned);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_deviceSignedNamespaceWithoutElementsThrowsError() {
		JsonObject deviceSigned = new JsonObject();
		deviceSigned.add("org.iso.18013.5.1", new JsonArray());
		setupEnvironment(deviceSigned);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}

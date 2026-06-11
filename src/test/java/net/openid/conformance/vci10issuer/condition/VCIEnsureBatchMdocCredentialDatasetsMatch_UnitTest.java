package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.testapp.VciMdocUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCIEnsureBatchMdocCredentialDatasetsMatch_UnitTest {

	private VCIEnsureBatchMdocCredentialDatasetsMatch cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureBatchMdocCredentialDatasetsMatch();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private String generateDeviceKeyJwk() throws Exception {
		return new ECKeyGenerator(Curve.P_256).generate().toJSONString();
	}

	private void putCredentials(String... credentials) {
		JsonArray list = new JsonArray();
		for (String credential : credentials) {
			list.add(credential);
		}
		JsonObject extracted = new JsonObject();
		extracted.add("list", list);
		env.putObject("extracted_credentials", extracted);
	}

	@Test
	public void testEvaluate_passesWhenOnlyDeviceKeysDiffer() throws Exception {
		putCredentials(
			VciMdocUtils.createMdocCredential(generateDeviceKeyJwk(), "org.iso.18013.5.1.mDL", null),
			VciMdocUtils.createMdocCredential(generateDeviceKeyJwk(), "org.iso.18013.5.1.mDL", null));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenDocTypeAndElementsDiffer() throws Exception {
		putCredentials(
			VciMdocUtils.createMdocCredential(generateDeviceKeyJwk(), "org.iso.18013.5.1.mDL", null),
			VciMdocUtils.createMdocCredential(generateDeviceKeyJwk(), "eu.europa.ec.eudi.pid.1", null));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenCredentialIsNotAnMdoc() {
		putCredentials("bm90LWNib3I", "bm90LWNib3I");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenNoCredentials() {
		putCredentials();

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}

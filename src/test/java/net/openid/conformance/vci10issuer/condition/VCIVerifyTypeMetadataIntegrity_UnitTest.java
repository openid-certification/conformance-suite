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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VCIVerifyTypeMetadataIntegrity_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIVerifyTypeMetadataIntegrity cond;

	private static final String SAMPLE_BODY = "{\"vct\":\"https://example.com/pid\",\"name\":\"PID\"}";

	@BeforeEach
	public void setUp() {
		cond = new VCIVerifyTypeMetadataIntegrity();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private static String sha256Base64(String body) throws NoSuchAlgorithmException {
		byte[] hash = MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(hash);
	}

	private void putSdjwt(String integrityValue) {
		JsonObject sdjwt;
		if (integrityValue == null) {
			sdjwt = JsonParser.parseString("{\"credential\":{\"claims\":{\"vct\":\"https://example.com/pid\"}}}").getAsJsonObject();
		} else {
			JsonObject claims = new JsonObject();
			claims.addProperty("vct", "https://example.com/pid");
			claims.addProperty("vct#integrity", integrityValue);
			JsonObject credential = new JsonObject();
			credential.add("claims", claims);
			sdjwt = new JsonObject();
			sdjwt.add("credential", credential);
		}
		env.putObject("sdjwt", sdjwt);
	}

	private void putVci(String body) {
		JsonObject vci = new JsonObject();
		vci.addProperty("sdjwt_vc_type_metadata_url", "https://example.com/pid");
		JsonObject response = new JsonObject();
		response.addProperty("body", body);
		vci.add("sdjwt_vc_type_metadata_endpoint_response", response);
		env.putObject("vci", vci);
	}

	@Test
	public void integrityAbsent_passes() {
		putSdjwt(null);
		putVci(SAMPLE_BODY);
		cond.execute(env);
	}

	@Test
	public void integrityMatches_passes() throws NoSuchAlgorithmException {
		putSdjwt("sha256-" + sha256Base64(SAMPLE_BODY));
		putVci(SAMPLE_BODY);
		cond.execute(env);
	}

	@Test
	public void integrityMismatch_fails() throws NoSuchAlgorithmException {
		putSdjwt("sha256-" + sha256Base64("different body"));
		putVci(SAMPLE_BODY);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not match"));
	}

	@Test
	public void integrityMalformed_fails() {
		putSdjwt("not-a-valid-sri-string");
		putVci(SAMPLE_BODY);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("malformed"));
	}

	@Test
	public void unsupportedAlgorithm_fails() {
		putSdjwt("md5-abc");
		putVci(SAMPLE_BODY);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("malformed"));
	}
}

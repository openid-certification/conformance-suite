package net.openid.conformance.condition.client;

import com.nimbusds.jose.util.Base64URL;
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
import org.multipaz.testapp.VciMdocUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class FetchMdocRevocationList_UnitTest {

	private static final String DEVICE_KEY_JWK = """
		{
			"kty": "EC",
			"crv": "P-256",
			"x": "cwYyuS94hcOtcPlrMMtGtflCfbZUwz5Mf1Gfa2m0AM8",
			"y": "KB7sJkFQyB8jZHO9vmWS5LNECL4id3OJO9HX9ChNonA"
		}
		""";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private TestableFetchMdocRevocationList cond;

	@BeforeEach
	public void setUp() {
		cond = new TestableFetchMdocRevocationList();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_skipsAndClearsStateWhenMsoHasNoStatus() {
		env.putString(AbstractRevocationListCwtCondition.ENV_TOKEN, "stale");
		env.putString(AbstractRevocationListCwtCondition.ENV_URI, "https://stale.example.com/1");
		env.putInteger(AbstractRevocationListCwtCondition.ENV_STATUS_LIST_IDX, 7);

		putCredentialWithStatusList(null, null);

		cond.execute(env);

		assertNull(env.getString(AbstractRevocationListCwtCondition.ENV_TOKEN));
		assertNull(env.getString(AbstractRevocationListCwtCondition.ENV_URI));
		assertNull(env.getInteger(AbstractRevocationListCwtCondition.ENV_STATUS_LIST_IDX));
		assertFalse(env.containsObject(AbstractRevocationListCwtCondition.ENV_RESPONSE));
	}

	@Test
	public void testEvaluate_storesTokenAndStatusListReference() throws Exception {
		byte[] token = StatusListCwtTestFixtures.validStatusListToken();
		cond.setResponse(ResponseEntity.ok(token));

		putCredentialWithStatusList(StatusListCwtTestFixtures.DEFAULT_URI, 4L);

		cond.execute(env);

		assertEquals(StatusListCwtTestFixtures.DEFAULT_URI,
			env.getString(AbstractRevocationListCwtCondition.ENV_URI));
		assertEquals(4, env.getInteger(AbstractRevocationListCwtCondition.ENV_STATUS_LIST_IDX).intValue());
		assertArrayEquals(token, Base64.getDecoder().decode(
			env.getString(AbstractRevocationListCwtCondition.ENV_TOKEN)));
		assertTrue(env.containsObject(AbstractRevocationListCwtCondition.ENV_RESPONSE));
	}

	@Test
	public void testEvaluate_failsOnNon2xxButKeepsResponse() {
		cond.setResponse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(new byte[0]));

		putCredentialWithStatusList(StatusListCwtTestFixtures.DEFAULT_URI, 0L);

		assertThrows(ConditionError.class, () -> cond.execute(env));

		assertTrue(env.containsObject(AbstractRevocationListCwtCondition.ENV_RESPONSE));
		assertNull(env.getString(AbstractRevocationListCwtCondition.ENV_TOKEN));
	}

	@Test
	public void testEvaluate_failsOnEmptyBody() {
		cond.setResponse(ResponseEntity.ok(new byte[0]));

		putCredentialWithStatusList(StatusListCwtTestFixtures.DEFAULT_URI, 0L);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putCredentialWithStatusList(String statusListUri, Long statusListIndex) {
		putCredential(VciMdocUtils.createMdocCredential(
			DEVICE_KEY_JWK, "org.iso.18013.5.1.mDL", null, null, statusListUri, statusListIndex));
	}

	private void putCredential(String mdocBase64Url) {
		env.putString("mdoc_credential_cbor",
			Base64.getEncoder().encodeToString(new Base64URL(mdocBase64Url).decode()));
	}

	private static class TestableFetchMdocRevocationList extends FetchMdocRevocationList {
		private ResponseEntity<byte[]> response;

		void setResponse(ResponseEntity<byte[]> response) {
			this.response = response;
		}

		@Override
		protected ResponseEntity<byte[]> getRevocationList(Environment env, String uri, String acceptContentType) {
			return response;
		}
	}
}

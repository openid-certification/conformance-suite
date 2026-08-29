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
public class FetchMdocStatusListToken_UnitTest {

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

	private TestableFetchMdocStatusListToken cond;

	@BeforeEach
	public void setUp() {
		cond = new TestableFetchMdocStatusListToken();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_skipsAndClearsStateWhenMsoHasNoStatus() {
		env.putString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_TOKEN, "stale");
		env.putString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_URI, "https://stale.example.com/1");
		env.putInteger(AbstractStatusListCwtCondition.ENV_STATUS_LIST_IDX, 7);

		putCredential(null, null);

		cond.execute(env);

		assertNull(env.getString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_TOKEN));
		assertNull(env.getString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_URI));
		assertNull(env.getInteger(AbstractStatusListCwtCondition.ENV_STATUS_LIST_IDX));
		assertFalse(env.containsObject(AbstractStatusListCwtCondition.ENV_STATUS_LIST_RESPONSE));
	}

	@Test
	public void testEvaluate_storesTokenAndStatusReference() throws Exception {
		byte[] token = StatusListCwtTestFixtures.validStatusListToken();
		cond.setResponse(ResponseEntity.ok(token));

		putCredential(StatusListCwtTestFixtures.DEFAULT_URI, 4L);

		cond.execute(env);

		assertEquals(StatusListCwtTestFixtures.DEFAULT_URI,
			env.getString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_URI));
		assertEquals(4, env.getInteger(AbstractStatusListCwtCondition.ENV_STATUS_LIST_IDX).intValue());
		assertArrayEquals(token, Base64.getDecoder().decode(
			env.getString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_TOKEN)));
		assertTrue(env.containsObject(AbstractStatusListCwtCondition.ENV_STATUS_LIST_RESPONSE));
	}

	@Test
	public void testEvaluate_failsOnNon2xxButKeepsResponse() {
		cond.setResponse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(new byte[0]));

		putCredential(StatusListCwtTestFixtures.DEFAULT_URI, 0L);

		assertThrows(ConditionError.class, () -> cond.execute(env));

		assertTrue(env.containsObject(AbstractStatusListCwtCondition.ENV_STATUS_LIST_RESPONSE));
		assertNull(env.getString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_TOKEN));
	}

	@Test
	public void testEvaluate_failsOnEmptyBody() {
		cond.setResponse(ResponseEntity.ok(new byte[0]));

		putCredential(StatusListCwtTestFixtures.DEFAULT_URI, 0L);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putCredential(String statusListUri, Long statusListIndex) {
		String mdocBase64Url = VciMdocUtils.createMdocCredential(
			DEVICE_KEY_JWK, "org.iso.18013.5.1.mDL", null, null, statusListUri, statusListIndex);
		env.putString("mdoc_credential_cbor",
			Base64.getEncoder().encodeToString(new Base64URL(mdocBase64Url).decode()));
	}

	private static class TestableFetchMdocStatusListToken extends FetchMdocStatusListToken {
		private ResponseEntity<byte[]> response;

		void setResponse(ResponseEntity<byte[]> response) {
			this.response = response;
		}

		@Override
		protected ResponseEntity<byte[]> fetchStatusListToken(Environment env, String uri) {
			return response;
		}
	}
}

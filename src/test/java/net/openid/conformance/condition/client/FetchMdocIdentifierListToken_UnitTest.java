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
public class FetchMdocIdentifierListToken_UnitTest {

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

	private TestableFetchMdocIdentifierListToken cond;

	@BeforeEach
	public void setUp() {
		cond = new TestableFetchMdocIdentifierListToken();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_skipsAndClearsStateWhenMsoHasNoStatus() {
		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_TOKEN, "stale");
		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_URI,
			"https://stale.example.com/1");
		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_ID, "AAAA");

		putCredentialWithIdentifierList(null, null);

		cond.execute(env);

		assertNull(env.getString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_TOKEN));
		assertNull(env.getString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_URI));
		assertNull(env.getString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_ID));
		assertFalse(env.containsObject(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_RESPONSE));
	}

	@Test
	public void testEvaluate_skipsWhenTheMsoUsesTheStatusListMechanism() throws Exception {
		cond.setResponse(ResponseEntity.ok(IdentifierListCwtTestFixtures.validIdentifierListToken()));

		String mdocBase64Url = VciMdocUtils.createMdocCredential(DEVICE_KEY_JWK,
			"org.iso.18013.5.1.mDL", null, null, StatusListCwtTestFixtures.DEFAULT_URI, 4L, null, null);
		env.putString("mdoc_credential_cbor",
			Base64.getEncoder().encodeToString(new Base64URL(mdocBase64Url).decode()));

		cond.execute(env);

		assertNull(env.getString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_TOKEN));
	}

	@Test
	public void testEvaluate_storesTokenAndIdentifierReference() throws Exception {
		byte[] token = IdentifierListCwtTestFixtures.validIdentifierListToken();
		cond.setResponse(ResponseEntity.ok(token));

		putCredentialWithIdentifierList(IdentifierListCwtTestFixtures.DEFAULT_URI,
			IdentifierListCwtTestFixtures.LISTED_IDENTIFIER);

		cond.execute(env);

		assertEquals(IdentifierListCwtTestFixtures.DEFAULT_URI,
			env.getString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_URI));
		assertArrayEquals(IdentifierListCwtTestFixtures.LISTED_IDENTIFIER, Base64.getDecoder().decode(
			env.getString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_ID)));
		assertArrayEquals(token, Base64.getDecoder().decode(
			env.getString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_TOKEN)));
		assertTrue(env.containsObject(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_RESPONSE));
	}

	@Test
	public void testEvaluate_failsOnNon2xxButKeepsResponse() {
		cond.setResponse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(new byte[0]));

		putCredentialWithIdentifierList(IdentifierListCwtTestFixtures.DEFAULT_URI,
			IdentifierListCwtTestFixtures.LISTED_IDENTIFIER);

		assertThrows(ConditionError.class, () -> cond.execute(env));

		assertTrue(env.containsObject(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_RESPONSE));
		assertNull(env.getString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_TOKEN));
	}

	@Test
	public void testEvaluate_failsOnEmptyBody() {
		cond.setResponse(ResponseEntity.ok(new byte[0]));

		putCredentialWithIdentifierList(IdentifierListCwtTestFixtures.DEFAULT_URI,
			IdentifierListCwtTestFixtures.LISTED_IDENTIFIER);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putCredentialWithIdentifierList(String uri, byte[] identifier) {
		String mdocBase64Url = VciMdocUtils.createMdocCredential(DEVICE_KEY_JWK,
			"org.iso.18013.5.1.mDL", null, null, null, null, uri, identifier);
		env.putString("mdoc_credential_cbor",
			Base64.getEncoder().encodeToString(new Base64URL(mdocBase64Url).decode()));
	}

	private static class TestableFetchMdocIdentifierListToken extends FetchMdocIdentifierListToken {
		private ResponseEntity<byte[]> response;

		void setResponse(ResponseEntity<byte[]> response) {
			this.response = response;
		}

		@Override
		protected ResponseEntity<byte[]> fetchIdentifierListToken(Environment env, String uri) {
			return response;
		}
	}
}

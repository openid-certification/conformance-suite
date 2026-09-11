package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.oauth.statuslists.StatusListCwt;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborDouble;
import org.multipaz.cbor.DataItemExtensionsKt;
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.Tstr;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateMdocRevocationListCwtFormat_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateMdocRevocationListCwtFormat cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateMdocRevocationListCwtFormat();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		useStatusList();
	}

	@Test
	public void testEvaluate_acceptsWellFormedToken() throws Exception {
		putToken(StatusListCwtTestFixtures.validStatusListToken());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsDisallowedSignatureAlgorithm() throws Exception {
		putToken(StatusListCwtTestFixtures.statusListTokenWithDisallowedAlgorithm());

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("-257"), error.getMessage());
	}

	@Test
	public void testEvaluate_rejectsX5chainInUnprotectedHeader() throws Exception {
		putToken(StatusListCwtTestFixtures.statusListTokenWithX5chainInUnprotectedHeader());

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("x5chain"), error.getMessage());
	}

	@Test
	public void testEvaluate_rejectsUntaggedCoseSign1() throws Exception {
		byte[] tagged = StatusListCwtTestFixtures.validStatusListToken();
		byte[] untagged = Cbor.INSTANCE.encode(
			((Tagged) Cbor.INSTANCE.decode(tagged)).getTaggedItem());
		putToken(untagged);

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("COSE_Sign1 tag"), error.getMessage());
	}

	@Test
	public void testEvaluate_rejectsSubjectThatDoesNotMatchTheStatusListUri() throws Exception {
		putToken(StatusListCwtTestFixtures.statusListToken("https://elsewhere.example.com/statuslists/9"));

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("sub claim"), error.getMessage());
	}

	@Test
	public void testEvaluate_acceptsTheRegisteredCoapContentFormatIdAsType() throws Exception {
		putToken(StatusListCwtTestFixtures.statusListTokenWithType(
			DataItemExtensionsKt.toDataItem(StatusListCwt.COAP_CONTENT_FORMAT_ID)));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsAnotherCoapContentFormatIdAsType() throws Exception {
		putToken(StatusListCwtTestFixtures.statusListTokenWithType(DataItemExtensionsKt.toDataItem(0)));

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("279"), error.getMessage());
	}

	@Test
	public void testEvaluate_acceptsAFloatingPointExp() throws Exception {
		// RFC 8392 NumericDate values need not be integers
		putToken(StatusListCwtTestFixtures.statusListTokenWithClaim(StatusListCwt.CLAIM_EXP,
			new CborDouble(Instant.now().getEpochSecond() + 600.5)));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsANonNumericIat() throws Exception {
		putToken(StatusListCwtTestFixtures.statusListTokenWithClaim(StatusListCwt.CLAIM_IAT,
			new Tstr("not a timestamp")));

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("iat claim"), error.getMessage());
	}

	@Test
	public void testEvaluate_rejectsANonPositiveTtl() throws Exception {
		putToken(StatusListCwtTestFixtures.statusListTokenWithClaim(StatusListCwt.CLAIM_TTL,
			DataItemExtensionsKt.toDataItem(0)));

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("ttl claim"), error.getMessage());
	}

	@Test
	public void testEvaluate_acceptsAWellFormedIdentifierList() throws Exception {
		useIdentifierList();
		putToken(IdentifierListCwtTestFixtures.validIdentifierListToken());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_acceptsAnEmptyIdentifiersMap() throws Exception {
		useIdentifierList();
		putToken(IdentifierListCwtTestFixtures.emptyIdentifierListToken());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsTheStatusListMediaTypeInAnIdentifierListTypeHeader() throws Exception {
		useIdentifierList();
		putToken(IdentifierListCwtTestFixtures.identifierListTokenWithStatusListType());

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("application/identifierlist+cwt"), error.getMessage());
	}

	@Test
	public void testEvaluate_rejectsAStatusListClaimAlongsideTheIdentifierList() throws Exception {
		useIdentifierList();
		putToken(IdentifierListCwtTestFixtures.identifierListTokenWithStatusListClaim());

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("StatusList claim (key 65533)"), error.getMessage());
	}

	@Test
	public void testEvaluate_rejectsAMissingIdentifierListClaim() throws Exception {
		useIdentifierList();
		putToken(IdentifierListCwtTestFixtures.identifierListTokenWithoutIdentifierListClaim());

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("IdentifierList claim (key 65530)"), error.getMessage());
	}

	@Test
	public void testEvaluate_rejectsAnIdentifierListSubClaimThatDoesNotMatchTheMsoReference() throws Exception {
		useIdentifierList();
		putToken(IdentifierListCwtTestFixtures.validIdentifierListToken());
		env.putString(AbstractRevocationListCwtCondition.ENV_URI,
			"https://elsewhere.example.com/identifierlists/9");

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("sub claim (key 2)"), error.getMessage());
	}

	private void useStatusList() {
		env.putString(AbstractRevocationListCwtCondition.ENV_MECHANISM, "status_list");
		env.putString(AbstractRevocationListCwtCondition.ENV_URI, StatusListCwtTestFixtures.DEFAULT_URI);
	}

	private void useIdentifierList() {
		env.putString(AbstractRevocationListCwtCondition.ENV_MECHANISM, "identifier_list");
		env.putString(AbstractRevocationListCwtCondition.ENV_URI, IdentifierListCwtTestFixtures.DEFAULT_URI);
	}

	private void putToken(byte[] token) {
		env.putString(AbstractRevocationListCwtCondition.ENV_TOKEN, StatusListCwtTestFixtures.encode(token));
	}
}

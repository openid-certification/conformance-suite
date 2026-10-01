package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.MdlDataElements;
import net.openid.conformance.util.PidDataElements;
import net.openid.conformance.vp1finalwallet.VP1FinalWalletCredentialFormat;
import net.openid.conformance.vp1finalwallet.VP1FinalWalletCredentialType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.DataItemExtensionsKt;
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.Tstr;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class PidMdocDataElements_UnitTest {

	private EnsureMdocPidMandatoryDataElementsPresent mandatory;

	private EnsureIssuedMdocPidElementsAreDefined issuedDefined;

	private EnsurePresentedMdocPidElementsAreDefined presentedDefined;

	private EnsureMdocPidElementValuesAreValid values;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		mandatory = new EnsureMdocPidMandatoryDataElementsPresent();
		mandatory.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		issuedDefined = new EnsureIssuedMdocPidElementsAreDefined();
		issuedDefined.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		presentedDefined = new EnsurePresentedMdocPidElementsAreDefined();
		presentedDefined.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		values = new EnsureMdocPidElementValuesAreValid();
		values.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private byte[] pidCredential() throws Exception {
		return MdocCredentialTestUtil.createCredentialBytes(PidDataElements.PID_DOCTYPE);
	}

	private void putCredential(byte[] issuerSignedBytes, String docType) {
		MdocCredentialTestUtil.putCredential(env, issuerSignedBytes);
		env.putString("mdoc_doctype", docType);
	}

	private void putPresentedMdoc(String docType, String namespace, String... elements) {
		JsonArray array = new JsonArray();
		for (String element : elements) {
			array.add(element);
		}
		JsonObject disclosed = new JsonObject();
		disclosed.add(namespace, array);
		JsonObject mdoc = new JsonObject();
		mdoc.addProperty("docType", docType);
		mdoc.add("disclosed_elements", disclosed);
		env.putObject("mdoc", mdoc);
	}

	@Test
	public void testAll_passForCredentialTheSuiteIssues() throws Exception {
		putCredential(pidCredential(), PidDataElements.PID_DOCTYPE);

		assertDoesNotThrow(() -> mandatory.execute(env));
		assertDoesNotThrow(() -> issuedDefined.execute(env));
		assertDoesNotThrow(() -> values.execute(env));
	}

	@Test
	public void testAll_doNotApplyToOtherDocTypes() throws Exception {
		putCredential(MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE),
			MdlDataElements.MDL_DOCTYPE);

		assertDoesNotThrow(() -> mandatory.execute(env));
		assertDoesNotThrow(() -> issuedDefined.execute(env));
		assertDoesNotThrow(() -> values.execute(env));
	}

	@Test
	public void testMandatory_failsWhenEachMandatoryElementMissing() throws Exception {
		for (String element : PidDataElements.MANDATORY_ELEMENTS) {
			putCredential(MdocCredentialTestUtil.removeElement(pidCredential(),
				PidDataElements.PID_NAMESPACE, element), PidDataElements.PID_DOCTYPE);

			ConditionError e = assertThrows(ConditionError.class, () -> mandatory.execute(env), element);
			assertTrue(e.getMessage().contains("mandatory"), e.getMessage());
		}
	}

	/** Mandatory inclusion of the portrait is deferred, and the user may opt out of it. */
	@Test
	public void testMandatory_passesWithoutPortrait() throws Exception {
		putCredential(MdocCredentialTestUtil.removeElement(pidCredential(),
			PidDataElements.PID_NAMESPACE, "portrait"), PidDataElements.PID_DOCTYPE);

		assertDoesNotThrow(() -> mandatory.execute(env));
	}

	@Test
	public void testIssuedDefined_failsForAttributeTheRulebookRemoved() throws Exception {
		putCredential(MdocCredentialTestUtil.addElement(pidCredential(), PidDataElements.PID_NAMESPACE,
			"age_over_18", DataItemExtensionsKt.toDataItem(true)), PidDataElements.PID_DOCTYPE);

		ConditionError e = assertThrows(ConditionError.class, () -> issuedDefined.execute(env));
		assertTrue(e.getMessage().contains("does not define"), e.getMessage());
	}

	@Test
	public void testIssuedDefined_allowsDomesticNamespace() throws Exception {
		putCredential(MdocCredentialTestUtil.addElement(pidCredential(), "eu.europa.ec.eudi.pid.de.1",
			"anything_the_member_state_likes", new Tstr("value")), PidDataElements.PID_DOCTYPE);

		assertDoesNotThrow(() -> issuedDefined.execute(env));
	}

	@Test
	public void testPresentedDefined_passesForDefinedElements() {
		putPresentedMdoc(PidDataElements.PID_DOCTYPE, PidDataElements.PID_NAMESPACE,
			"family_name", "place_of_birth", "nationality", "sex", "trust_anchor",
			"attestation_legal_category");

		assertDoesNotThrow(() -> presentedDefined.execute(env));
	}

	/** Identifiers ARF 1.4 defined that the current Rulebook does not. */
	@Test
	public void testPresentedDefined_failsForAttributesTheRulebookRemoved() {
		for (String element : new String[] { "age_over_18", "age_in_years", "birth_place", "gender",
				"resident_house_number" }) {
			putPresentedMdoc(PidDataElements.PID_DOCTYPE, PidDataElements.PID_NAMESPACE, element);

			assertThrows(ConditionError.class, () -> presentedDefined.execute(env), element);
		}
	}

	@Test
	public void testPresentedDefined_passesForOtherDocType() {
		putPresentedMdoc(MdlDataElements.MDL_DOCTYPE, MdlDataElements.MDL_NAMESPACE, "age_over_18");

		assertDoesNotThrow(() -> presentedDefined.execute(env));
	}

	/**
	 * The emulated wallet's PID must be able to answer the built-in query for every mandatory
	 * attribute with values these checks accept.
	 */
	@Test
	public void testPresented_emulatedWalletAnswersAllMandatoryQuery() {
		LoadBuiltInDcqlQuery load = new LoadBuiltInDcqlQuery();
		load.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env.putString(LoadBuiltInDcqlQuery.RESOURCE_ENV_KEY, VP1FinalWalletCredentialType.EUDI_PID
			.getAllMandatoryClaimsDcqlResource(VP1FinalWalletCredentialFormat.ISO_MDL));
		load.execute(env);
		MdocCredentialTestUtil.putPresentedCredential(env, eventLog);

		Set<String> disclosed = new TreeSet<>();
		for (JsonElement element : env.getElementFromObject("mdoc", "disclosed_elements")
				.getAsJsonObject().getAsJsonArray(PidDataElements.PID_NAMESPACE)) {
			disclosed.add(OIDFJSON.getString(element));
		}
		assertEquals(new TreeSet<>(PidDataElements.MANDATORY_ELEMENTS), disclosed);
		assertDoesNotThrow(() -> presentedDefined.execute(env));
		assertDoesNotThrow(() -> values.execute(env));
	}

	private void assertValueRejected(String element, DataItem value) throws Exception {
		putCredential(MdocCredentialTestUtil.replaceElementValue(pidCredential(),
			PidDataElements.PID_NAMESPACE, element, value), PidDataElements.PID_DOCTYPE);

		ConditionError e = assertThrows(ConditionError.class, () -> values.execute(env), element);
		assertTrue(e.getMessage().contains("do not match"), e.getMessage());
	}

	@Test
	public void testValues_failForWrongEncodings() throws Exception {
		// ARF 1.4 encoded these as a single text string
		assertValueRejected("nationality", new Tstr("FR"));
		assertValueRejected("place_of_birth", new Tstr("Paris"));
		// full-date is tag 1004, not a bare text string
		assertValueRejected("birth_date", new Tstr("1980-05-23"));
		assertValueRejected("issuing_country", new Tstr("FRA"));
		assertValueRejected("expiry_date",
			new Tagged(Tagged.DATE_TIME_STRING, new Tstr("2030-01-01T00:00:00+01:00")));
		assertValueRejected("family_name", new Tstr("x".repeat(151)));
	}

	@Test
	public void testValues_checkOptionalAttributes() throws Exception {
		byte[] bytes = MdocCredentialTestUtil.addElement(pidCredential(), PidDataElements.PID_NAMESPACE,
			"sex", DataItemExtensionsKt.toDataItem(5));
		bytes = MdocCredentialTestUtil.addElement(bytes, PidDataElements.PID_NAMESPACE,
			"mobile_phone_number", new Tstr("+31123456789"));
		putCredential(bytes, PidDataElements.PID_DOCTYPE);
		assertDoesNotThrow(() -> values.execute(env));

		putCredential(MdocCredentialTestUtil.addElement(pidCredential(), PidDataElements.PID_NAMESPACE,
			"sex", DataItemExtensionsKt.toDataItem(7)), PidDataElements.PID_DOCTYPE);
		assertThrows(ConditionError.class, () -> values.execute(env));

		putCredential(MdocCredentialTestUtil.addElement(pidCredential(), PidDataElements.PID_NAMESPACE,
			"mobile_phone_number", new Tstr("0031 123456789")), PidDataElements.PID_DOCTYPE);
		assertThrows(ConditionError.class, () -> values.execute(env));
	}

	/** An opted-out portrait is an empty byte string. */
	@Test
	public void testValues_acceptEmptyPortrait() throws Exception {
		putCredential(MdocCredentialTestUtil.replaceElementValue(pidCredential(),
			PidDataElements.PID_NAMESPACE, "portrait", new Bstr(new byte[0])), PidDataElements.PID_DOCTYPE);

		assertDoesNotThrow(() -> values.execute(env));
	}
}

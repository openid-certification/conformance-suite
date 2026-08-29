package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.ParseCredentialAsMdoc;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.MdocUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.mdoc.mso.MobileSecurityObject;
import org.multipaz.revocation.RevocationStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
public class CreateMdocCredential_UnitTest {

	private static final String SESSION_TRANSCRIPT =
		"g/b2gnZPcGVuSUQ0VlBEQ0FQSUhhbmRvdmVyWCBd0cMpz6ie3V5hrfH0TMRNv/K/U1jcr0o2rN+i0gMNWA==";

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CreateMdocCredential cond;

	@BeforeEach
	public void setUp() {
		cond = new CreateMdocCredential();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_createsParsableCredential() {
		env.putString("session_transcript", SESSION_TRANSCRIPT);

		cond.execute(env);

		String credential = env.getString("credential");
		assertThat(credential).isNotBlank();

		parseCredential();
	}

	@Test
	public void testEvaluate_noDcql_emitsAllDefaultMdlElements() {
		env.putString("session_transcript", SESSION_TRANSCRIPT);

		cond.execute(env);
		parseCredential();

		assertThat(env.getString("mdoc", "docType")).isEqualTo("org.iso.18013.5.1.mDL");
		assertThat(countDisclosedElements()).isGreaterThan(2);
	}

	@Test
	public void testEvaluate_noClaimsInDcqlQuery_emitsNoElements() {
		env.putString("session_transcript", SESSION_TRANSCRIPT);
		env.putObject("dcql_query", dcql("""
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "mso_mdoc",
			      "meta": {"doctype_value": "org.iso.18013.5.1.mDL"}
			    }
			  ]
			}
			"""));

		cond.execute(env);
		parseCredential();

		assertThat(env.getString("mdoc", "docType")).isEqualTo("org.iso.18013.5.1.mDL");
		assertThat(countDisclosedElements()).isZero();
	}

	@Test
	public void testEvaluate_subsetKeepsOnlyRequested() {
		env.putString("session_transcript", SESSION_TRANSCRIPT);
		env.putObject("dcql_query", dcql("""
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "mso_mdoc",
			      "meta": {"doctype_value": "org.iso.18013.5.1.mDL"},
			      "claims": [
			        {"path": ["org.iso.18013.5.1", "family_name"]},
			        {"path": ["org.iso.18013.5.1", "given_name"]}
			      ]
			    }
			  ]
			}
			"""));

		cond.execute(env);
		parseCredential();

		assertThat(env.getString("mdoc", "docType")).isEqualTo("org.iso.18013.5.1.mDL");
		assertThat(collectStrings(disclosedElementsFor("org.iso.18013.5.1")))
			.containsExactlyInAnyOrder("family_name", "given_name");
		assertThat(countDisclosedElements()).isEqualTo(2);
	}

	@Test
	public void testEvaluate_photoIdDoctype_presentsPhotoIdCredential() {
		env.putString("session_transcript", SESSION_TRANSCRIPT);
		env.putObject("dcql_query", dcql("""
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "mso_mdoc",
			      "meta": {"doctype_value": "org.iso.23220.photoid.1"},
			      "claims": [
			        {"path": ["org.iso.23220.1", "family_name"]},
			        {"path": ["org.iso.23220.1", "portrait"]}
			      ]
			    }
			  ]
			}
			"""));

		cond.execute(env);
		parseCredential();

		assertThat(env.getString("mdoc", "docType")).isEqualTo("org.iso.23220.photoid.1");
		assertThat(collectStrings(disclosedElementsFor("org.iso.23220.1")))
			.containsExactlyInAnyOrder("family_name", "portrait");
		assertThat(countDisclosedElements()).isEqualTo(2);
	}

	@Test
	public void testEvaluate_dcqlWithNoMdocCredential_fails() {
		env.putString("session_transcript", SESSION_TRANSCRIPT);
		env.putObject("dcql_query", dcql("""
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "meta": {"vct_values": ["https://credentials.example.com/identity_credential"]}
			    }
			  ]
			}
			"""));

		assertThatExceptionOfType(ConditionError.class)
			.isThrownBy(() -> cond.execute(env))
			.withMessageContaining("no credential entry with format 'mso_mdoc'");
	}

	@Test
	public void testEvaluate_mdocCredentialWithoutDoctypeValue_fails() {
		env.putString("session_transcript", SESSION_TRANSCRIPT);
		env.putObject("dcql_query", dcql("""
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "mso_mdoc"
			    }
			  ]
			}
			"""));

		assertThatExceptionOfType(ConditionError.class)
			.isThrownBy(() -> cond.execute(env))
			.withMessageContaining("meta.doctype_value");
	}

	@Test
	public void testEvaluate_noStatusElementWithoutAStatusListReference() throws Exception {
		env.putString("session_transcript", SESSION_TRANSCRIPT);

		cond.execute(env);

		assertThat(msoOfPresentedMdoc().getRevocationStatus()).isNull();
	}

	@Test
	public void testEvaluate_referencesTheStatusListWhenOneWasAllocated() throws Exception {
		env.putString("session_transcript", SESSION_TRANSCRIPT);
		env.putObject(CreateRevokedStatusListReference.ENV_KEY, dcql("""
			{"uri": "https://example.com/test/a/alias/statuslists/1", "idx": 41}"""));

		cond.execute(env);

		// ISO/IEC 18013-5 12.3.6.2: the MSO's status element references the revocation list
		RevocationStatus status = msoOfPresentedMdoc().getRevocationStatus();
		assertThat(status).isInstanceOf(RevocationStatus.StatusList.class);
		RevocationStatus.StatusList statusList = (RevocationStatus.StatusList) status;
		assertThat(statusList.getUri()).isEqualTo("https://example.com/test/a/alias/statuslists/1");
		assertThat(statusList.getIdx()).isEqualTo(41);
		// no Certificate element, so the revocation list's signer certificate must chain to the
		// CA that certified the document signer
		assertThat(statusList.getCertificate()).isNull();
	}

	private MobileSecurityObject msoOfPresentedMdoc() throws Exception {
		byte[] deviceResponse = new Base64URL(env.getString("credential")).decode();
		DataItem documents = Cbor.INSTANCE.decode(deviceResponse).getOrNull("documents");
		assertThat(documents).isNotNull();
		DataItem issuerSigned = documents.getAsArray().get(0).getOrNull("issuerSigned");
		assertThat(issuerSigned).isNotNull();
		return MdocUtil.parseMso(issuerSigned);
	}

	private void parseCredential() {
		ParseCredentialAsMdoc parseCond = new ParseCredentialAsMdoc();
		parseCond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		parseCond.execute(env);
	}

	private static JsonObject dcql(String json) {
		return JsonParser.parseString(json).getAsJsonObject();
	}

	private int countDisclosedElements() {
		JsonObject disclosed = env.getElementFromObject("mdoc", "disclosed_elements").getAsJsonObject();
		int total = 0;
		for (var entry : disclosed.entrySet()) {
			total += entry.getValue().getAsJsonArray().size();
		}
		return total;
	}

	private JsonArray disclosedElementsFor(String namespace) {
		JsonObject disclosed = env.getElementFromObject("mdoc", "disclosed_elements").getAsJsonObject();
		return disclosed.getAsJsonArray(namespace);
	}

	private static java.util.List<String> collectStrings(JsonArray array) {
		java.util.List<String> list = new java.util.ArrayList<>();
		for (JsonElement el : array) {
			list.add(OIDFJSON.getString(el));
		}
		return list;
	}
}

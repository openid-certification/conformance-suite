package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborArray;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.DiagnosticOption;
import org.multipaz.cose.CoseSign1;
import org.multipaz.mdoc.mso.MobileSecurityObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Checks that all mdoc credentials issued in a batch contain the same Credential Dataset.
 *
 * OID4VCI 1.0 Final §3.3.2: "In the context of a single request, the batch of issued
 * Credentials sent in response MUST share the same Credential Format and Credential
 * Dataset, but SHOULD contain different Cryptographic Data."
 *
 * The comparison covers the MSO docType and the data element values in all namespaces of
 * the IssuerSigned structure, ignoring per-credential Cryptographic Data: the IssuerSignedItem
 * digestID and random salt, and the MSO's valueDigests, validity timestamps, status and
 * deviceKeyInfo (the device key must differ; that is checked by a separate condition).
 */
public class VCIEnsureBatchMdocCredentialDatasetsMatch extends AbstractCondition {

	private static class MdocDataset {
		private String docType;
		// namespace -> elementIdentifier -> elementValue
		private final Map<String, Map<String, DataItem>> elements = new TreeMap<>();
	}

	@Override
	@PreEnvironment(required = "extracted_credentials")
	public Environment evaluate(Environment env) {

		JsonArray list = env.getObject("extracted_credentials").getAsJsonArray("list");
		if (list.isEmpty()) {
			throw error("No credentials found in 'extracted_credentials' - there is nothing to compare");
		}

		List<MdocDataset> datasets = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			datasets.add(extractDataset(OIDFJSON.getString(list.get(i)), i));
		}

		MdocDataset first = datasets.get(0);
		for (int i = 1; i < datasets.size(); i++) {
			MdocDataset other = datasets.get(i);
			if (!Objects.equals(first.docType, other.docType)) {
				throw error("Credentials issued in the same batch must have the same docType",
					args("credential_index", i,
						"first_credential_doctype", first.docType,
						"differing_credential_doctype", other.docType));
			}
			if (!first.elements.equals(other.elements)) {
				throw error("Credentials issued in the same batch must contain the same Credential Dataset, "
						+ "but the data elements of one credential differ from the first credential. "
						+ "(The comparison ignores digestID, random, validity timestamps and the device key, "
						+ "which may legitimately differ.)",
					args("credential_index", i,
						"first_credential_elements", datasetToJson(first),
						"differing_credential_elements", datasetToJson(other)));
			}
		}

		logSuccess("All " + datasets.size() + " credentials in the batch contain the same Credential Dataset",
			args("doctype", first.docType, "credential_dataset", datasetToJson(first)));

		return env;
	}

	private MdocDataset extractDataset(String mdocBase64Url, int index) {
		byte[] bytes;
		try {
			bytes = new Base64URL(mdocBase64Url).decode();
		} catch (Exception e) {
			throw error("Failed to decode credential as base64url", e, args("credential_index", index));
		}

		try {
			MdocDataset dataset = new MdocDataset();

			DataItem issuerSigned = Cbor.INSTANCE.decode(bytes);

			DataItem issuerAuth = issuerSigned.getOrNull("issuerAuth");
			if (issuerAuth == null) {
				throw error("mdoc credential is missing the required 'issuerAuth' field",
					args("credential_index", index));
			}
			CoseSign1 coseSign1 = issuerAuth.getAsCoseSign1();
			DataItem msoDataItem = Cbor.INSTANCE.decode(coseSign1.getPayload()).getAsTaggedEncodedCbor();
			MobileSecurityObject mso = MobileSecurityObject.Companion.fromDataItem(msoDataItem);
			dataset.docType = mso.getDocType();

			DataItem nameSpaces = issuerSigned.getOrNull("nameSpaces");
			if (nameSpaces instanceof CborMap nameSpacesMap) {
				for (Map.Entry<DataItem, DataItem> namespaceEntry : nameSpacesMap.getItems().entrySet()) {
					String namespace = namespaceEntry.getKey().getAsTstr();
					Map<String, DataItem> namespaceElements = new TreeMap<>();
					for (DataItem issuerSignedItemBytes : ((CborArray) namespaceEntry.getValue()).getItems()) {
						DataItem issuerSignedItem = issuerSignedItemBytes.getAsTaggedEncodedCbor();
						String elementIdentifier = issuerSignedItem.getOrNull("elementIdentifier").getAsTstr();
						DataItem elementValue = issuerSignedItem.getOrNull("elementValue");
						namespaceElements.put(elementIdentifier, elementValue);
					}
					dataset.elements.put(namespace, namespaceElements);
				}
			}

			return dataset;
		} catch (ConditionError e) {
			throw e;
		} catch (Exception e) {
			throw error("Failed to parse credential as an mdoc IssuerSigned structure", e,
				args("credential_index", index));
		}
	}

	private JsonObject datasetToJson(MdocDataset dataset) {
		JsonObject namespaces = new JsonObject();
		for (Map.Entry<String, Map<String, DataItem>> namespaceEntry : dataset.elements.entrySet()) {
			JsonObject elements = new JsonObject();
			for (Map.Entry<String, DataItem> elementEntry : namespaceEntry.getValue().entrySet()) {
				elements.addProperty(elementEntry.getKey(),
					Cbor.INSTANCE.toDiagnostics(elementEntry.getValue(), Set.of(DiagnosticOption.EMBEDDED_CBOR)));
			}
			namespaces.add(namespaceEntry.getKey(), elements);
		}
		return namespaces;
	}
}

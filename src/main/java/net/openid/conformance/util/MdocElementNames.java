package net.openid.conformance.util;

import com.google.gson.JsonElement;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads the data element identifiers of an mdoc out of the environment, keyed by namespace. */
public final class MdocElementNames {

	private MdocElementNames() {
		// utility class
	}

	/**
	 * From the raw IssuerSigned CBOR stored as standard base64 in 'mdoc_credential_cbor' by
	 * ParseMdocCredentialFromVCIIssuance.
	 */
	public static Map<String, List<String>> fromIssuedCredential(Environment env)
			throws MdocUtil.MdocParseException {
		Map<String, List<String>> result = new LinkedHashMap<>();
		byte[] bytes = Base64.getDecoder().decode(env.getString("mdoc_credential_cbor"));
		Map<String, List<DataItem>> namespaces = MdocUtil.getIssuerSignedItems(Cbor.INSTANCE.decode(bytes));
		for (Map.Entry<String, List<DataItem>> entry : namespaces.entrySet()) {
			List<String> elements = new ArrayList<>();
			for (DataItem issuerSignedItemBytes : entry.getValue()) {
				elements.add(issuerSignedItemBytes.getAsTaggedEncodedCbor()
					.getOrNull("elementIdentifier").getAsTstr());
			}
			result.put(entry.getKey(), elements);
		}
		return result;
	}

	/** From the 'mdoc' object stored by ParseCredentialAsMdoc. */
	public static Map<String, List<String>> fromPresentation(Environment env) {
		Map<String, List<String>> result = new LinkedHashMap<>();
		JsonElement disclosed = env.getElementFromObject("mdoc", "disclosed_elements");
		if (disclosed == null || !disclosed.isJsonObject()) {
			return result;
		}
		for (Map.Entry<String, JsonElement> entry : disclosed.getAsJsonObject().entrySet()) {
			List<String> elements = new ArrayList<>();
			for (JsonElement element : entry.getValue().getAsJsonArray()) {
				elements.add(OIDFJSON.getString(element));
			}
			result.put(entry.getKey(), elements);
		}
		return result;
	}
}

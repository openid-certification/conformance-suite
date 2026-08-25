package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks an mdoc photo ID as issued over VCI contains no data element ISO/IEC TS 23220-4 Annex C
 * does not define.
 *
 * Reads the docType stored in 'mdoc_doctype' and the raw CBOR stored as standard base64 in
 * 'mdoc_credential_cbor' by ParseMdocCredentialFromVCIIssuance.
 */
public class EnsureIssuedMdocPhotoIdElementsAreDefined extends AbstractEnsureMdocPhotoIdElementsAreDefined {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor", "mdoc_doctype" })
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}

	@Override
	protected String getDocType(Environment env) {
		return env.getString("mdoc_doctype");
	}

	@Override
	protected String getCredentialDescription() {
		return "issued";
	}

	@Override
	protected Map<String, List<String>> getElementsByNamespace(Environment env) {
		Map<String, List<String>> result = new LinkedHashMap<>();
		try {
			byte[] bytes = Base64.getDecoder().decode(env.getString("mdoc_credential_cbor"));
			Map<String, List<DataItem>> namespaces =
				MdocUtil.getIssuerSignedItems(Cbor.INSTANCE.decode(bytes));
			for (Map.Entry<String, List<DataItem>> entry : namespaces.entrySet()) {
				List<String> elements = new ArrayList<>();
				for (DataItem issuerSignedItemBytes : entry.getValue()) {
					DataItem issuerSignedItem = issuerSignedItemBytes.getAsTaggedEncodedCbor();
					elements.add(issuerSignedItem.getOrNull("elementIdentifier").getAsTstr());
				}
				result.put(entry.getKey(), elements);
			}
		} catch (MdocUtil.MdocParseException e) {
			throw error(e.getMessage(), e);
		} catch (Exception e) {
			throw error("Failed to parse the mdoc credential", e);
		}
		return result;
	}
}

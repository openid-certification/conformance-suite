package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks an mdoc photo ID as presented to a verifier contains no data element ISO/IEC TS 23220-4
 * Annex C does not define.
 *
 * Unlike the mandatory and recommended element checks this one does apply to presentations:
 * selective disclosure explains why an element is absent, but never why an undefined one is
 * present.
 *
 * Reads the 'mdoc' object stored by ParseCredentialAsMdoc.
 */
public class EnsurePresentedMdocPhotoIdElementsAreDefined extends AbstractEnsureMdocPhotoIdElementsAreDefined {

	@Override
	@PreEnvironment(required = "mdoc")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}

	@Override
	protected String getDocType(Environment env) {
		return env.getString("mdoc", "docType");
	}

	@Override
	protected String getCredentialDescription() {
		return "presented";
	}

	@Override
	protected Map<String, List<String>> getElementsByNamespace(Environment env) {
		Map<String, List<String>> result = new LinkedHashMap<>();
		JsonElement disclosed = env.getElementFromObject("mdoc", "disclosed_elements");
		if (disclosed == null || !disclosed.isJsonObject()) {
			throw error("The parsed mdoc has no disclosed elements");
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

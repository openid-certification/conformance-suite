package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocElementNames;
import net.openid.conformance.util.MdocUtil;

import java.util.List;
import java.util.Map;

/**
 * {@link AbstractEnsureMdocElementsAreDefined} for a credential as issued over VCI, reading the
 * raw IssuerSigned CBOR stored in 'mdoc_credential_cbor' and the docType stored in 'mdoc_doctype'
 * by ParseMdocCredentialFromVCIIssuance. Subclasses supply only the docType's specification data.
 */
public abstract class AbstractEnsureIssuedMdocElementsAreDefined extends AbstractEnsureMdocElementsAreDefined {

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
		try {
			return MdocElementNames.fromIssuedCredential(env);
		} catch (MdocUtil.MdocParseException e) {
			throw error(e.getMessage(), e);
		} catch (Exception e) {
			throw error("Failed to parse the mdoc credential", e);
		}
	}
}

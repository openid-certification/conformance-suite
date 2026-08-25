package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocElementNames;

import java.util.List;
import java.util.Map;

/**
 * {@link AbstractEnsureMdocElementsAreDefined} for a credential as presented to a verifier,
 * reading the 'mdoc' object stored by ParseCredentialAsMdoc. Subclasses supply only the docType's
 * specification data.
 */
public abstract class AbstractEnsurePresentedMdocElementsAreDefined extends AbstractEnsureMdocElementsAreDefined {

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
		return MdocElementNames.fromPresentation(env);
	}
}

package net.openid.conformance.condition.client;

import net.openid.conformance.util.MdocValueConstraint;
import net.openid.conformance.util.PidDataElements;

/**
 * Checks that the attribute values of an mdoc PID match the encoding and value constraints the
 * EUDI PID Rulebook defines. Applies to both an issued and a presented credential.
 */
public class EnsureMdocPidElementValuesAreValid extends AbstractEnsureMdocElementValuesAreValid {

	@Override
	protected String getExpectedDocType() {
		return PidDataElements.PID_DOCTYPE;
	}

	@Override
	protected String getCredentialName() {
		return "PID";
	}

	@Override
	protected String getSpecificationName() {
		return "the EUDI PID Rulebook";
	}

	@Override
	protected boolean isKnownNamespace(String namespace) {
		return PidDataElements.isKnownNamespace(namespace);
	}

	@Override
	protected MdocValueConstraint getValueConstraint(String namespace, String elementIdentifier) {
		return PidDataElements.getValueConstraint(namespace, elementIdentifier);
	}
}

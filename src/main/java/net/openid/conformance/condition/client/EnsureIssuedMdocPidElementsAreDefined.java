package net.openid.conformance.condition.client;

import net.openid.conformance.util.PidDataElements;

/**
 * Checks an mdoc PID as issued over VCI contains no attribute in the eu.europa.ec.eudi.pid.1
 * namespace that the EUDI PID Rulebook does not define. Attributes a Member State defines itself
 * belong in a domestic namespace, which is not checked.
 */
public class EnsureIssuedMdocPidElementsAreDefined extends AbstractEnsureIssuedMdocElementsAreDefined {

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
	protected boolean isDefined(String namespace, String elementIdentifier) {
		return PidDataElements.isDefined(namespace, elementIdentifier);
	}
}

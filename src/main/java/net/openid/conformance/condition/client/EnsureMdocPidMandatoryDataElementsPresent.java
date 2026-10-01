package net.openid.conformance.condition.client;

import net.openid.conformance.util.PidDataElements;

import java.util.Set;

/**
 * Checks that an issued mdoc credential with docType eu.europa.ec.eudi.pid.1 contains all the
 * attributes the EUDI PID Rulebook sections 2.2 and 2.4 list as mandatory, other than the
 * portrait, whose mandatory inclusion is deferred and which the user may opt out of.
 */
public class EnsureMdocPidMandatoryDataElementsPresent
		extends AbstractEnsureMdocDataElementsPresent {

	@Override
	protected String getDocType() {
		return PidDataElements.PID_DOCTYPE;
	}

	@Override
	protected String getNamespace() {
		return PidDataElements.PID_NAMESPACE;
	}

	@Override
	protected Set<String> getRequiredElements() {
		return PidDataElements.MANDATORY_ELEMENTS;
	}

	@Override
	protected String getRequirementDescription() {
		return "mandatory";
	}

	@Override
	protected String getCredentialName() {
		return "PID";
	}

	@Override
	protected String getSpecificationName() {
		return "the EUDI PID Rulebook";
	}
}

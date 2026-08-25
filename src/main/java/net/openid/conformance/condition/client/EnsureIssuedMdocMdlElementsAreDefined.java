package net.openid.conformance.condition.client;

import net.openid.conformance.util.MdlDataElements;

/**
 * Checks an mDL as issued over VCI contains no data element ISO/IEC 18013-5 Table 20 does not
 * define. 13.4.1 says "Within this NameSpace, only data elements defined in 13.4.2 may be used".
 * */
public class EnsureIssuedMdocMdlElementsAreDefined extends AbstractEnsureIssuedMdocElementsAreDefined {

	@Override
	protected String getExpectedDocType() {
		return MdlDataElements.MDL_DOCTYPE;
	}

	@Override
	protected String getCredentialName() {
		return "mDL";
	}

	@Override
	protected String getSpecificationName() {
		return "ISO/IEC 18013-5 Table 20";
	}

	@Override
	protected boolean isKnownNamespace(String namespace) {
		return MdlDataElements.isKnownNamespace(namespace);
	}

	@Override
	protected boolean isDefined(String namespace, String elementIdentifier) {
		return MdlDataElements.isDefined(namespace, elementIdentifier);
	}
}

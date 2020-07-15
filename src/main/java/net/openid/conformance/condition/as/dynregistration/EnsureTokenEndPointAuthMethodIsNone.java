package net.openid.conformance.condition.as.dynregistration;

public class EnsureTokenEndPointAuthMethodIsNone extends AbstractEnsureTokenEndPointAuthMethod {

	@Override
	public String expectedTokenEndPointAuthMethod() {
		return "none";
	}
}

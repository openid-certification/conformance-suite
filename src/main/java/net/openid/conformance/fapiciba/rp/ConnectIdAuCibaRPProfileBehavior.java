package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.AustraliaConnectIdAddTxnToIdTokenClaims;
import net.openid.conformance.condition.as.AustraliaConnectIdCheckForFAPI2ClaimsInRequestObject;
import net.openid.conformance.condition.as.AustraliaConnectIdEnsureRequestObjectContainsNoAcrClaims;
import net.openid.conformance.condition.as.AustraliaConnectIdEnsureRequestObjectContainsTrustFramework;
import net.openid.conformance.condition.as.AustraliaConnectIdEnsureRequestObjectSigningAlgIsPS256;
import net.openid.conformance.condition.as.AustraliaConnectIdEnsureVerifiedClaimsInRequestObject;
import net.openid.conformance.condition.as.AustraliaConnectIdValidateRequestObjectBindingMessage;
import net.openid.conformance.condition.as.AustraliaConnectIdValidateRequestObjectExp;
import net.openid.conformance.condition.as.AustraliaConnectIdValidateRequestObjectNBFClaim;
import net.openid.conformance.condition.as.AustraliaConnectIdValidateRequestObjectPurpose;
import net.openid.conformance.condition.as.LoadRequestedIdTokenClaims;

public class ConnectIdAuCibaRPProfileBehavior extends FAPICIBARPProfileBehavior {

	@Override
	public void exposeProfileSpecificEndpoints() {
		module.exposeMtlsPath("userinfo_endpoint", "userinfo");
	}

	@Override
	public void applyProfileSpecificBackchannelRequestChecks() {
		super.applyProfileSpecificBackchannelRequestChecks();
		module.callCondition(AustraliaConnectIdEnsureRequestObjectSigningAlgIsPS256.class, "CID-SP-4.2-8");
		module.callCondition(AustraliaConnectIdValidateRequestObjectExp.class, "CID-SP-4.2-10");
		module.callCondition(AustraliaConnectIdValidateRequestObjectNBFClaim.class, "CID-SP-4.2-11");
		module.callCondition(AustraliaConnectIdValidateRequestObjectBindingMessage.class, "CID-IDA-5.2-10");
		module.callCondition(AustraliaConnectIdEnsureRequestObjectContainsNoAcrClaims.class, "CID-IDA-5.2-5");
		module.callCondition(AustraliaConnectIdCheckForFAPI2ClaimsInRequestObject.class, "CID-IDA-5.2-7");
		module.callCondition(AustraliaConnectIdEnsureVerifiedClaimsInRequestObject.class, "CID-IDA-5.2-11");
		module.callCondition(AustraliaConnectIdEnsureRequestObjectContainsTrustFramework.class, "CID-IDA-5.1-11");
	}

	@Override
	public void applyProfileSpecificIdTokenClaims() {
		super.applyProfileSpecificIdTokenClaims();
		module.callCondition(LoadRequestedIdTokenClaims.class);
		module.callCondition(AustraliaConnectIdAddTxnToIdTokenClaims.class, "CID-IDA-5.1.6");
	}

}

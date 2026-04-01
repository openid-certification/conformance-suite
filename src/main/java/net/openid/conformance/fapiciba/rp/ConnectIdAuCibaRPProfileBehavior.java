package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.AustraliaConnectIdCheckForFAPI2ClaimsInRequestObject;
import net.openid.conformance.condition.as.AustraliaConnectIdEnsureRequestObjectContainsNoAcrClaims;
import net.openid.conformance.condition.as.AustraliaConnectIdEnsureRequestObjectContainsTrustFramework;
import net.openid.conformance.condition.as.AustraliaConnectIdEnsureRequestObjectSigningAlgIsPS256;
import net.openid.conformance.condition.as.AustraliaConnectIdEnsureVerifiedClaimsInRequestObject;
import net.openid.conformance.condition.as.AustraliaConnectIdValidateRequestObjectExp;
import net.openid.conformance.condition.as.AustraliaConnectIdValidateRequestObjectNBFClaim;
import net.openid.conformance.condition.as.AustraliaConnectIdValidateRequestObjectPurpose;

public class ConnectIdAuCibaRPProfileBehavior extends FAPICIBARPProfileBehavior {

	@Override
	public void exposeProfileSpecificEndpoints() {
		module.exposeMtlsPath("userinfo_endpoint", "userinfo");
	}

	@Override
	public void applyProfileSpecificBackchannelRequestChecks() {
		super.applyProfileSpecificBackchannelRequestChecks();
		module.callCondition(AustraliaConnectIdEnsureRequestObjectSigningAlgIsPS256.class);
		module.callCondition(AustraliaConnectIdValidateRequestObjectExp.class);
		module.callCondition(AustraliaConnectIdValidateRequestObjectNBFClaim.class);
		module.callCondition(AustraliaConnectIdValidateRequestObjectPurpose.class);
		module.callCondition(AustraliaConnectIdEnsureRequestObjectContainsNoAcrClaims.class);
		module.callCondition(AustraliaConnectIdCheckForFAPI2ClaimsInRequestObject.class);
		module.callCondition(AustraliaConnectIdEnsureVerifiedClaimsInRequestObject.class);
		module.callCondition(AustraliaConnectIdEnsureRequestObjectContainsTrustFramework.class);
	}

}

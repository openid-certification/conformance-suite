package net.openid.conformance.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.info.TestPlanService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;


@Component
public class MigrationAuthenticationHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	private TestPlanService testPlanService;
	private TestInfoService testInfoService;

	public MigrationAuthenticationHandler(TestPlanService testPlanService, TestInfoService testInfoService) {
		this.testPlanService = testPlanService;
		this.testInfoService = testInfoService;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {

		OidcUser principal = (OidcUser) authentication.getPrincipal();
		Map<String, Object> claims = principal.getAttribute("idp");

		if (claims != null) {
			String idp_iss = claims.get("iss").toString();
			String idp_sub = claims.get("sub").toString();

			if (idp_iss != null && idp_sub != null) {
				this.testPlanService.migrateOwnership(idp_iss, idp_sub);
				this.testInfoService.migrateOwnership(idp_iss, idp_sub);
			}
		}

		super.onAuthenticationSuccess(request, response, authentication);
	}
}

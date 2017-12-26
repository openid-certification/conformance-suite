package io.fintechlabs.testframework.ui;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.security.AuthenticationFacade;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.mitre.openid.connect.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Controller
public class JavascriptController{

	@Autowired
	private AuthenticationFacade authenticationFacade;

	/**
	 * Inject the user's display name and principal as a string into the
	 * /js/fapi.ui.js javascript file, which will then insert it into the diplayed HTML
	 * @param m
	 * @return
	 */
	@RequestMapping(value = "/js/fapi.ui.js", method = RequestMethod.GET)
	public String getAvailableTests(Model m) {
		OIDCAuthenticationToken token = authenticationFacade.getAuthenticationToken();
		String principal =  authenticationFacade.getPrincipal().toString();
		String displayName = principal;
		UserInfo userInfo = token.getUserInfo();
		if (userInfo != null){
			if (!Strings.isNullOrEmpty(userInfo.getEmail())) {
				displayName = userInfo.getEmail();
			} else if (!Strings.isNullOrEmpty(userInfo.getPreferredUsername())) {
				displayName = userInfo.getPreferredUsername();
			} else if (!Strings.isNullOrEmpty(userInfo.getName())){
				displayName = userInfo.getName();
			}
		}

		m.addAttribute("principal",principal);
		m.addAttribute("displayName",displayName);
		return "js/fapi.ui.js";
	}
}


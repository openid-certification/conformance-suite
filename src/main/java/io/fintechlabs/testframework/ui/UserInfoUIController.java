package io.fintechlabs.testframework.ui;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.security.AuthenticationFacade;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.mitre.openid.connect.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.HashMap;
import java.util.Map;

@Controller
public class UserInfoUIController {

	@Autowired
	private AuthenticationFacade authenticationFacade;

	/**
	 * Inject the user's display name and principal as a string into the
	 * /js/fapi.ui.js javascript file, which will then insert it into the diplayed HTML
	 * @param m
	 * @return
	 */
//	@RequestMapping(value = "/js/fapi.ui.js", method = RequestMethod.GET)
//	public String getAvailableTests(Model m) {
//		OIDCAuthenticationToken token = authenticationFacade.getAuthenticationToken();
//		String principal =  authenticationFacade.getPrincipal().toString();
//		String displayName = authenticationFacade.getDisplayName();
//		m.addAttribute("principal",principal);
//		m.addAttribute("displayName",displayName);
//		return "js/fapi.ui.js";
//	}

	@RequestMapping(value = "/currentuser", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getCurrentUserInfo(){
		Map<String, Object> map = new HashMap<>();

		OIDCAuthenticationToken token = authenticationFacade.getAuthenticationToken();
		Map<String,String> principal =  authenticationFacade.getPrincipal();
		String displayName = authenticationFacade.getDisplayName();
		UserInfo userInfo = token.getUserInfo();
		map.put("iss",principal.get("iss"));
		map.put("sub",principal.get("sub"));
		map.put("principal", principal.toString());
		map.put("displayName",displayName);
		map.put("isAdmin",authenticationFacade.isAdmin());
		map.put("userInfo",userInfo);
		return new ResponseEntity<>(map, HttpStatus.OK);
	}
}


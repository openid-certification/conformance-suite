package io.fintechlabs.testframework.ui;

import java.util.HashMap;
import java.util.Map;

import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.mitre.openid.connect.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import io.fintechlabs.testframework.security.AuthenticationFacade;

@Controller
public class UserInfoUIController {

	@Autowired
	private AuthenticationFacade authenticationFacade;

	/**
	 * Provide a JSON result that represents the currently logged in user.
	 * @return
	 */
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


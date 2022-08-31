package net.openid.conformance.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class LoginPageController {

	@Autowired
	private ServerInfoTemplate serverInfoTemplate;

	@RequestMapping("/login.html")
	public String login(Model model) {
		String brand = serverInfoTemplate.getServerInfo().get("brand")
			.toLowerCase()
			.replaceAll("\\W", "");

		model.addAttribute("brand", brand);
		return "login.html";
	}

}
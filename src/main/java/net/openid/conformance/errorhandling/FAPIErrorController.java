package net.openid.conformance.errorhandling;

import com.google.common.base.Strings;
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
public class FAPIErrorController extends AbstractErrorController {

	public FAPIErrorController(ErrorAttributes errorAttributes) {
		super(errorAttributes);
	}

	@RequestMapping(value = "/error")
	public Object handleError(HttpServletRequest request) {
		Map<String, Object> map = getErrorAttributes(request, false);

		String path = (String) map.get("path");
		if (!Strings.isNullOrEmpty(path) && path.contains("/api/")) {
			return new ResponseEntity<>(map, getStatus(request));
		} else {
			return new ModelAndView("error", map);
		}
	}

	@Override
	public String getErrorPath() {
		return "/error";
	}
}

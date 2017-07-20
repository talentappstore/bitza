package com.aotal.bitza;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WebController {

  private static final Logger logger = LoggerFactory.getLogger(WebController.class);

  // poor security - we should at least check that this tenant has us installed, either using our own customers table,
  // or via core-in API call
  @RequestMapping("/tenants/{tenant}")
  public String otherRoot(Model model, @PathVariable String tenant) {
	  model.addAttribute("tenant", tenant);
	  model.addAttribute("bodyUrl", APIController.BODYURL);
	  return "index";
  }
  
}


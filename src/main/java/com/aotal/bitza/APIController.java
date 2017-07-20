package com.aotal.bitza;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
public class APIController {

  private static final Logger logger = LoggerFactory.getLogger(APIController.class);

  public final static String APP = "bitza";
  public final static String SECRET = "yd4pPhrU2C5Z21kAdS_HqcPRAoqq9dKtzcSxdiTQ";
  public final static String BODYURL = "http://talentappstore.github.io/tas-tenant-apis/examples/applicationRead-simple.json";
  
  private RestTemplate restTemplate = new RestTemplate();

  /**
   * helper method to make API calls with the tazzy-secret header as required when making API calls on TAS, and optionally a request body
   * @return
   */
  private HttpEntity entityWithSecret(String body) {
	  HttpHeaders headers = new HttpHeaders();
	  headers.set("tazzy-secret", SECRET);  // attaching the "tazzy-secret" request header
	  HttpEntity entity;
	  if (body != null)
		  entity = new HttpEntity(body, headers);
	  else
		  entity = new HttpEntity(headers);
	  return entity;
  }

  /** convert a web resource (i.e. json document hosted on github) into a String that we can pass into an API call
   * 
   * @return
   * @throws MalformedURLException
   * @throws IOException
   */
  private String getBody() throws MalformedURLException, IOException {
	InputStream is = new URL(BODYURL).openStream();
	final int bufferSize = 1024;
	final char[] buffer = new char[bufferSize];
	final StringBuilder out = new StringBuilder();
	Reader in = new InputStreamReader(is, "UTF-8");
	for (; ; ) {
	    int rsz = in.read(buffer, 0, buffer.length);
	    if (rsz < 0)
	        break;
	    out.append(buffer, 0, rsz);
	}
//	logger.info("built API body from web resource: " + out.toString());
	return out.toString();
  }
  
  /** 
   * This endpoint is hit via ajax from the web page. It triggers sending off the outgoing push messages.
   * (obviously insecure, as we are open to the world!)
   * 
   * @param model
   * @return
 * @throws IOException 
 * @throws JsonMappingException 
 * @throws JsonParseException 
   */
  @RequestMapping(value = "/tenants/{tenant}/ping", method = RequestMethod.POST)
  public String ping(Model model, @PathVariable String tenant) throws JsonParseException, JsonMappingException, IOException {

	  StringBuffer results = new StringBuffer();
	  
	  // call GET /routes, to learn about all producers of /applications/views/at/onboard/now/byID/{application}/pushes,
	  // i.e. everyone who wants to know when a new hire is due to be onboarded
	  String routes;
	  {
		  String url = "https://" + APP + ".tazzy.io/core/routes/" + tenant + "/" + APP
				  + "?apiDev=tas&api=%2Fapplications%2Fviews%2Fat%2Fonboard%2Fnow%2FbyID%2F%7Bapplication%7D%2Fpushes&sot=false";
		  logger.info("calling: " + url);
		  results.append("calling: " + url);
		  ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entityWithSecret(null), String.class);
		  routes = response.getBody();
		  logger.info("GET /routes returned: " + routes);
		  results.append("GET /routes returned: " + routes + "\n");
	  }
	  
	  // now examine the routes json to parse out the list of the app shortcodes (one per producer)    
	  ArrayList<String> producerList = new ArrayList<>();
	  {
		  Reader reader = new StringReader(routes);
		  ObjectMapper objectMapper = new ObjectMapper();
		  ObjectNode node = objectMapper.readValue(reader, ObjectNode.class);
		  
		  JsonNode producers = node.get("producingAppInstalls");
		  for (int i = 0; i < producers.size(); i++) {
		    JsonNode producer = producers.get(i);
		    String shortCode = producer.get("app").asText();
		    logger.info("found a producer: " + shortCode);
		    results.append("found a producer: " + shortCode + "\n");
		    producerList.add(shortCode);
		  }
	  }
	  logger.info("found a total of " + producerList.size() + " producers");
	  results.append("found a total of " + producerList.size() + " producers" + "\n");

	  
	  // OK, now we have a good list of producers, lets call the API on each of them
	  // TODO: Too slow! Should do this sort of stuff in parallel threads, not one at a time.
	  for (String producer : producerList) {
		  logger.info("calling POST /devs/tas/applications/views/at/onboard/now/byID/1/pushes on app " + producer);
		  results.append("calling POST /devs/tas/applications/views/at/onboard/now/byID/1/pushes on app " + producer + "\n");
		  String url = "https://" + APP + ".tazzy.io/t/"
				  + tenant + "/apps/" + producer + "/devs/tas/applications/views/at/onboard/now/byID/1/pushes";
		  try {
			  ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entityWithSecret(getBody()), String.class);
		  } catch (HttpServerErrorException e) {
			  logger.error("error pushing to app", e);
			  results.append("error pushing to app " + e.getMessage() + "\n");
		  }
	  }
  
	  return results.toString();
  }

	@RequestMapping(value = "/t/{tenant}/tas/devs/tas/applications/views/at/onboard/now/byID/{application}/pushes", method = RequestMethod.POST)
	public String newOnboarder(@PathVariable String tenant, @PathVariable String application,
			@RequestHeader("tazzy-secret") String secret, @RequestBody String body) throws MalformedURLException, IOException {

		logger.info("in POST /applications/views/at/onboard/now/byID/{application}/pushes for tenant " + tenant);

		if (! secret.equals(SECRET)) throw new UnauthorizedException(); // check incoming tazzy-secret

		logger.info("body is: " + body);
		
		return "done!";
	}
	
	@RequestMapping(value = "/t/{tenant}/tas/devs/tas/appStatus", method = RequestMethod.GET)
	public String appStatus(@PathVariable String tenant, @RequestHeader("tazzy-secret") String secret) {
		
		logger.info("in GET /appStatus for tenant " + tenant);

		if (! secret.equals(SECRET)) throw new UnauthorizedException(); // check incoming tazzy-secret
		
		String ret = 			
		"{" +
//		"  \"landingPage\": \"https://" + APP + ".communityapps.talentappstore.com/tenants/" + tenant + "\"," +
		"  \"landingPage\": \"https://" + APP + ".apps.aotal.com/tenants/" + tenant + "\"," +
		"  \"setupRequired\": false" +
		"}";
		
		return ret;
	}

	
}


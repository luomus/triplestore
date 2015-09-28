package fi.luomus.triplestore.utils;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.http.HttpClientService;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.session.SessionHandler;
import fi.luomus.commons.utils.Utils;
import fi.luomus.lajiauth.model.AuthenticationToken;
import fi.luomus.lajiauth.model.Constants;
import fi.luomus.lajiauth.model.UserDetails;
import fi.luomus.lajiauth.service.LajiAuthClient;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.utils.exceptions.ApiException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LoginUtil  {

	private static class AuthenticationResponse {

		private final boolean success;
		private String errorMessage;
		private String userId;
		private String userFullname;
		private String userQname;
		private boolean isAdmin = false;

		public AuthenticationResponse(boolean success) {
			this.success = success;
		}

		public boolean successful() {
			return success;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public String getUserId() {
			return userId;
		}

		public String getUserFullname() {
			return userFullname;
		}

		public String getUserQname() {
			return userQname;
		}

		public boolean isForAdminUser() {
			return isAdmin;
		}

		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		public void setUserId(String userId) {
			this.userId = userId;
		}

		public void setUserFullname(String userFullname) {
			this.userFullname = userFullname;
		}

		public void setUserQname(String userQname) {
			this.userQname = userQname;
		}

		public void setAdmin(boolean isAdmin) {
			this.isAdmin = isAdmin;
		}

	}

	private final SessionHandler session; 
	private final ResponseData responseData; 
	private final String frontPage;
	private final ErrorReporter errorReporter;
	private final TriplestoreDAO dao;
	private final Config config;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public LoginUtil(String frontpage, Config config, SessionHandler session, ResponseData responseData, ErrorReporter errorReporter, TriplestoreDAO dao) {
		this.config = config;
		this.session = session;
		this.responseData = responseData;
		this.frontPage = frontpage;
		this.errorReporter = errorReporter;
		this.dao = dao;
	}

	public static boolean authorized() {
		return true;
	}

	public ResponseData processGet(HttpServletRequest req) throws Exception {
		if (session.isAuthenticatedFor("triplestore")) {
			return responseData.setRedirectLocation(frontPage);
		}
		String originalUrl = getOriginalUrl(req);
		responseData.setData("originalURL", originalUrl);
		if (usingLajiAuth()) {
			setLajiAuthLinks(originalUrl);
		}
		return responseData.setViewName("login");
	}

	private String getOriginalUrl(HttpServletRequest req) {
		String originalUrl = req.getParameter("originalURL");
		if (originalUrl == null) return "";
		if (originalUrl.contains("/logout")) {
			return frontPage;
		}
		return originalUrl;
	}

	private void setLajiAuthLinks(String originalUrl) throws URISyntaxException {
		if (originalUrl == null) {
			originalUrl = "";
		} else {
			originalUrl = originalUrl.replace(config.baseURL(), "");
		}

		LajiAuthClient client = getLajiAuthClient();
		URI hakaURI = client.createLoginUrlForAuthenticationSource(originalUrl, Constants.AuthenticationSources.HAKA);
		URI virtuURI = client.createLoginUrlForAuthenticationSource(originalUrl, Constants.AuthenticationSources.VIRTU);
		responseData.setData("hakaURI", hakaURI.toString());
		responseData.setData("virtuURI", virtuURI.toString());
		responseData.setData("usingLajiAuth", true);
	}

	private boolean usingLajiAuth() {
		return config.defines("LajiAuthURL") && config.defines("SystemQname");
	}

	public ResponseData processPost(HttpServletRequest req) throws Exception {
		String username = req.getParameter("username");
		String password = req.getParameter("password");
		String next = req.getParameter("next");
		String originalUrl = getOriginalUrl(req);
		
		responseData.setViewName("login").setData("originalURL", next).setData("username", username);

		if (usingLajiAuth()) {
			setLajiAuthLinks(originalUrl);
			String token = req.getParameter("token");
			if (given(token)) {
				return tryLajiAuthentication(req, token, next);
			}
		} 
		return tryLuomusAdLogin(req, username, password, originalUrl); 
	}

	private ResponseData tryLuomusAdLogin(HttpServletRequest req, String username, String password, String originalUrl) {
		try {
			AuthenticationResponse authentication = authenticateViaKotkaAPI(username, password);
			if (authentication.successful()) {
				authenticateSession(req, authentication);
				if (given(originalUrl)) {
					return responseData.setRedirectLocation(originalUrl);
				} else {
					return responseData.setRedirectLocation(frontPage);
				}
			} else {
				responseData.setData("error", authentication.getErrorMessage());
				return responseData;
			}
		} catch (Exception e) {
			errorReporter.report("Login for username " + username, e);
			responseData.setData("error", e.getMessage());
			return responseData;
		}
	}

	private ResponseData tryLajiAuthentication(HttpServletRequest req, String token, String next) throws URISyntaxException, IOException, JsonParseException, JsonMappingException, ApiException {
		AuthenticationToken authorizationToken = null;
		try {
			System.out.println("Jee: " + token);
			LajiAuthClient client = getLajiAuthClient();
			authorizationToken = objectMapper.readValue(token, AuthenticationToken.class);
			client.validateToken(authorizationToken);
			// Validation throws exception if something is wrong; Authentication has been successful:
			authenticateSession(req, authorizationToken.getUser());
			if (given(next)) {
				return responseData.setRedirectLocation(config.baseURL() + next);
			} else {
				return responseData.setRedirectLocation(frontPage);
			}
		} catch (Exception e) {
			errorReporter.report("Erroreous LajiAuth login for " + Utils.debugS(token, authorizationToken.toString()), e);
			responseData.setData("lajiAuthError", "Something went wrong! " + e.getMessage());
			return responseData;
		}
	}

	private LajiAuthClient getLajiAuthClient() throws URISyntaxException {
		return new LajiAuthClient(config.get("SystemQname"), new URI(config.get("LajiAuthURL")));
	}

	private boolean given(String s) {
		return s != null && s.trim().length() > 0;
	}

	private void authenticateSession(HttpServletRequest req, AuthenticationResponse authentication) throws Exception {
		session.authenticateFor("triplestore");
		session.setUserId(authentication.getUserId());
		session.setUserName(authentication.getUserFullname());
		session.put("user_qname", authentication.getUserQname());

		int userFK = dao.getUserFK(authentication.getUserQname());
		session.put("user_fk", Integer.toString(userFK)); // Tämä sitä varten, että jos (ja kun) AddStatement ja AddStatementL käytöstä luovutaan, voidaan pistää userfk statement tauluun

		if (authentication.isForAdminUser()) {
			session.put("role", "admin");
		}
		req.getSession().setMaxInactiveInterval(60 * 60 *3);
	}

	private void authenticateSession(HttpServletRequest req, UserDetails user) throws Exception {
		session.authenticateFor("triplestore");
		session.setUserId(user.getEmail());
		session.setUserName(user.getName());
		session.put("user_qname", user.getId());
		int userFK = dao.getUserFK(user.getId());
		session.put("user_fk", Integer.toString(userFK)); // Tämä sitä varten, että jos (ja kun) AddStatement ja AddStatementL käytöstä luovutaan, voidaan pistää userfk statement tauluun
		req.getSession().setMaxInactiveInterval(60 * 60 *3);
	}

	private AuthenticationResponse authenticateViaKotkaAPI(String username, String password) throws Exception {
		HttpClientService client = null;
		try {
			client = new HttpClientService();
			HttpPost postRequest = new HttpPost("https://kotka.luomus.fi/user/remote");
			List <NameValuePair> params = new ArrayList <NameValuePair>();
			params.add(new BasicNameValuePair("username", username));
			params.add(new BasicNameValuePair("password", password));
			postRequest.setEntity(new UrlEncodedFormEntity(params));

			JSONObject response = client.contentAsJson(postRequest);
			return handleAuthenticationResponse(response);
		} finally {
			if (client != null) client.close();
		}
	}

	private AuthenticationResponse handleAuthenticationResponse(JSONObject response) {
		// {"success":false,"message":"Invalid credentials","code":-3}
		// {"success":true,"identity":{"qname":"MA.5","username":"eopiirai","userGroups":["linuxlogin","hatikka","Computer Admins","mutikkalogin","Data Department Staff"],"email":"esko.piirainen@helsinki.fi","lastname":"Piirainen","firstname":"Esko Olavi","fullname":"Piirainen, Esko Olavi"}}
		// {"identity":{"MA.role":"MA.admin","links":[{"rel":"self","href":"/view?uri=http://id.luomus.fi/MA.5"}],"uri":"http://id.luomus.fi/MA.5","MA.emailAddress":["esko.piirainen@helsinki.fi"],"MA.description":null,"rdf:type":"MA.person","MA.yearOfBirth":"1981","MA.fullName":"Esko Piirainen","MA.hatikkaLoginName":"Esko.Piirainen@hatikka.fi","MA.inheritedName":"Piirainen","MA.givenNames":"Esko Olavi","MA.organisation":{"MOS.1016":"University of Helsinki, Finnish Museum of Natural History, General Services Unit, IT Team"},"MA.firstJoined":{"timezone":"Europe/Helsinki","timezone_type":3,"date":"2011-06-01 00:00:00"},"MA.preferredName":"Esko","MA.LTKMLoginName":"eopiirai"},"success":true}
		if (!response.getBoolean("success")) {
			AuthenticationResponse authenticationResponse = new AuthenticationResponse(false);
			authenticationResponse.setErrorMessage(response.getString("message"));
			return authenticationResponse;
		} else {
			AuthenticationResponse authenticationResponse = new AuthenticationResponse(true);
			JSONObject identity = response.getObject("identity");
			authenticationResponse.setUserId(identity.getString("MA.LTKMLoginName"));
			authenticationResponse.setUserQname(identity.getString("qname"));
			authenticationResponse.setUserFullname(identity.getString("MA.fullName"));
			setAdminStatus(authenticationResponse, identity);
			return authenticationResponse;
		}
	}

	private void setAdminStatus(AuthenticationResponse authenticationResponse, JSONObject identity) {
		if ("MA.admin".equals(identity.getString("MA.role"))) {
			authenticationResponse.setAdmin(true);
		}
	}

}

package fi.luomus.triplestore.utils;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.http.HttpClientService;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.session.SessionHandler;
import fi.luomus.commons.utils.Utils;
import fi.luomus.lajiauth.model.AuthenticationEvent;
import fi.luomus.lajiauth.model.AuthenticationSource;
import fi.luomus.lajiauth.model.UserDetails;
import fi.luomus.lajiauth.service.LajiAuthClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import com.fasterxml.jackson.databind.ObjectMapper;

public class LoginUtil  {

	private static final String ORIGINAL_URL = "originalURL";

	private static class AuthenticationResult {

		private final boolean success;
		private String errorMessage;
		private String userId;
		private String userFullname;
		private String userQname;
		private boolean isAdmin = false;

		public AuthenticationResult(boolean success) {
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

		public AuthenticationResult setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
			return this;
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
	private final Config config;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public LoginUtil(String frontpage, Config config, SessionHandler session, ResponseData responseData, ErrorReporter errorReporter) {
		this.config = config;
		this.session = session;
		this.responseData = responseData;
		this.frontPage = frontpage;
		this.errorReporter = errorReporter;
	}

	public static boolean authorized() {
		return true;
	}

	public ResponseData processGet(HttpServletRequest req) throws Exception {
		if (session.isAuthenticatedFor("triplestore")) {
			return responseData.setRedirectLocation(frontPage);
		}
		String originalUrl = getOriginalUrl(req);
		responseData.setData(ORIGINAL_URL, originalUrl);
		if (usingLajiAuth()) {
			setLajiAuthLinks(originalUrl);
		}
		return responseData.setViewName("login");
	}

	private String getOriginalUrl(HttpServletRequest req) {
		String originalUrl = req.getParameter(ORIGINAL_URL);
		if (originalUrl == null) return "";
		if (originalUrl.contains("/logout")) {
			return frontPage;
		}
		return originalUrl;
	}

	private void setLajiAuthLinks(String originalUrl) throws URISyntaxException {
		MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
		params.putSingle(ORIGINAL_URL, originalUrl);

		LajiAuthClient client = getLajiAuthClient();
		URI hakaURI = client.createLoginUrl("").authenticationSource(AuthenticationSource.HAKA).query(params).build();
		URI virtuURI = client.createLoginUrl("").authenticationSource(AuthenticationSource.VIRTU).query(params).build();
		URI lajifiURI = client.createLoginUrl("").authenticationSource(AuthenticationSource.LOCAL).query(params).build();

		responseData.setData("hakaURI", hakaURI.toString());
		responseData.setData("virtuURI", virtuURI.toString());
		responseData.setData("lajifiURI", lajifiURI.toString());
		responseData.setData("usingLajiAuth", true);
	}

	private boolean usingLajiAuth() {
		return config.defines("LajiAuthURL") && config.defines("SystemQname");
	}

	public ResponseData processPost(HttpServletRequest req) throws Exception {
		String adUsername = req.getParameter("username");
		String adPassword = req.getParameter("password");
		String lajiAuthToken = req.getParameter("token");
		String originalUrl = getOriginalUrl(req);

		responseData.setViewName("login").setData(ORIGINAL_URL, originalUrl).setData("username", adUsername);

		if (usingLajiAuth()) {
			setLajiAuthLinks(originalUrl);
		}

		AuthenticationResult authentication = null;
		if (usingLajiAuth() && given(lajiAuthToken)) {
			authentication = authenticateViaLajiAuthentication(lajiAuthToken);
		} else {
			authentication = authenticateViaKotkaAPI(adUsername, adPassword);
		}

		try {
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
			errorReporter.report("Login data " + Utils.debugS(adUsername, lajiAuthToken, originalUrl), e);
			responseData.setData("error", "Something went wrong: " + e.getMessage());
			return responseData;
		} 
	}

	private void authenticateSession(HttpServletRequest req, AuthenticationResult authentication) throws Exception {
		session.authenticateFor("triplestore");
		session.setUserId(authentication.getUserId());
		session.setUserName(authentication.getUserFullname());
		session.put("user_qname", authentication.getUserQname());

		if (authentication.isForAdminUser()) {
			session.put("role", "admin");
		}
		req.getSession().setMaxInactiveInterval(60 * 60 *3);
	}

	private AuthenticationResult authenticateViaLajiAuthentication(String token) throws Exception {
		AuthenticationEvent authorizationInfo = null;
		try {
			LajiAuthClient client = getLajiAuthClient();
			authorizationInfo = client.validateTokenAndGetAuthenticationInfo(token);
			// Validation throws exception if something is wrong; Authentication has been successful:

			AuthenticationResult authenticationResponse = new AuthenticationResult(true);
			UserDetails userDetails = authorizationInfo.getUser();
			return authenticationResultFromLajiAuth(authenticationResponse, userDetails);
		} catch (Exception e) {
			if (authorizationInfo != null) {
				errorReporter.report("Erroreous LajiAuth login for " + Utils.debugS(token, objectMapper.writeValueAsString(authorizationInfo)), e);
			} else {
				errorReporter.report("Unsuccesful LajiAuth login for " + token, e);
			}
			AuthenticationResult authenticationResult = new AuthenticationResult(false);
			authenticationResult.setErrorMessage(e.getMessage());
			return authenticationResult;
		}
	}

	private AuthenticationResult authenticationResultFromLajiAuth(AuthenticationResult authenticationResponse, UserDetails userDetails) {
		if (!userDetails.getQname().isPresent()) return new AuthenticationResult(false).setErrorMessage("Required permissions to to use this system are missing.");;
		// TODO taxon editor permission predicate
		authenticationResponse.setUserId(userDetails.getEmail());
		authenticationResponse.setUserQname(userDetails.getQname().get());
		authenticationResponse.setUserFullname(userDetails.getName());
		setAdminStatus(authenticationResponse, userDetails.getRoles());
		return authenticationResponse;
	}

	private void setAdminStatus(AuthenticationResult authenticationResponse, Set<String> roles) {
		authenticationResponse.setAdmin(roles.contains("MA.admin"));
	}

	private LajiAuthClient getLajiAuthClient() throws URISyntaxException {
		return new LajiAuthClient(config.get("SystemQname"), new URI(config.get("LajiAuthURL")));
	}

	private AuthenticationResult authenticateViaKotkaAPI(String username, String password) throws Exception {
		HttpClientService client = null;
		try {
			client = new HttpClientService();
			HttpPost postRequest = new HttpPost("https://kotka.luomus.fi/user/remote");
			List <NameValuePair> params = new ArrayList <NameValuePair>();
			params.add(new BasicNameValuePair("username", username));
			params.add(new BasicNameValuePair("password", password));
			postRequest.setEntity(new UrlEncodedFormEntity(params));

			JSONObject response = client.contentAsJson(postRequest);
			return authenticationResultFromKotkaAPI(response);
		} finally {
			if (client != null) client.close();
		}
	}

	private AuthenticationResult authenticationResultFromKotkaAPI(JSONObject response) {
		// {"success":false,"message":"Invalid credentials","code":-3}
		// {"identity":{"MA.role":"MA.admin","links":[{"rel":"self","href":"/view?uri=http://tun.fi/MA.5"}],"uri":"http://tun.fi/MA.5","MA.emailAddress":["esko.piirainen@helsinki.fi"],"MA.description":null,"rdf:type":"MA.person","MA.yearOfBirth":"1981","MA.fullName":"Esko Piirainen","MA.hatikkaLoginName":"Esko.Piirainen@hatikka.fi","MA.inheritedName":"Piirainen","MA.givenNames":"Esko Olavi","MA.organisation":{"MOS.1016":"University of Helsinki, Finnish Museum of Natural History, General Services Unit, IT Team"},"MA.firstJoined":{"timezone":"Europe/Helsinki","timezone_type":3,"date":"2011-06-01 00:00:00"},"MA.preferredName":"Esko","MA.LTKMLoginName":"eopiirai"},"success":true}
		if (!response.getBoolean("success")) {
			AuthenticationResult authenticationResult = new AuthenticationResult(false);
			authenticationResult.setErrorMessage(response.getString("message"));
			return authenticationResult;
		} else {
			AuthenticationResult authenticationResponse = new AuthenticationResult(true);
			JSONObject identity = response.getObject("identity");
			authenticationResponse.setUserId(identity.getString("MA.LTKMLoginName"));
			authenticationResponse.setUserQname(identity.getString("qname"));
			authenticationResponse.setUserFullname(identity.getString("MA.fullName"));
			setAdminStatus(authenticationResponse, identity);
			return authenticationResponse;
		}
	}

	private void setAdminStatus(AuthenticationResult authenticationResponse, JSONObject identity) {
		if ("MA.admin".equals(identity.getString("MA.role"))) {
			authenticationResponse.setAdmin(true);
		}
	}

	private boolean given(String s) {
		return s != null && s.trim().length() > 0;
	}
}

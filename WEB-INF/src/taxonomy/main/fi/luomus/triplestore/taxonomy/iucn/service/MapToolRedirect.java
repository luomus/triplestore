package fi.luomus.triplestore.taxonomy.iucn.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.URIBuilder;
import fi.luomus.commons.utils.Utils;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn/maptool/*"})
public class MapToolRedirect extends FrontpageServlet {

	private static final long serialVersionUID = -5380229863383051746L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		Qname taxonId = Qname.of(getId(req));
		if (!given(taxonId)) return status500(res);
		boolean localhost = "true".equals(req.getParameter("localhost"));

		if (!localhost) {
			checkIucnPermissions(taxonId.toString(), req);
		} else {
			if (!getUser(req).isAdmin()) throw new IllegalAccessException("Person does not have permissions to alter iucn target " + taxonId);
		}
		Qname editor = getUser(req).getQname();

		TokenGenerator generator = new TokenGenerator(getConfig().get("MapTool_Secret"));
		String token = generator.generate(taxonId, editor);
		String baseUrl = localhost ? "http://localhost:5000" : getConfig().get("MapTool_URL");

		URIBuilder uriB = new URIBuilder(baseUrl).addParameter("token", token);
		return redirectTo(uriB.toString());
	}

	private static class TokenGenerator {
		private static final SecureRandom RANDOM = new SecureRandom();

		private static final int NONCE_LENGTH = 12;
		private static final int TAG_LENGTH_BITS = 128;

		private final String sharedSecret;

		/**
		 * 32-byte secret
		 * @param sharedSecret
		 */
		public TokenGenerator(String sharedSecret) {
			this.sharedSecret = sharedSecret;
		}

		public String generate(Qname taxon, Qname user) throws Exception {
			long issued = System.currentTimeMillis() / 1000;
			long expired = issued + 120; // 2 minutes
			JSONObject json = new JSONObject()
					.setString("taxon", taxon.toString())
					.setString("user", user.toString())
					.setString("id", Utils.generateGUID())
					.setInteger("issued", (int)issued)
					.setInteger("expired", (int)expired);
			byte[] key = Base64.getDecoder().decode(sharedSecret);
			if (key.length != 32) {
				throw new IllegalStateException("MapTool secret must decode to 32 bytes.");
			}
			byte[] nonce = new byte[NONCE_LENGTH];
			RANDOM.nextBytes(nonce);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));

			byte[] ciphertextAndTag = cipher.doFinal(json.toString().getBytes(StandardCharsets.UTF_8));

			byte[] token = new byte[1 + nonce.length + ciphertextAndTag.length];
			token[0] = 1;
			System.arraycopy(nonce, 0, token, 1, nonce.length);

			System.arraycopy(ciphertextAndTag,0,token,1 + nonce.length,ciphertextAndTag.length);

			return Base64.getUrlEncoder()
					.withoutPadding()
					.encodeToString(token);
		}
	}

}

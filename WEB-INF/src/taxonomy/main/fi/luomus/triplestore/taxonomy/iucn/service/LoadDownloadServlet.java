package fi.luomus.triplestore.taxonomy.iucn.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.io.ByteStreams;

import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.FileUtils;
import fi.luomus.triplestore.taxonomy.service.TaxonomyEditorBaseServlet;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn/file/*"})
public class LoadDownloadServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = -5248574840635249571L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String filename = getId(req);
		File file = new File(getConfig().reportFolder(), filename);
		if (!file.exists()) throw new IllegalStateException("File " + filename + " does not exist!");

		OutputStream out = null;
		FileInputStream in = null;
		try {
			res.setContentType("application/zip");
			res.setHeader("Content-disposition","attachment; filename=" + filename);
			out = res.getOutputStream();
			in = new FileInputStream(file);
			ByteStreams.copy(in, out);
			return new ResponseData().setOutputAlreadyPrinted();
		} finally {
			FileUtils.close(in);
			if (out != null) out.flush();
		}
	}

}

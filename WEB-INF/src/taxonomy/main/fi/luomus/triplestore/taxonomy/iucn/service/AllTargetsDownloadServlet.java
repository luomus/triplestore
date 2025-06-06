package fi.luomus.triplestore.taxonomy.iucn.service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.commons.utils.FileCompresser;
import fi.luomus.commons.utils.FileUtils;
import fi.luomus.triplestore.taxonomy.iucn.model.Container;
import fi.luomus.triplestore.taxonomy.iucn.model.EvaluationTarget;
import fi.luomus.triplestore.taxonomy.iucn.model.EvaluationYear;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn/download-all/*"})
public class AllTargetsDownloadServlet extends GroupSpeciesListServlet {

	private static final long serialVersionUID = -5521328653270033492L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		startDownloadThread();
		return redirectTo(getConfig().baseURL()+"/iucn");
	}

	private void startDownloadThread() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					doDownload();
				} catch (Exception e) {
					getErrorReporter().report(e);
				}
			}
		}).start();
	}

	private void doDownload() throws Exception, IOException {
		FileCompresser c = null;
		File tempFile = getTempFile();
		try {
			System.out.println("Starting IUCN all data download ...");
			String data = getData();
			System.out.println(" ... writing IUCN all data download ...");
			FileUtils.writeToFile(tempFile, data);
			c = new FileCompresser(getZipFile());
			c.addToZip(tempFile);
			System.out.println("IUCN all data download completed!");
		} finally {
			if (c != null) c.close();
			try {
				tempFile.delete();
			} catch (Exception e) {
				getErrorReporter().report("Unable to delete temp file " + tempFile.getName(), e);
			}
		}
	}

	private String getData() throws Exception {
		List<String> rows = getDataRows();
		String data = toString(rows);
		return data;
	}

	private List<String> getDataRows() throws Exception {
		Container container = getTaxonomyDAO().getIucnDAO().getIUCNContainer();
		Collection<EvaluationTarget> targets = container.getGroupOrderedTargets();
		int selectedYear = getMaxYear();
		List<String> rows = getDownloadRows(container, selectedYear, targets);
		return rows;
	}

	private File getTempFile() {
		File folder = new File(getConfig().reportFolder());
		folder.mkdirs();
		File file = new File(folder, "kaikki_lajit_"+DateUtils.getFilenameDatetime()+".csv");
		return file;
	}

	private File getZipFile() {
		File folder = new File(getConfig().reportFolder());
		folder.mkdirs();
		File file = new File(folder, "kaikki_lajit_"+DateUtils.getFilenameDatetime()+".zip");
		return file;
	}

	private String toString(List<String> rows) {
		StringBuilder b = new StringBuilder();
		for (String row : rows) {
			b.append(row).append("\n");
		}
		return b.toString();
	}

	private int getMaxYear() throws Exception {
		int max = 0;
		for (EvaluationYear y : getTaxonomyDAO().getIucnDAO().getEvaluationYears()) {
			max = Math.max(max, y.getYear());
		}
		return max;
	}

}

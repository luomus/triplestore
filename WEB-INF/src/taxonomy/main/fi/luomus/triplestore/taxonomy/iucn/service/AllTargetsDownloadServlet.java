package fi.luomus.triplestore.taxonomy.iucn.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.DateUtils;
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
		File file = getFile();
		System.out.println("Starting IUCN all data download ...");
		Container container = getTaxonomyDAO().getIucnDAO().getIUCNContainer();
		Collection<EvaluationTarget> targets = container.getGroupOrderedTargets();
		int selectedYear = getMaxYear();
		System.out.println("... writing Excel file ...");
		try (FileOutputStream os = new FileOutputStream(file)) {
			writeExcel(os, container, selectedYear, targets);
		}
		System.out.println("IUCN all data download completed!");
	}

	private File getFile() {
		File folder = new File(getConfig().reportFolder());
		folder.mkdirs();
		File file = new File(folder, "kaikki_lajit_"+DateUtils.getFilenameDatetime()+".xlsx");
		return file;
	}

	private int getMaxYear() throws Exception {
		int max = 0;
		for (EvaluationYear y : getTaxonomyDAO().getIucnDAO().getEvaluationYears()) {
			max = Math.max(max, y.getYear());
		}
		return max;
	}

}

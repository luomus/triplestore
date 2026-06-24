package fi.luomus.triplestore.taxonomy.models;

import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.junit.After;
import org.junit.Test;

public class ExcelGenerationTest {

	private Path tempFile;

	@After
	public void cleanup() throws Exception {
		if (tempFile != null) {
			System.out.println(tempFile.toString());
			Files.deleteIfExists(tempFile);
		}
	}

	@Test
	public void shouldCreateSimpleExcelFile() throws Exception {
		tempFile = Files.createTempFile("fastexcel-test-", ".xlsx");
		try (
				OutputStream os = Files.newOutputStream(tempFile);
				Workbook wb = new Workbook(os, "Test", "1.0")
				) {
			Worksheet ws = wb.newWorksheet("Species");

			// Header
			ws.value(0, 0, "Id");
			ws.value(0, 1, "Scientific name");
			ws.value(0, 2, "Status");

			// Data rows
			ws.value(1, 0, 1);
			ws.value(1, 1, "Pica pica");
			ws.value(1, 2, "LC");

			ws.value(2, 0, 2);
			ws.value(2, 1, "Lynx lynx");
			ws.value(2, 2, "VU");

			ws.value(3, 0, 3);
			ws.value(3, 1, "Pusa hispida saimensis");
			ws.value(3, 2, "EN");
		}

		assertTrue(Files.exists(tempFile));
		assertTrue(Files.size(tempFile) > 0);
	}

}
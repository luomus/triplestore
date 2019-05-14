package fi.luomus.triplestore.taxonomy.iucn.runnable;
import java.io.File;
import java.io.IOException;

import fi.luomus.commons.utils.FileUtils;

public class IUCNFileFiksaaja {

	public static void main(String[] args) {
		try {
			main();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void main() throws Exception {
		File orig = new File("C:/Users/Zz/git/eskon-dokkarit/Taksonomia/punainen-kirja-2010-2015/2019/sammaleet_karsittu.csv");
		File fixed = new File(orig.getParentFile(), orig.getName().replace(".csv", "_fixed.csv"));
		String completedLine = "";
		for (String line : FileUtils.readLines(orig)) {
			if (line.startsWith("|")) continue;
			if (newline(line)) {
				write(completedLine, fixed);
				completedLine = line;
			} else {
				completedLine += " " + line;
			}
		}
		write(completedLine, fixed);
		System.out.println("done");
	}

	private static boolean newline(String line) {
		return line.startsWith("\"") && !line.startsWith("\"|");
	}

	private static void write(String completedLine, File fixed) throws IOException {
		if (completedLine.isEmpty()) return;
		completedLine = completedLine.replace("\"", "");
		completedLine += "\n";
		FileUtils.writeToFile(fixed, completedLine, true);
	}
}

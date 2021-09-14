package de.neuefische.elotracking.backend.logging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;

public class LogFileTools {

	private static final String LOG_ARCHIVE_FOLDER = "archive";
	private static final String LOG_FILE_NAME = "log.txt";

	public static void archiveOldLogFile() {
		try {
			if (!Files.exists(Path.of(LOG_ARCHIVE_FOLDER))) {
				Files.createDirectory(Path.of(LOG_ARCHIVE_FOLDER));
			}
			if (!Files.exists(Path.of(LOG_FILE_NAME))) return;
			File preExistingLogFile = new File(LOG_FILE_NAME);

			SimpleDateFormat dateFormat = AopConfig.getDateFormat();
			String archivedLogFileDestinationPath = String.format("%s/log-%s.txt", LOG_ARCHIVE_FOLDER, dateFormat.format(preExistingLogFile.lastModified()));
			Files.move(Path.of(LOG_FILE_NAME), Path.of(archivedLogFileDestinationPath));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}

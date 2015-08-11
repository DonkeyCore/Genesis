package genesis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class IOManager {
	
	public static final String folderPath = "Genesis/";
	private static final File genesisFolder = new File(folderPath);
	private static final Calendar cal = Calendar.getInstance();
	private static final File logFile = new File(folderPath + "log-" + cal.get(Calendar.HOUR_OF_DAY) + "_" + cal.get(Calendar.MINUTE) + "_" + cal.get(Calendar.SECOND) + "-" + (cal.get(Calendar.MONTH) + 1) + "_" + cal.get(Calendar.DAY_OF_MONTH) + "_" + cal.get(Calendar.YEAR) + ".log");
	private static final File responsesFile = new File(folderPath + "responses.txt");
	private static final Logger logger = Logger.getLogger("Genesis");
	private static FileHandler logFileHandler = createLogFileHandler(logFile, logger);
	private final Genesis genesis;
	private final PrintWriter responseWriter;
	
	public IOManager(Genesis g) throws IOException {
		this.genesis = g;
		genesisFolder.mkdirs();
		createFiles();
		responseWriter = new PrintWriter(new FileWriter(responsesFile, true));
	}
	
	private void createFiles() {
		try {
			if (!logFile.exists())
				logFile.createNewFile();
			if (!responsesFile.exists())
				responsesFile.createNewFile();
		} catch (IOException e) {
			genesis.logError(e);
		}
	}
	
	public Genesis getGenesis() {
		return genesis;
	}
	
	public File getFolder() {
		return genesisFolder;
	}
	
	public File getLog() {
		return logFile;
	}
	
	public File getResponsesFile() {
		return responsesFile;
	}
	
	public HashMap<ResponseType, List<String>> getResponses() {
		createFiles();
		HashMap<ResponseType, List<String>> res = new HashMap<>();
		if (responsesFile.length() == 0)
			return res;
		try (BufferedReader r = new BufferedReader(new FileReader(responsesFile))) {
			String line;
			while ((line = r.readLine()) != null) {
				for (ResponseType rt : ResponseType.values()) {
					if (line.split(":")[0].equalsIgnoreCase(rt.name())) {
						String response = "";
						for (int i = 1; i < line.split(":").length; i++)
							response = line.split(":")[i].trim();
						if (res.get(rt) == null) {
							List<String> list = new ArrayList<String>();
							list.add(response);
							res.put(rt, list);
						} else
							res.get(rt).add(response);
					}
				}
			}
		} catch (IOException e) {
			genesis.logError(e);
		}
		return res;
	}
	
	public List<String> getResponses(ResponseType rt) {
		List<String> res = new ArrayList<String>();
		if (responsesFile.length() == 0)
			return res;
		try (BufferedReader r = new BufferedReader(new FileReader(responsesFile))) {
			String line;
			while ((line = r.readLine()) != null) {
				if (line.split(":")[0].equalsIgnoreCase(rt.name()))
					res.add(line.split(":")[1].trim());
			}
		} catch (Throwable t) {
			genesis.logError(t);
		}
		return res;
	}
	
	public void setResponse(ResponseType type, String response) {
		response = response.trim();
		try (BufferedReader r = new BufferedReader(new FileReader(responsesFile))) {
			String s;
			while ((s = r.readLine()) != null) {
				if (s.equals(type.toString() + ": " + response))
					return;
			}
			responseWriter.println(type.toString() + ": " + response);
			responseWriter.flush();
		} catch (Throwable t) {
			genesis.logError(t);
		}
	}
	
	public void log(String message) {
		createFiles();
		log(Level.INFO, message);
	}
	
	public static void log(Level level, String message) {
		logger.log(level, message);
	}
	
	public PrintWriter getResponseWriter() {
		return responseWriter;
	}
	
	public void close() {
		responseWriter.close();
	}
	
	private static FileHandler createLogFileHandler(File f, Logger parent) {
		if (logFileHandler != null)
			return logFileHandler;
		try {
			logFileHandler = new FileHandler(f.getAbsolutePath());
			parent.addHandler(logFileHandler);
			parent.setUseParentHandlers(false);
			logFileHandler.setFormatter(new GenesisLogFormatter());
			return logFileHandler;
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
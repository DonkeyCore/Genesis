package genesis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class FileManager {
	
	private final Genesis g;
	public static final String folderPath = "Genesis/";
	private final File folder = new File(folderPath);
	private final File log = new File(folderPath + "log.txt");
	private final File resp = new File(folderPath + "responses.txt");
	private final PrintWriter lout;
	private final PrintWriter rout;
	
	public FileManager(Genesis g) throws IOException {
		this.g = g;
		folder.mkdirs();
		checkFiles();
		lout = new PrintWriter(new FileWriter(log, true));
		rout = new PrintWriter(new FileWriter(resp, true));
	}
	
	private void checkFiles() {
		try {
			if (!log.exists())
				log.createNewFile();
			if (!resp.exists())
				resp.createNewFile();
		} catch (Throwable t) {
			g.printError(t);
		}
	}
	
	public Genesis getGenesis() {
		return g;
	}
	
	public File getFolder() {
		return folder;
	}
	
	public File getLog() {
		return log;
	}
	
	public File getResponsesFile() {
		return resp;
	}
	
	public HashMap<ResponseType, List<String>> getResponses() {
		checkFiles();
		if (resp.length() == 0)
			return null;
		HashMap<ResponseType, List<String>> res = new HashMap<ResponseType, List<String>>();
		try (BufferedReader r = new BufferedReader(new FileReader(resp))) {
			String line;
			while ((line = r.readLine()) != null) {
				for (ResponseType rt : ResponseType.values()) {
					if (line.split("�")[0].equalsIgnoreCase(rt.name())) {
						String response = "";
						for (int i = 1; i < line.split("�").length; i++)
							response = line.split("�")[i].trim();
						if (res.get(rt) == null) {
							List<String> list = new ArrayList<String>();
							list.add(response);
							res.put(rt, list);
						} else
							res.get(rt).add(response);
					}
				}
			}
		} catch (Throwable t) {
			g.printError(t);
		}
		return res;
	}
	
	public List<String> getResponses(ResponseType rt) {
		checkFiles();
		if (resp.length() == 0)
			return null;
		List<String> res = new ArrayList<String>();
		try (BufferedReader r = new BufferedReader(new FileReader(resp))) {
			String line;
			while ((line = r.readLine()) != null) {
				if (line.split("�")[0].equalsIgnoreCase(rt.name()))
					res.add(line.split("�")[1].trim());
			}
		} catch (Throwable t) {
			g.printError(t);
		}
		return res;
	}
	
	public void setResponse(ResponseType type, String response) {
		response = response.trim();
		try (BufferedReader r = new BufferedReader(new FileReader(resp))) {
			String s;
			while ((s = r.readLine()) != null) {
				if (s.equals(type.toString() + "� " + response))
					return;
			}
			rout.println(type.toString() + "� " + response);
			rout.flush();
		} catch (Throwable t) {
			g.printError(t);
		}
	}
	
	public void log(String message) {
		checkFiles();
		
		String hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR) + "";
		String minute = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE) + "";
		String second = java.util.Calendar.getInstance().get(java.util.Calendar.SECOND) + "";
		int ampm = java.util.Calendar.getInstance().get(java.util.Calendar.AM_PM);
		
		if (Integer.parseInt(hour) < 10)
			hour = "0" + hour;
		if (Integer.parseInt(minute) < 10)
			minute = "0" + minute;
		if (Integer.parseInt(second) < 10)
			second = "0" + second;
		
		String timestamp = "[" + hour + ":" + minute + ":" + second + ":" + (ampm == 0 ? "AM" : "PM") + "]";
		
		lout.println(timestamp + ": " + message);
		lout.flush();
	}
	
	public PrintWriter getLogStream() {
		return lout;
	}
	
	public PrintWriter getResponseStream() {
		return rout;
	}
	
	public void close() {
		lout.close();
		rout.close();
	}
	
}
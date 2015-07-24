package genesis;

import genesis.Genesis.PrintWriterStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
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
		folder.mkdirs(); //make the Genesis folder if it doesn't exist
		checkFiles(); //make the files
		lout = new PrintWriter(new FileWriter(log, true));
		rout = new PrintWriter(new FileWriter(resp, true));
	}
	
	private void checkFiles() { //create new log and response files if they don't exist
		try {
			if (!log.exists())
				log.createNewFile();
			if (!resp.exists())
				resp.createNewFile();
		} catch (IOException e) {
			g.printError(e);
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
	
	public HashMap<ResponseType, List<String>> getResponses() { //get a hashmap of all responsetypes and responses for that type in list form
		checkFiles(); //make sure our files exist first
		HashMap<ResponseType, List<String>> res = new HashMap<ResponseType, List<String>>();
		if (resp.length() == 0) //if we don't have anything, don't return anything
			return res;
		try (BufferedReader r = new BufferedReader(new FileReader(resp))) {
			String line;
			while ((line = r.readLine()) != null) {
				for (ResponseType rt : ResponseType.values()) {
					if (line.split("�")[0].equalsIgnoreCase(rt.name())) { //I could use a different character... but it kept changing back when I moved the project between computers
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
		} catch (IOException e) {
			g.printError(e);
		}
		return res;
	}
	
	public List<String> getResponses(ResponseType rt) { //get all the responses in list form for a certain response type
		return getResponses().get(rt);
		/*checkFiles(); //commented out just in case
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
		return res;*/
	}
	
	public void setResponse(ResponseType type, String response) { //set a response for a certain type
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
		log(lout, message);
	}
	
	public static void log(Object output, String message) {
		PrintWriterStream out;
		if(output instanceof PrintWriter)
			out = new PrintWriterStream((PrintWriter) output);
		else if(output instanceof PrintStream)
			out = new PrintWriterStream((PrintStream) output);
		else
			throw new IllegalArgumentException("Output must be of type PrintWriter or PrintStream");
		Calendar c = Calendar.getInstance();
		String hour = c.get(Calendar.HOUR) + "";
		String minute = c.get(Calendar.MINUTE) + "";
		String second = c.get(Calendar.SECOND) + "";
		int ampm = c.get(Calendar.AM_PM);
		
		if(Integer.parseInt(hour) < 10) //keep the hours minutes and seconds 2 digits
			hour = "0" + hour;
		if(Integer.parseInt(minute) < 10)
			minute = "0" + minute;
		if(Integer.parseInt(second) < 10)
			second = "0" + second;
		
		String timestamp = "[" + hour + ":" + minute + ":" + second + ":" + (ampm == 0 ? "AM" : "PM") + "]";
		
		out.println(timestamp + ": " + message);
		out.flush();
	}
	
	public PrintWriter getLogStream() {
		return lout;
	}
	
	public PrintWriter getResponseStream() {
		return rout;
	}
	
	public void close() { //close the log and response file streams
		lout.close();
		rout.close();
	}
	
}
package genesis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Genesis {
	
	public static final String name = "Genesis";
	public static final String version = "1.0.1";
	
	public static void main(String... args) {
		try {
			new Genesis();
		} catch (IOException e) {
			logError(Thread.currentThread(), e);
		}
	}
	
	private final IOManager iomanager;
	private String lastMessage;
	
	public Genesis() throws IOException {
		Thread.setDefaultUncaughtExceptionHandler(Genesis::logError);
		log("Initializing " + toString() + "...");
		log("Generating files...");
		iomanager = new IOManager(this);
		log(toString() + " started on " + System.getProperty("os.name"), true);
		start();
		stop();
	}
	
	public void stop() {
		stop(0);
	}
	
	public static void stop(int error) {
		if (error == 0)
			log(name + " v" + version + " shut down successfully!", true);
		else {
			log("Uh oh! Something bad happened: please report the error code below and attach the most recent log file.");
			log(name + " v" + version + " shut down with error code: " + error, true);
		}
		System.exit(error);
	}
	
	public void start() {
		try (BufferedReader r = new BufferedReader(new InputStreamReader(System.in))) {
			System.out.print("You: ");
			String s = r.readLine();
			if (respond(s))
				start();
		} catch (Throwable t) {
			if (iomanager != null)
				logError(t, 1);
		}
	}
	
	public boolean respond(String message) {
		if (message.trim().equals(""))
			return true;
		String response = "";
		if (lastMessage == null) {
			if (message.trim().equalsIgnoreCase("exit"))
				return false;
			boolean newGreeting = true;
			for (String r : iomanager.getResponses(ResponseType.GREETING)) {
				if (transform(message).equalsIgnoreCase(transform(r)))
					newGreeting = false;
			}
			if (newGreeting)
				iomanager.setResponse(ResponseType.GREETING, removeEndPunctuation(message));
			response = (name + ": " + format(iomanager.getResponses(ResponseType.GREETING).get((int) (System.nanoTime() % iomanager.getResponses(ResponseType.GREETING).size()))));
			System.out.println(response);
		} else {
			boolean isGreeting = false;
			for (String r : iomanager.getResponses(ResponseType.GREETING)) { //check if THE LAST MESSAGE is another greeting
				if (transform(lastMessage).equalsIgnoreCase(transform(r)))
					isGreeting = true;
			}
			boolean isFarewell = false;
			for (String r : iomanager.getResponses(ResponseType.FAREWELL)) {
				if (transform(message).equalsIgnoreCase(transform(r)))
					isFarewell = true;
			}
			if (isFarewell || message.equalsIgnoreCase("exit")) { //giving a farewell & last message isn't a greeting
				List<String> f = iomanager.getResponses(ResponseType.FAREWELL);
				if (message.equalsIgnoreCase("exit") && !isGreeting) {
					boolean newFarewell = true;
					for (String r : f) { //check if it's a new farewell
						if (transform(lastMessage).equalsIgnoreCase(transform(r)))
							newFarewell = false;
					}
					if (newFarewell) //if it's new, store it for another session (or this one) IF AND ONLY IF we are using "exit"
						iomanager.setResponse(ResponseType.FAREWELL, removeEndPunctuation(lastMessage));
				}
				//say bye back
				if (f != null && f.size() > 0) {
					response = (name + ": " + format(f.get((int) (System.nanoTime() % f.size()))));
					System.out.println(response);
				}
				return false; //exit the program
			}
		}
		boolean containsLaugh = false;
		for (String r : iomanager.getResponses(ResponseType.LAUGH)) {
			if (message.matches(".*?\\b" + r + "\\b.*?"))
				containsLaugh = true;
		}
		boolean laughIfPossible = false;
		int laughCounter = 0;
		for (char c : message.toCharArray()) {
			if (c == 'h' || c == 'l') //measure the h's in l's in a message to determine a laugh (e.g. lolol or haha)
				laughCounter++;
		}
		if (laughCounter >= message.toCharArray().length / 2 && !iomanager.getResponses(ResponseType.GREETING).stream().anyMatch((g) -> {
			return transform(g).equalsIgnoreCase(transform(message));
		})) {
			boolean newLaugh = true;
			for (String r : iomanager.getResponses(ResponseType.LAUGH)) {
				if (transform(message).equalsIgnoreCase(transform(r)))
					newLaugh = false;
			}
			if (newLaugh)
				iomanager.setResponse(ResponseType.LAUGH, removeEndPunctuation(message));
			laughIfPossible = true;
		}
		if (!containsLaugh) {
			String[] set = message.split("(?i)(\\s+is\\s+|\\'s\\s+)");
			try { //if it's math, solve it
				response = (name + ": " + solve(transform(set[1]).trim()));
				System.out.println(response);
			} catch (Throwable t) { //it's not math
				String rawKey = transform(set[0]);
				if (rawKey.toLowerCase().contains("what")) {
					String key = transform(reversePerson(join(set, "is", 1)));
					for (String values : iomanager.getResponses(ResponseType.VALUE)) {
						if (transform(values.split("��=��")[0]).trim().equalsIgnoreCase(key))
							response = name + ": " + capitalize(key) + " is " + values.split("��=��")[1].trim() + addPunctuation();
					}
					if (!response.equals(""))
						System.out.println(response);
				} else if (message.toLowerCase().contains(" is ")) {
					String key = reversePerson(message.split(" is ")[0].trim());
					String value = join(message.split("(?i) (is) "), "$1%s", 1).trim();
					iomanager.setResponse(ResponseType.VALUE, key + "��=��" + reversePerson(removeEndPunctuation(value)));
					response = (name + ": " + capitalize(key) + " is " + removeEndPunctuation(value) + addPunctuation());
					System.out.println(response);
				}
			}
		}
		if (response.trim().equals("") && (laughIfPossible || containsLaugh)) {
			response = (name + ": " + capitalize(iomanager.getResponses(ResponseType.LAUGH).get(((int) (System.nanoTime() % iomanager.getResponses(ResponseType.LAUGH).size())))));
			System.out.println(response);
		}
		iomanager.log("You: " + message);
		iomanager.log(name + ": " + (response.replace(name + ": ", "")));
		lastMessage = message;
		return true;
	}
	
	private static String join(String[] set, String medium, int offset) {
		String s = set[offset];
		int i = 0;
		for (String part : set) {
			if (i > offset)
				s = s + " " + medium + " " + part;
			i++;
		}
		return s;
	}
	
	private static String reversePerson(String s) {
		return s.replaceAll("(?i)\\byour\\b", "����m����y����").replaceAll("(?i)\\byou\\b", "����m����e����").replaceAll("(?i)\\bme\\b", "you").replaceAll("(?i)\\bmy\\b", "your").replaceAll("(?i)\\byours\\b", "����mi����ne����").replaceAll("(?i)\\bmine\\b", "yours").replace("����", "").trim();
	}
	
	public static double solve(String c) {
		Pattern p = Pattern.compile("(\\d+|\\d+\\.\\d+)\\s*(\\+|\\-|\\*|\\/|\\%|\\|)\\s*(\\d+|\\d+\\.\\d+).*");
		Matcher m = p.matcher(c);
		if (m.matches()) {
			Double d1 = Double.parseDouble(m.group(1));
			Double d2 = Double.parseDouble(m.group(3));
			while (c.contains("+") || c.contains("-") || c.contains("*") || c.contains("/") || c.contains("%") || c.contains("|")) {
				c = c.replaceAll("(\\d)\\.0(\\D)", "$1$2");
				m = p.matcher(c);
				if (!m.matches())
					throw new ArithmeticException("Invalid math expression: " + c);
				switch (m.group(2)) {
					default:
						break;
					case "+":
						c = c.replaceAll("[" + d1 + "]\\s*\\+\\s*[" + d2 + "]", (d1 + d2) + "");
						break;
					case "-":
						c = c.replaceAll("[" + d1 + "]\\s*\\-\\s*[" + d2 + "]", (d1 - d2) + "");
						break;
					case "*":
						c = c.replaceAll("[" + d1 + "]\\s*\\*\\s*[" + d2 + "]", (d1 * d2) + "");
						break;
					case "/":
						c = c.replaceAll("[" + d1 + "]\\s*\\/\\s*[" + d2 + "]", (d1 / d2) + "");
						break;
					case "%":
						c = c.replaceAll("[" + d1 + "]\\s*%\\s*[" + d2 + "]", (d1 % d2) + "");
						break;
					case "|":
						c = c.replaceAll("[" + d1 + "]\\s*\\|\\s*[" + d2 + "]", (Integer.parseInt((d1 + "").replace(".0", "")) | Integer.parseInt((d2 + "").replace(".0", ""))) + "");
						break;
				}
			}
		}
		return Double.parseDouble(c);
	}
	
	private static String transform(String s) {
		return s.replace("?", "").replace(".", "").replace("!", "").replace(",", "").replace("_", "").replace("~", "").replace("`", "").replace("'", "").replace("\"", "").replace("\\", "").replace(":", "").replace(";", "").replaceAll("(?i)the", " ").replaceAll("(?i)teh", " ").replaceAll("(?i)how\\s+do", "how can").replaceAll("(?i)re", "").replaceAll("(?i)\\s+a ", " ").replaceAll("(?i)is", "").replaceAll("(?i)has", "").replaceAll("(?i)get to", "go to").replaceAll("\\Bs\\b", "").replaceAll(" {2}?", "").trim();
	}
	
	private static String removeEndPunctuation(String s) {
		return s.replaceAll("[!\\.\\?]+$", "");
	}
	
	private static String format(String s) {
		return capitalize(s) + addPunctuation();
	}
	
	private static String capitalize(String s) {
		String r = s.toUpperCase();
		if (s.length() > 1)
			r = s.replaceFirst(s.substring(0, 1), s.substring(0, 1).toUpperCase());
		return r;
	}
	
	private static char addPunctuation() {
		switch ((int) System.nanoTime() % 5) {
			case 0:
				return '!';
			default:
				return '.';
		}
	}
	
	public IOManager getIOManager() {
		return iomanager;
	}
	
	public void logError(Throwable t) {
		logError(t, 0);
	}
	
	public void logError(Throwable t, int fatal) {
		logError(Thread.currentThread(), t, fatal);
	}
	
	private static void logError(Thread thread, Throwable t) {
		logError(thread, t, 0);
	}
	
	private static void logError(Thread thread, Throwable t, int fatal) {
		IOManager.log(Level.SEVERE, "");
		IOManager.log(Level.SEVERE, "A fatal error occurred: " + t.toString());
		IOManager.log(Level.SEVERE, "Thread: " + thread.getName());
		IOManager.log(Level.SEVERE, "");
		IOManager.log(Level.SEVERE, "-----=[Full Stack Trace]=-----");
		for (StackTraceElement s : t.getStackTrace()) //print the throwable's stack trace
			IOManager.log(Level.SEVERE, s.getClassName() + "." + s.getMethodName() + "() on line " + s.getLineNumber());
		IOManager.log(Level.SEVERE, "-----=[" + name + " Stack Trace]=-----");
		IOManager.log(Level.SEVERE, "");
		IOManager.log(Level.SEVERE, "-----=[" + name + " Stack Trace]=-----");
		boolean fault = false;
		for (StackTraceElement s : t.getStackTrace()) { //filter out the stack trace for only Genesis
			if (s.getClassName().startsWith("genesis")) {
				IOManager.log(Level.SEVERE, s.getClassName() + "." + s.getMethodName() + "() on line " + s.getLineNumber());
				fault = true;
			}
		}
		if (!fault) //if it's not our fault, tell the user
			IOManager.log(Level.SEVERE, "This doesn't look like a problem relating to " + name + ". Check the below remote stack trace.");
		IOManager.log(Level.SEVERE, "-----=[Genesis Stack Trace]=-----");
		IOManager.log(Level.SEVERE, "");
		IOManager.log(Level.SEVERE, "-----=[Remote Stack Trace]=-----");
		fault = false;
		for (StackTraceElement s : t.getStackTrace()) { //filter out the stack trace for only outside Genesis
			if (!s.getClassName().startsWith("genesis")) {
				IOManager.log(Level.SEVERE, s.getClassName() + "." + s.getMethodName() + "() on line " + s.getLineNumber());
				fault = true;
			}
		}
		if (!fault) //if it's not their fault, tell the user
			IOManager.log(Level.SEVERE, "This doesn't look like a problem with anything outside " + name + ". Check the above " + name + " stack trace.");
		IOManager.log(Level.SEVERE, "-----=[Remote Stack Trace]=-----");
		IOManager.log(Level.SEVERE, "");
		if (fatal != 0)
			stop(fatal);
	}
	
	public static void log(String message) {
		log(message, false);
	}
	
	public static void log(String message, boolean withConsole) {
		log(System.out, message, withConsole);
	}
	
	public static void log(PrintStream out, String message, boolean withConsole) {
		IOManager.log(Level.INFO, message);
		if (withConsole)
			System.out.println(message);
	}
	
	public String toString() {
		return name + " v" + version;
	}
	
	static class PrintWriterStream {
		
		private PrintWriter w;
		private PrintStream s;
		
		PrintWriterStream(PrintWriter w) { //support for PrintWriter
			if (w == null)
				throw new NullPointerException();
			this.w = w;
		}
		
		PrintWriterStream(PrintStream s) { //support for PrintStream
			if (s == null)
				throw new NullPointerException();
			this.s = s;
		}
		
		void println() {
			if (w == null && s != null)
				s.println();
			else if (s == null && w != null)
				w.println();
			else
				throw new NullPointerException("No valid output");
		}
		
		void println(String x) {
			if (w == null && s != null)
				s.println(x);
			else if (s == null && w != null)
				w.println(x);
			else
				throw new NullPointerException("No valid output");
		}
		
		public void flush() {
			if (w == null && s != null)
				s.flush();
			else if (s == null && w != null)
				w.flush();
			else
				throw new NullPointerException("No valid output");
		}
	}
}

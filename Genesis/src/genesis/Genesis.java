package genesis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Genesis {
	
	public static final String name = "Genesis";
	public static final String version = "1.0.1";
	
	public static void main(String[] args) {
		try {
			new Genesis();
		} catch(IOException e) {
			printError(new PrintWriter(System.err), e);
		}
	}
	
	private final FileManager fm;
	protected String last = null;
	
	public Genesis() throws IOException {
		log("Initializing " + toString() + "...");
		log("Generating files...");
		fm = new FileManager(this);
		log(toString() + " started on " + System.getProperty("os.name"));
		start();
		stop();
	}
	
	public void stop() {
		stop(0);
	}
	
	public void stop(int error) {
		if(error == 0)
			log(toString() + " shut down successfully!");
		else
			log(toString() + " shut down with error code: " + error);
		if(fm != null) {
			fm.getLogStream().println();
			fm.close();
		}
		System.exit(error);
	}
	
	public void start() {
		try(BufferedReader r = new BufferedReader(new InputStreamReader(System.in))) {
			System.out.print("You: ");
			String s = r.readLine();
			if(respond(s))
				start();
		} catch(Throwable t) {
			printError(t);
		}
	}
	
	public boolean respond(String s) {
		if(s.trim().equals(""))
			return true;
		String response = "";
		if(last == null) {
			if(s.trim().equalsIgnoreCase("exit"))
				return false;
			boolean newg = true;
			for(String r : fm.getResponses(ResponseType.GREETING)) {
				if(transform(s).equalsIgnoreCase(transform(r)))
					newg = false;
			}
			if(newg)
				fm.setResponse(ResponseType.GREETING, removeEndPunc(s));
			System.out.println(response = (name + ": " + format(fm.getResponses(ResponseType.GREETING).get((int) (System.nanoTime() % fm.getResponses(ResponseType.GREETING).size())))));
		} else {
			boolean notg = true;
			for(String r : fm.getResponses(ResponseType.GREETING)) { //check if THE LAST MESSAGE is another greeting
				if(transform(last).equalsIgnoreCase(transform(r)))
					notg = false;
			}
			boolean notf = true;
			for(String r : fm.getResponses(ResponseType.FAREWELL)) {
				if(transform(s).equalsIgnoreCase(transform(r)))
					notf = false;
			}
			if((!notf || s.equalsIgnoreCase("exit")) && notg) {
				boolean newf = true;
				for(String r : fm.getResponses(ResponseType.FAREWELL)) { //check if it's a new farewell
					if(transform(last).equalsIgnoreCase(transform(r)))
						newf = false;
				}
				if(newf) //if it's new, store it for another session (or this one)
					fm.setResponse(ResponseType.FAREWELL, removeEndPunc(last));
				//say bye back
				System.out.println(response = (name + ": " + format(fm.getResponses(ResponseType.FAREWELL).get((int) (System.nanoTime() % fm.getResponses(ResponseType.FAREWELL).size())))));
				return false; //exit the program
			}
		}
		boolean containsLaugh = false;
		for(String r : fm.getResponses(ResponseType.LAUGH)) {
			if(s.matches(".*?\\b" + r + "\\b.*?"))
				containsLaugh = true;
		}
		boolean laughIfPossible = false;
		int laugh = 0;
		for(char c : s.toCharArray()) {
			if(c == 'h' || c == 'l')
				laugh++;
		}
		if(laugh > s.toCharArray().length / 2 && !fm.getResponses(ResponseType.GREETING).stream().anyMatch((g) -> {
			return transform(g).equalsIgnoreCase(transform(s));
		})) {
			boolean newl = true;
			for(String r : fm.getResponses(ResponseType.LAUGH)) {
				if(transform(s).equalsIgnoreCase(transform(r)))
					newl = false;
			}
			if(newl)
				fm.setResponse(ResponseType.LAUGH, removeEndPunc(s));
			laughIfPossible = true;
		}
		if(!containsLaugh) {
			String[] set = s.split("(\\s+is\\s+|\\'s\\s+)");
			try { //if it's math, solve it
				System.out.println(response = (name + ": " + solve(transform(set[1]).trim())));
			} catch(Throwable t) { //it's not math
				String ek = transform(set[0]);
				if(ek.contains("what")) {
					String k = transform(reversePerson(join(set, "is", 1)));
					for(String values : fm.getResponses(ResponseType.VALUE)) {
						if(transform(values.split("§=§")[0]).trim().equalsIgnoreCase(k))
							response = name + ": " + cap(k) + " is " + values.split("§=§")[1].trim() + punc();
					}
					if(!response.equals(""))
						System.out.println(response);
				} else if(s.contains(" is ")) {
					String k = reversePerson(s.split(" is ")[0].trim());
					String v = join(s.split("(?i) (is) "), "$1%s", 1).trim();
					fm.setResponse(ResponseType.VALUE, k + "§=§" + reversePerson(removeEndPunc(v)));
					System.out.println(response = (name + ": " + cap(k) + " is " + removeEndPunc(v) + punc()));
				}
			}
		}
		if(response.trim().equals("") && (laughIfPossible || containsLaugh))
			System.out.println(response = (name + ": " + cap(fm.getResponses(ResponseType.LAUGH).get(((int) (System.nanoTime() % fm.getResponses(ResponseType.LAUGH).size()))))));
		fm.log("You: " + s);
		fm.log(name + ": " + (response.replace(name + ": ", "")));
		last = s;
		return true;
	}
	
	private static String join(String[] set, String medium, int offset) {
		String s = set[offset];
		int i = 0;
		for(String part : set) {
			if(i > offset)
				s = s + " " + medium + " " + part;
			i++;
		}
		return s;
	}
	
	private static String reversePerson(String s) {
		return s.replaceAll("(?i)\\byour\\b", "§§m§§y§§").replaceAll("(?i)\\byou\\b", "§§m§§e§§").replaceAll("(?i)\\bme\\b", "you").replaceAll("(?i)\\bmy\\b", "your").replaceAll("(?i)\\byours\\b", "§§mi§§ne§§").replaceAll("(?i)\\bmine\\b", "yours").replace("§§", "").trim();
	}
	
	public static double solve(String c) {
		Pattern p = Pattern.compile("(\\d+|\\d+\\.\\d+)\\s*(\\+|\\-|\\*|\\/|\\%|\\|)\\s*(\\d+|\\d+\\.\\d+).*");
		Matcher m = p.matcher(c);
		if(m.matches()) {
			Double d1 = Double.parseDouble(m.group(1));
			Double d2 = Double.parseDouble(m.group(3));
			while(c.contains("+") || c.contains("-") || c.contains("*") || c.contains("/") || c.contains("%") || c.contains("|")) {
				c = c.replaceAll("(\\d)\\.0(\\D)", "$1$2");
				m = p.matcher(c);
				if(!m.matches())
					throw new ArithmeticException();
				switch(m.group(2)) {
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
	
	private static String removeEndPunc(String s) {
		return s.replaceAll("[!\\.\\?]+$", "");
	}
	
	private static String format(String s) {
		return cap(s) + punc();
	}
	
	private static String cap(String s) {
		String r = s.toUpperCase();
		if(s.length() > 1)
			r = s.replaceFirst(s.substring(0, 1), s.substring(0, 1).toUpperCase());
		return r;
	}
	
	private static char punc() {
		switch((int) System.nanoTime() % 5) {
			default:
			case 0:
			case 1:
			case 2:
			case 3:
				return '.';
			case 4:
				return '!';
		}
	}
	
	public FileManager getFileManager() {
		return fm;
	}
	
	public void printError(Throwable t) {
		printError(System.err, t);
		if(fm != null)
			printError(fm.getLogStream(), t);
		stop(1);
	}
	
	private static void printError(Object output, Throwable t) {
		PrintWriterStream out;
		if(output instanceof PrintWriter)
			out = new PrintWriterStream((PrintWriter) output);
		else if(output instanceof PrintStream)
			out = new PrintWriterStream((PrintStream) output);
		else
			throw new IllegalArgumentException("Output must be of type PrintWriter or PrintStream");
		out.println();
		out.println("A fatal error occurred: " + t.toString());
		out.println();
		out.println("-----=[General Stack Trace]=-----");
		for(StackTraceElement s : t.getStackTrace())
			//print the throwable's stack trace
			out.println(s.getClassName() + "." + s.getMethodName() + "() on line " + s.getLineNumber());
		out.println("-----=[" + name + " Stack Trace]=-----");
		out.println();
		out.println("-----=[" + name + " Stack Trace]=-----");
		boolean fault = false;
		for(StackTraceElement s : t.getStackTrace()) { //filter out the stack trace for only Genesis
			if(s.getClassName().startsWith("genesis")) {
				out.println(s.getClassName() + "." + s.getMethodName() + "() on line " + s.getLineNumber());
				fault = true;
			}
		}
		if(!fault) //if it's not our fault, tell the user
			out.println("This doesn't look like a problem relating to " + name + ". Check the above general stack trace.");
		out.println("-----=[Genesis Stack Trace]=-----");
		out.println();
		out.println("-----=[Remote Stack Trace]=-----");
		fault = false;
		for(StackTraceElement s : t.getStackTrace()) { //filter out the stack trace for only outside Genesis
			if(!s.getClassName().startsWith("genesis")) {
				out.println(s.getClassName() + "." + s.getMethodName() + "() on line " + s.getLineNumber());
				fault = true;
			}
		}
		if(!fault) //if it's not their fault, tell the user
			out.println("This doesn't look like a problem with anything outside " + name + ". Check the above " + name + " stack trace.");
		out.println("-----=[Remote Stack Trace]=-----");
		out.println();
	}
	
	public void log(String message) {
		log(System.out, message);
	}
	
	public void log(PrintStream out, String message) {
		FileManager.log(out, message);
		if(fm != null)
			fm.log(message);
	}
	
	public String toString() {
		return name + " v" + version;
	}
	
	static class PrintWriterStream { //super hacky way of combining a PrintWriter and a PrintStream
	
		private PrintWriter w;
		private PrintStream s;
		
		PrintWriterStream(PrintWriter w) { //support for PrintWriter
			if(w == null)
				throw new NullPointerException();
			this.w = w;
		}
		
		PrintWriterStream(PrintStream s) { //support for PrintStream
			if(s == null)
				throw new NullPointerException();
			this.s = s;
		}
		
		void println() {
			if(w == null && s != null)
				s.println();
			else if(s == null && w != null)
				w.println();
			else
				throw new NullPointerException("No valid output");
		}
		
		void println(String x) {
			if(w == null && s != null)
				s.println(x);
			else if(s == null && w != null)
				w.println(x);
			else
				throw new NullPointerException("No valid output");
		}
		
		public void flush() {
			if(w == null && s != null)
				s.flush();
			else if(s == null && w != null)
				w.flush();
			else
				throw new NullPointerException("No valid output");
		}
	}
}

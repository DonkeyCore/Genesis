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
	public static final String version = "1.0";
	
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
		if(fm != null)
			fm.close();
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
	
	public boolean respond(String s) { //decide how and what to respond, return if we should keep the program alive
		if(s.trim().equals(""))
			return true; //nothing to do, but keep the program alive
		String response = "";
		if(last == null) { //first message must always be a greeting
			boolean newg = true;
			for(String r : fm.getResponses(ResponseType.GREETING)) {
				if(transform(s).equalsIgnoreCase(transform(r)))
					newg = false; //if this is a greeting that we know about, we dont need to store it again
			}
			if(newg) //store a new greeting for use in another session (or this one)
				fm.setResponse(ResponseType.GREETING, removeEndPunc(s));
			//give a greeting back!
			System.out.println(response = (name + ": " + format(fm.getResponses(ResponseType.GREETING).get((int) (System.nanoTime() % fm.getResponses(ResponseType.GREETING).size())))));
		} else {
			boolean notg = true;
			for(String r : fm.getResponses(ResponseType.GREETING)) { //check if THE LAST MESSAGE is another greeting
				if(transform(last).equalsIgnoreCase(transform(r)))
					notg = false;
			}
			boolean notf = true;
			for(String r : fm.getResponses(ResponseType.FAREWELL)) { //check if they're saying a known farewell
				if(transform(s).equalsIgnoreCase(transform(r)))
					notf = false;
			}
			if((!notf || s.equalsIgnoreCase("exit")) && notg) { //if they're doing a farewell or saying "exit", and THE LAST MESSAGE is not a greeting
				boolean newf = true;
				for(String r : fm.getResponses(ResponseType.FAREWELL)) { //check if it's a new farewell
					if(transform(last).equalsIgnoreCase(transform(r)))
						newf = false;
				}
				if(newf) //if it's new, store it for another session (or this one)
					fm.setResponse(ResponseType.FAREWELL, removeEndPunc(last));
				//say bye back
				System.out.println(response = (name + ": " + format(fm.getResponses(ResponseType.FAREWELL).get((int) (System.nanoTime() % fm.getResponses(ResponseType.FAREWELL).size())))));
				return false; //exit the loop
			}
		}
		
		boolean containsLaugh = false;
		for(String r : fm.getResponses(ResponseType.LAUGH)) { //are they laughing?
			if(s.matches(".*?\\b" + r + "\\b.*?"))
				containsLaugh = true;
		}
		boolean laughIfPossible = false;
		int laugh = 0;
		for(char c : s.toCharArray()) { //very bad laugh detection: >50% h's or l's
			if(c == 'h' || c == 'l')
				laugh++;
		}
		if(laugh > s.toCharArray().length / 2) { //if >50% h's or l's
			boolean newl = true;
			for(String r : fm.getResponses(ResponseType.LAUGH)) { //is this a laugh we know?
				if(transform(s).equalsIgnoreCase(transform(r)))
					newl = false;
			}
			if(newl) //if it's new, save it for later
				fm.setResponse(ResponseType.LAUGH, removeEndPunc(s));
			laughIfPossible = true; //if there's nothing else to say later, laugh
		}
		
		if(!containsLaugh) { //if super serious business mode is on
			String[] set = s.split("(\\s+is\\s+|\\'s\\s+)"); //regex: split for every "is" or 's ('s as in contraction for is)
			try { //if it's math, solve it
				System.out.println(response = (name + ": " + solve(transform(set[1]).trim())));
			} catch(Throwable t) { //it's not math
				String ek = transform(set[0]).toLowerCase(); //get the first part of the phrase
				if(ek.contains("what")) { //user is asking for information
					String k = transform(reversePerson(join(set, "is", 1).toLowerCase())); //get the object to look up
					for(String values : fm.getResponses(ResponseType.VALUE)) {
						if(transform(values.split("§=§")[0]).trim().equalsIgnoreCase(k)) //if we know the information, tell the user
							response = name + ": " + cap(k) + " is " + values.split("§=§")[1].trim() + punc();
					}
					if(!response.equals("")) //only respond if we have something useful to say
						System.out.println(response);
				} else if(s.contains(" is ")) { //the user is telling us information
					String k = reversePerson(s.split(" is ")[0].toLowerCase().trim()); //the key to store
					String v = join(s.split(" is "), "is", 1).toLowerCase().trim(); //the value to store for the key
					fm.setResponse(ResponseType.VALUE, k + "§=§" + reversePerson(removeEndPunc(v))); //store the key and value
					System.out.println(response = (name + ": " + cap(k) + " is " + removeEndPunc(v) + punc())); //tell the user about our new information
				}
			}
		}
		if(response.trim().equals("") && (laughIfPossible || containsLaugh)) //if we have nothing else to say, but we can laugh, laugh
			System.out.println(response = (name + ": " + cap(fm.getResponses(ResponseType.LAUGH).get((int) (System.nanoTime() % fm.getResponses(ResponseType.LAUGH).size()))))); //say a random laugh
		fm.log("You: " + s); //log what the user said
		fm.log(name + ": " + (response.replace(name + ": ", ""))); //log our response, make sure to include the name even if we didn't earlier
		last = s; //set the new last message
		return true; //keep the program alive
	}
	
	private static String join(String[] set, String medium, int offset) { //join an array together with a specified string in between starting at a specified offset
		String s = set[offset];
		int i = 0;
		for(String part : set) {
			if(i > offset)
				s = s + " " + medium + " " + part;
			i++;
		}
		return s;
	}
	
	private static String reversePerson(String s) { //reverse between 1st and 3rd person, so Genesis makes sense
		return s.replaceAll("\\byour\\b", "§§m§§y§§").replaceAll("\\byou\\b", "§§m§§e§§").replaceAll("\\bme\\b", "you").replaceAll("\\bmy\\b", "your").replaceAll("\\byours\\b", "§§mi§§ne§§").replaceAll("\\bmine\\b", "yours").replace("§§", "").trim();
	}
	
	public static double solve(String c) { //solve math expressions
		Pattern p = Pattern.compile("(\\d+|\\d+\\.\\d+)\\s*(\\+|\\-|\\*|\\/|\\%|\\|)\\s*(\\d+|\\d+\\.\\d+).*"); //<number> <+-*/%|> <number>
		Matcher m = p.matcher(c);
		if(m.matches()) { //if this is a correct math expression
			Double d1 = Double.parseDouble(m.group(1));
			Double d2 = Double.parseDouble(m.group(3));
			while(c.contains("+") || c.contains("-") || c.contains("*") || c.contains("/") || c.contains("%") || c.contains("|")) { //checking for valid operations
				c = c.replaceAll("(\\d)\\.0(\\D)", "$1$2"); //replace all x.0 with just x (it looks better)
				m = p.matcher(c);
				if(!m.matches()) //this SHOULD match
					throw new ArithmeticException();
				switch(m.group(2)) { //check the operation symbol and do math
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
		} //else, maybe it's just a number (return the number)... or maybe it's not even math - if it is, return the parsed answer
		return Double.parseDouble(c);
	}
	
	private static String transform(String s) { //transform a string into something the program can read
		return s.toLowerCase().replace("?", "").replace(".", "").replace("!", "").replace(",", "").replace("_", "").replace("~", "").replace("`", "").replace("'", "").replace("\"", "").replace("\"", "").replace("\\", "").replace(":", "").replace(";", "").replace("the", " ").replace("teh", " ").replace("how do", "how can").replace("re", "").replace(" a ", " ").replace("is", "").replace("has", "").replace("get to", "go to").replaceAll("\\Bs\\b", "").replaceAll(" {2}?", "").trim();
	}
	
	private static String removeEndPunc(String s) {
		return s.replaceAll("[!\\.\\?]+$", ""); //remove all !'s .'s and ?'s from the end of a string
	}
	
	private static String format(String s) { //add random punctuation, and capitalize the first character
		return cap(s) + punc();
	}
	
	private static String cap(String s) { //capitalize the first letter of a given string
		String r = s.toUpperCase();
		if(r.length() > 1)
			r = s.replaceFirst(s.substring(0, 1), s.substring(0, 1).toUpperCase());
		return r;
	}
	
	private static char punc() { //return random punctuation
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

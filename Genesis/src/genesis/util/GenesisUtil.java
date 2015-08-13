package genesis.util;

import static genesis.Genesis.name;
import static genesis.Genesis.version;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import genesis.logging.LogManager;

public class GenesisUtil {
	
	public static void stop(int error) {
		if (error == 0)
			log(name + " v" + version + " shut down successfully!", true);
		else {
			log("Uh oh! Something bad happened: please report the error code below and attach the most recent log file.", true);
			log(name + " v" + version + " shut down with error code: " + error, true);
		}
		System.exit(error);
	}
	
	public static String join(String[] set, String medium, int offset) {
		String s = set[offset];
		int i = 0;
		for (String part : set) {
			if (i > offset)
				s = s + " " + medium + " " + part;
			i++;
		}
		return s;
	}
	
	public static String reversePerson(String s) {
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
	
	public static String transform(String s) {
		return s.replace("?", "").replace(".", "").replace("!", "").replace(",", "").replace("_", "").replace("~", "").replace("`", "").replace("'", "").replace("\"", "").replace("\\", "").replace(":", "").replace(";", "").replaceAll("(?i) the ", " ").replaceAll("(?i) teh ", " ").replaceAll("(?i)how\\s+do", "how can").replaceAll("(?i)re", "").replaceAll("(?i)\\s+a ", " ").replaceAll("(?i)\\s+is\\s+", "").replaceAll("(?i) has", "").replaceAll("(?i)get to", "go to").replaceAll(" {2}?", "").trim();
	}
	
	public static String removeEndPunctuation(String s) {
		return s.replaceAll("[!\\.\\?]+$", "");
	}
	
	public static String format(String s) {
		return capitalize(s) + addPunctuation();
	}
	
	public static String capitalize(String s) {
		String r = s.toUpperCase();
		if (s.length() > 1)
			r = s.replaceFirst(s.substring(0, 1), s.substring(0, 1).toUpperCase());
		return r;
	}
	
	public static char addPunctuation() {
		switch ((int) System.nanoTime() % 5) {
			case 0:
				return '!';
			default:
				return '.';
		}
	}
	
	public static void logError(Thread thread, Throwable t) {
		logError(thread, t, 0);
	}
	
	public static void logError(Thread thread, Throwable t, int fatal) {
		LogManager.log(Level.SEVERE, "");
		LogManager.log(Level.SEVERE, "A fatal error occurred: " + t.toString());
		LogManager.log(Level.SEVERE, "Thread: " + thread.getName());
		LogManager.log(Level.SEVERE, "");
		LogManager.log(Level.SEVERE, "-----=[Full Stack Trace]=-----");
		for (StackTraceElement s : t.getStackTrace()) //print the throwable's stack trace
			LogManager.log(Level.SEVERE, s.getClassName() + "." + s.getMethodName() + "() on line " + s.getLineNumber());
		LogManager.log(Level.SEVERE, "-----=[" + name + " Stack Trace]=-----");
		LogManager.log(Level.SEVERE, "");
		LogManager.log(Level.SEVERE, "-----=[" + name + " Stack Trace]=-----");
		boolean fault = false;
		for (StackTraceElement s : t.getStackTrace()) { //filter out the stack trace for only Genesis
			if (s.getClassName().startsWith("genesis")) {
				LogManager.log(Level.SEVERE, s.getClassName() + "." + s.getMethodName() + "() on line " + s.getLineNumber());
				fault = true;
			}
		}
		if (!fault) //if it's not our fault, tell the user
			LogManager.log(Level.SEVERE, "This doesn't look like a problem relating to " + name + ". Check the below remote stack trace.");
		LogManager.log(Level.SEVERE, "-----=[Genesis Stack Trace]=-----");
		LogManager.log(Level.SEVERE, "");
		LogManager.log(Level.SEVERE, "-----=[Remote Stack Trace]=-----");
		fault = false;
		for (StackTraceElement s : t.getStackTrace()) { //filter out the stack trace for only outside Genesis
			if (!s.getClassName().startsWith("genesis")) {
				LogManager.log(Level.SEVERE, s.getClassName() + "." + s.getMethodName() + "() on line " + s.getLineNumber());
				fault = true;
			}
		}
		if (!fault) //if it's not their fault, tell the user
			LogManager.log(Level.SEVERE, "This doesn't look like a problem with anything outside " + name + ". Check the above " + name + " stack trace.");
		LogManager.log(Level.SEVERE, "-----=[Remote Stack Trace]=-----");
		LogManager.log(Level.SEVERE, "");
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
		LogManager.log(Level.INFO, message);
		if (withConsole)
			System.out.println(message);
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

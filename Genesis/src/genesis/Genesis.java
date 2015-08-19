package genesis;

import static genesis.util.GenesisUtil.addPunctuation;
import static genesis.util.GenesisUtil.capitalize;
import static genesis.util.GenesisUtil.format;
import static genesis.util.GenesisUtil.join;
import static genesis.util.GenesisUtil.log;
import static genesis.util.GenesisUtil.removeEndPunctuation;
import static genesis.util.GenesisUtil.reversePerson;
import static genesis.util.GenesisUtil.solve;
import static genesis.util.GenesisUtil.transform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import genesis.logging.LogManager;
import genesis.util.GenesisUtil;
import genesis.util.ResponseType;
import genesis.util.Speech;
import genesis.util.WikipediaFinder;

public final class Genesis {
	
	public static final String name = "Genesis";
	public static final String version = "1.1";
	
	public static void main(String... args) {
		try {
			Genesis g = new Genesis();
			g.start();
			g.stop();
		} catch(IOException e) {
			GenesisUtil.logError(Thread.currentThread(), e);
		}
	}
	
	private final LogManager iomanager;
	private String lastMessage;
	
	public Genesis() throws IOException {
		Thread.setDefaultUncaughtExceptionHandler(GenesisUtil::logError);
		log("Initializing " + toString() + "...");
		log("Generating files...");
		iomanager = new LogManager(this);
		log(toString() + " started on " + System.getProperty("os.name"), true);
	}
	
	public void stop() {
		GenesisUtil.stop(0);
	}
	
	public void start() {
		try(BufferedReader r = new BufferedReader(new InputStreamReader(System.in))) {
			System.out.print("You: ");
			String s = r.readLine();
			String response = respond(s);
			if (response != null) {
				if(response.length() > 0)
					say(response);
				start();
			}
		} catch(Throwable t) {
			if (iomanager != null)
				logError(t, 1);
		}
	}
	
	public String respond(String message) {
		if (message.trim().equals(""))
			return "";
		String response = "";
		if (lastMessage == null) {
			if (message.trim().equalsIgnoreCase("exit"))
				return null;
			boolean newGreeting = true;
			for(String r : iomanager.getResponses(ResponseType.GREETING)) {
				if (transform(message).equalsIgnoreCase(transform(r)))
					newGreeting = false;
			}
			if (newGreeting)
				iomanager.setResponse(ResponseType.GREETING, removeEndPunctuation(message));
			response = (name + ": " + format(iomanager.getResponses(ResponseType.GREETING).get((int) (System.nanoTime() % iomanager.getResponses(ResponseType.GREETING).size()))));
		} else {
			boolean isGreeting = false;
			for(String r : iomanager.getResponses(ResponseType.GREETING)) { //check if THE LAST MESSAGE is another greeting
				if (transform(lastMessage).equalsIgnoreCase(transform(r)))
					isGreeting = true;
			}
			boolean isFarewell = false;
			for(String r : iomanager.getResponses(ResponseType.FAREWELL)) {
				if (transform(message).equalsIgnoreCase(transform(r)))
					isFarewell = true;
			}
			if (isFarewell || message.equalsIgnoreCase("exit")) { //giving a farewell & last message isn't a greeting
				List<String> f = iomanager.getResponses(ResponseType.FAREWELL);
				if (message.equalsIgnoreCase("exit") && !isGreeting) {
					boolean newFarewell = true;
					for(String r : f) { //check if it's a new farewell
						if (transform(lastMessage).equalsIgnoreCase(transform(r)))
							newFarewell = false;
					}
					if (newFarewell) //if it's new, store it for another session (or this one) IF AND ONLY IF we are using "exit"
						iomanager.setResponse(ResponseType.FAREWELL, removeEndPunctuation(lastMessage));
				}
				//say bye back
				if (f != null && f.size() > 0) {
					response = (name + ": " + format(f.get((int) (System.nanoTime() % f.size()))));
				}
				return null; //exit the program
			}
		}
		boolean containsLaugh = false;
		for(String r : iomanager.getResponses(ResponseType.LAUGH)) {
			if (message.matches(".*?\\b" + r + "\\b.*?"))
				containsLaugh = true;
		}
		boolean laughIfPossible = false;
		int laughCounter = 0;
		for(char c : message.toCharArray()) {
			if (c == 'h' || c == 'l') //measure the h's in l's in a message to determine a laugh (e.g. lolol or haha)
				laughCounter++;
		}
		if (laughCounter >= message.toCharArray().length / 2 && !iomanager.getResponses(ResponseType.GREETING).stream().anyMatch((g) -> {
			return transform(g).equalsIgnoreCase(transform(message));
		})) {
			boolean newLaugh = true;
			for(String r : iomanager.getResponses(ResponseType.LAUGH)) {
				if (transform(message).equalsIgnoreCase(transform(r)))
					newLaugh = false;
			}
			if (newLaugh)
				iomanager.setResponse(ResponseType.LAUGH, removeEndPunctuation(message));
			laughIfPossible = true;
		}
		if (!containsLaugh) {
			String[] set = message.split("(?i)(\\s+is\\s+|'s\\s+)");
			try { //if it's math, solve it
				response = (name + ": " + solve(transform(set[1]).trim()));
			} catch(Throwable t) { //it's not math
				String rawKey = transform(set[0]);
				if (rawKey.toLowerCase().contains("what") || rawKey.toLowerCase().contains("who")) {
					String key = transform(reversePerson(set[1]));
					for(String values : iomanager.getResponses(ResponseType.VALUE)) {
						if (transform(values.split("=")[0]).trim().equalsIgnoreCase(key))
							response = name + ": " + capitalize(key) + " is " + values.split("=")[1].trim() + addPunctuation();
					}
					if(response.equals("")) {
						//if we dont have a registered value for this, check with wikipedia and record it
						String summary = WikipediaFinder.getSummary(key);
						if (summary != null) {
							boolean flag = true;
							response = (name + ": " + summary);
							String wikiKey = null, wikiValue = null;
							try {
								if (summary.contains(" is ")) { //could be singular or plural
									wikiKey = reversePerson(summary.split("(?i)\\s+is\\s+")[0].trim());
									wikiValue = join(summary.split("(?i)\\s+is\\s+"), "$1%s", 1).trim();
								} else {
									wikiKey = reversePerson(summary.split("(?i)\\s+are\\s+")[0].trim());
									wikiValue = join(summary.split("(?i)\\s+are\\s+"), "$1%s", 1).trim();
								}
							} catch(IndexOutOfBoundsException e) {
								summary = WikipediaFinder.getSummary(key, true);
								if(summary != null) {
									try {
										if (summary.contains(" is ")) { //could be singular or plural
											wikiKey = reversePerson(summary.split("(?i)\\s+is\\s+")[0].trim());
											wikiValue = join(summary.split("(?i)\\s+is\\s+"), "$1%s", 1).trim();
										} else {
											wikiKey = reversePerson(summary.split("(?i)\\s+are\\s+")[0].trim());
											wikiValue = join(summary.split("(?i)\\s+are\\s+"), "$1%s", 1).trim();
										}
									} catch(IndexOutOfBoundsException e1) {
										flag = false;
									}
								} else
									flag = false;
							}
							if (flag) {
								iomanager.setResponse(ResponseType.VALUE, wikiKey.trim() + "=" + reversePerson(removeEndPunctuation(wikiValue)).trim());
								response = name + ": " + capitalize(wikiKey.replaceAll("\\s+", " ")) + " is " + reversePerson(removeEndPunctuation(wikiValue)).trim() + addPunctuation();
							}
						}
					}
				} else if (message.toLowerCase().contains(" is ")) {
					String key = reversePerson(message.split("(?i)\\s+is\\s+")[0].trim());
					String value = join(message.split("(?i)\\s+is\\s+"), "$1%s", 1).trim();
					iomanager.setResponse(ResponseType.VALUE, key + "=" + reversePerson(removeEndPunctuation(value)));
					response = (name + ": " + capitalize(key) + " is " + removeEndPunctuation(value) + addPunctuation());
				}
			}
		}
		if (response.trim().equals("") && (laughIfPossible || containsLaugh))
			response = (name + ": " + capitalize(iomanager.getResponses(ResponseType.LAUGH).get(((int) (System.nanoTime() % iomanager.getResponses(ResponseType.LAUGH).size())))));
		iomanager.log("You: " + message);
		iomanager.log(name + ": " + (response.replace(name + ": ", "")));
		lastMessage = message;
		return response;
	}
	
	private void say(String message) {
		System.out.println(message);
		Speech.say(message.replace(name + ": ", "").trim());
	}
	
	public LogManager getIOManager() {
		return iomanager;
	}
	
	public void logError(Throwable t) {
		logError(t, 0);
	}
	
	public void logError(Throwable t, int fatal) {
		GenesisUtil.logError(Thread.currentThread(), t, fatal);
	}
	
	public String toString() {
		return name + " v" + version;
	}
}

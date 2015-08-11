package genesis;

import static genesis.GenesisUtil.addPunctuation;
import static genesis.GenesisUtil.capitalize;
import static genesis.GenesisUtil.format;
import static genesis.GenesisUtil.join;
import static genesis.GenesisUtil.log;
import static genesis.GenesisUtil.removeEndPunctuation;
import static genesis.GenesisUtil.reversePerson;
import static genesis.GenesisUtil.solve;
import static genesis.GenesisUtil.transform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public final class Genesis {
	
	public static final String name = "Genesis";
	public static final String version = "1.0.1";
	
	public static void main(String... args) {
		try {
			new Genesis();
		} catch (IOException e) {
			GenesisUtil.logError(Thread.currentThread(), e);
		}
	}
	
	private final IOManager iomanager;
	private String lastMessage;
	
	public Genesis() throws IOException {
		Thread.setDefaultUncaughtExceptionHandler(GenesisUtil::logError);
		log("Initializing " + toString() + "...");
		log("Generating files...");
		iomanager = new IOManager(this);
		log(toString() + " started on " + System.getProperty("os.name"), true);
		start();
		stop();
	}
	
	public void stop() {
		GenesisUtil.stop(0);
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
			say(response);
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
					say(response);
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
				say(response);
			} catch (Throwable t) { //it's not math
				String rawKey = transform(set[0]);
				if (rawKey.toLowerCase().contains("what")) {
					String key = transform(reversePerson(join(set, "is", 1)));
					for (String values : iomanager.getResponses(ResponseType.VALUE)) {
						if (transform(values.split("=")[0]).trim().equalsIgnoreCase(key))
							response = name + ": " + capitalize(key) + " is " + values.split("=")[1].trim() + addPunctuation();
					}
					if (!response.equals(""))
						say(response);
				} else if (message.toLowerCase().contains(" is ")) {
					String key = reversePerson(message.split(" is ")[0].trim());
					String value = join(message.split("(?i) (is) "), "$1%s", 1).trim();
					iomanager.setResponse(ResponseType.VALUE, key + "=" + reversePerson(removeEndPunctuation(value)));
					response = (name + ": " + capitalize(key) + " is " + removeEndPunctuation(value) + addPunctuation());
					say(response);
				}
			}
		}
		if (response.trim().equals("") && (laughIfPossible || containsLaugh)) {
			response = (name + ": " + capitalize(iomanager.getResponses(ResponseType.LAUGH).get(((int) (System.nanoTime() % iomanager.getResponses(ResponseType.LAUGH).size())))));
			say(response);
		}
		iomanager.log("You: " + message);
		iomanager.log(name + ": " + (response.replace(name + ": ", "")));
		lastMessage = message;
		return true;
	}
	
	private void say(String message) {
		System.out.println(message);
		Speech.say(message.replace(name + ": ", "").trim());
	}
	
	public IOManager getIOManager() {
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

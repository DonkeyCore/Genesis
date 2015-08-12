package genesis.util;

public class Speech {
	
	public static boolean say(String message) {
		try {
			Process p = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", "espeak -s 150 -v mb-en1 \"" + message.replace("Eli", "e-lie") + "\"",});
			return p.waitFor() == 0;
		} catch (Throwable t) {
			return false;
		}
	}
	
}

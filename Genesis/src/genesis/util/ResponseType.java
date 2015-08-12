package genesis.util;

public enum ResponseType {
	
	GREETING("Greeting"), FAREWELL("Farewell"), VALUE("Value"), LAUGH("Laugh");
	
	private final String string;
	
	private ResponseType(String string) {
		this.string = string;
	}
	
	public String toString() {
		return string;
	}
	
	public static ResponseType getResponseType(String name) {
		for (ResponseType r : values()) {
			if (r.string.equalsIgnoreCase(name))
				return r;
		}
		return null;
	}
	
}
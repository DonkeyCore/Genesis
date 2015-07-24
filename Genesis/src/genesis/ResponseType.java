package genesis;

public enum ResponseType {
	
	GREETING, FAREWELL, VALUE, LAUGH;
	
	public String toString() {
		return name();
	}
	
	public static ResponseType getResponseType(String name) {
		for (ResponseType r : values()) {
			if(r.name().equalsIgnoreCase(name))
				return r;
		}
		return null;
	}
	
}
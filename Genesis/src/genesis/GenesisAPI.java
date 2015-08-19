package genesis;

import java.io.IOException;

public class GenesisAPI {
	
	private final Genesis g;
	private boolean stopped = false;
	
	public GenesisAPI() throws IOException {
		this.g = new Genesis();
	}
	
	public Genesis getGenesis() {
		if(stopped)
			throw new IllegalStateException("Cannot get Genesis when stopped");
		return g;
	}
	
	public String respond(String message) {
		if(stopped)
			throw new IllegalStateException("Cannot respond when stopped");
		return g.respond(message);
	}
	
	public void stop() {
		if(stopped)
			throw new IllegalStateException("Cannot stop when stopped");
		stopped = true;
		g.stop();
	}
	
}

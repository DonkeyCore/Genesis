package genesis.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public final class GenesisLogFormatter extends Formatter {
	
	
	@Override
	public String format(LogRecord record) {
		StringBuilder sb = new StringBuilder();
		
		sb.append(new Date(record.getMillis())).append(" ");
		sb.append(record.getLevel().getLocalizedName()).append(": ");
		sb.append(formatMessage(record)).append(System.getProperty("line.separator"));
		
		if (record.getThrown() != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			record.getThrown().printStackTrace(pw);
			pw.close();
			sb.append(sw.toString());
		}
		
		return sb.toString();
	}
}
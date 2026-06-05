package com.examportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ResumeParserApplication {

	public static void main(String[] args) {
		try {
			SpringApplication.run(ResumeParserApplication.class, args);
		} catch (Throwable t) {
			StringBuilder sb = new StringBuilder();
			sb.append("\n=================================================================\n");
			sb.append("=== STARTUP EXCEPTION CAUSE CHAIN WITH SUPPRESSED ===\n");
			Throwable cause = t;
			int depth = 0;
			while (cause != null && depth < 10) {
				String msg = cause.getMessage();
				if (msg != null && msg.length() > 100) {
					msg = msg.substring(0, 100) + "...";
				}
				sb.append("[").append(depth).append("] ").append(cause.getClass().getSimpleName()).append(": ").append(msg).append("\n");
				cause = cause.getCause();
				depth++;
			}
			sb.append("\n=== SYSTEM ENV VARIABLES (LENGTHS) ===\n");
			System.getenv().forEach((k, v) -> {
				if (k.contains("KEY") || k.contains("SECRET") || k.contains("PASSWORD") || k.contains("CREDENTIALS")) {
					sb.append(k).append(" = [PRESENT, Length: ").append(v.length()).append("]\n");
				} else {
					sb.append(k).append(" = ").append(v).append("\n");
				}
			});
			sb.append("=================================================================\n");
			System.err.println(sb.toString());
			throw t;
		}
	}
}

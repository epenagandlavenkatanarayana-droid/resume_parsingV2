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
			sb.append("=== STARTUP EXCEPTION CAUSE CHAIN ===\n");
			Throwable cause = t;
			int depth = 0;
			while (cause != null && depth < 10) {
				sb.append("[").append(depth).append("] ").append(cause.getClass().getName()).append(": ").append(cause.getMessage()).append("\n");
				if (cause.getStackTrace() != null && cause.getStackTrace().length > 0) {
					sb.append("   at ").append(cause.getStackTrace()[0].toString()).append("\n");
				}
				cause = cause.getCause();
				depth++;
			}
			sb.append("=================================================================\n");
			System.err.println(sb.toString());
			throw t;
		}
	}
}

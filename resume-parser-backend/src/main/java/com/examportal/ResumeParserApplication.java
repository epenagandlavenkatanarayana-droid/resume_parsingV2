package com.examportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ResumeParserApplication {

	public static void main(String[] args) {
		try {
			SpringApplication.run(ResumeParserApplication.class, args);
		} catch (Throwable t) {
			System.err.println("=== STARTUP EXCEPTION DETECTED ===");
			t.printStackTrace();
			Throwable cause = t;
			int depth = 0;
			while (cause != null && depth < 20) {
				System.err.println("--- Depth " + depth + " cause: " + cause.getClass().getName() + " ---");
				System.err.println(cause.getMessage());
				cause = cause.getCause();
				depth++;
			}
			System.err.println("=== END OF STARTUP EXCEPTION DETAILS ===");
			throw t;
		}
	}
}

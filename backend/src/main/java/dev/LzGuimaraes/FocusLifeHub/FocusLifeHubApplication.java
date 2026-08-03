package dev.LzGuimaraes.FocusLifeHub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class FocusLifeHubApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Cuiaba"));
		SpringApplication.run(FocusLifeHubApplication.class, args);
		System.out.println("FocusLifeHub is running!");
	}
}

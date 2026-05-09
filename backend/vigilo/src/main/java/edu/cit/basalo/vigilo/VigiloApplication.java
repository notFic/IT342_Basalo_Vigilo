package edu.cit.basalo.vigilo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VigiloApplication {

	public static void main(String[] args) {
		SpringApplication.run(VigiloApplication.class, args);
	}

}

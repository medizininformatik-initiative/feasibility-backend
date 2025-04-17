package de.numcodex.feasibility_gui_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FeasibilityGuiBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(FeasibilityGuiBackendApplication.class, args);
  }

}

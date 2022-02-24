package de.numcodex.feasibility_gui_backend.query.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryValidatorSpringConfig {

    @Bean
    public QueryValidator createQueryValidator(ObjectMapper objectMapper) {
        return new QueryValidator(objectMapper);
    }
}

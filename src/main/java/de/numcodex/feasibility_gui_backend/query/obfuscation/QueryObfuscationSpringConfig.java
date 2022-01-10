package de.numcodex.feasibility_gui_backend.query.obfuscation;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Configuration
public class QueryObfuscationSpringConfig {

    @Bean
    public QueryResultObfuscator createQueryResultObfuscator(@Qualifier("obfuscation") MessageDigest hashFn) {
        return new QueryResultObfuscator(hashFn);
    }

    @Qualifier("obfuscation")
    @Bean
    public MessageDigest createObfuscationHashFn() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA3-256");
    }
}

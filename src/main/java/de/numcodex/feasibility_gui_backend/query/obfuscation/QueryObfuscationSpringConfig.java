package de.numcodex.feasibility_gui_backend.query.obfuscation;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryObfuscationSpringConfig {

    @Bean
    public QueryResultObfuscator createQueryResultObfuscator(@Qualifier("obfuscation") HashFunction hashFn) {
        return new QueryResultObfuscator(hashFn);
    }

    @Qualifier("obfuscation")
    @Bean
    public HashFunction createObfuscationHashFn() {
        return Hashing.sha256();
    }
}

package com.ibfd.schematron.config;

import net.sf.saxon.s9api.Processor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SaxonConfig {

    @Bean
    public Processor saxonProcessor() {
        // false = Saxon-HE (Home Edition); no licence file required
        return new Processor(false);
    }
}

package com.agile.common.config;

import com.agile.common.dictionary.DictionaryManager;
import com.agile.common.mvc.model.dao.Dao;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 佟盟 on 2017/10/7
 */
@Configuration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class DaoAutoConfiguration {

    @Bean
    @ConfigurationProperties("agile.jpa")
    @ConditionalOnProperty(prefix = "agile.jpa", name = "enable")
    @ConditionalOnMissingBean
    public Dao dao() {
        return new Dao();
    }

    @Bean
    @ConditionalOnMissingBean(type = {"com.agile.common.dictionary.DictionaryManager"})
    public DictionaryManager defaultDictionaryManager() {
        return o -> {

        };
    }
}

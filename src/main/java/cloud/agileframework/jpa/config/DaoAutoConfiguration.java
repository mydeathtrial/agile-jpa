package cloud.agileframework.jpa.config;

import cloud.agileframework.jpa.dao.Dao;
import cloud.agileframework.jpa.dictionary.DataExtendManager;
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
@ConfigurationProperties("agile.jpa")
@ConditionalOnProperty(prefix = "agile.jpa", name = "enable")
@AutoConfigureAfter({DataSourceAutoConfiguration.class, DictionaryAutoConfiguration.class})
public class DaoAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Dao dao() {
        return new Dao();
    }

    @Bean
    @ConditionalOnMissingBean(DataExtendManager.class)
    public DataExtendManager defaultDictionaryManager() {
        return o -> {

        };
    }
}

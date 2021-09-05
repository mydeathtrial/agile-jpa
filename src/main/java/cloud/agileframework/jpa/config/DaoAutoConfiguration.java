package cloud.agileframework.jpa.config;

import cloud.agileframework.jpa.dao.Dao;
import cloud.agileframework.jpa.dictionary.DataExtendManager;
import cloud.agileframework.spring.util.BeanUtil;
import com.alibaba.druid.DbType;
import org.hibernate.SessionFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManager;
import java.util.Locale;

/**
 * @author 佟盟 on 2017/10/7
 */
@Configuration
@ConfigurationProperties("agile.jpa")
@ConditionalOnProperty(prefix = "agile.jpa", name = "enable", matchIfMissing = true)
@AutoConfigureAfter({DataSourceAutoConfiguration.class, DictionaryAutoConfiguration.class})
public class DaoAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public Dao dao(JpaProperties jpaProperties) {
		String dbType = jpaProperties.getDatabase().name();
		Dao dao = new Dao(DbType.of(dbType.toLowerCase(Locale.ROOT)));
		dao.setSessionFactory(BeanUtil.getApplicationContext()
				.getBean(EntityManager.class)
				.getEntityManagerFactory()
				.unwrap(SessionFactory.class));
		return dao;
	}

	@Bean
	@ConditionalOnMissingBean(DataExtendManager.class)
	public DataExtendManager defaultDictionaryManager() {
		return o -> {

		};
	}
}

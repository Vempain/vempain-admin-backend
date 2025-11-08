package fi.poltsi.vempain.site.configuration;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(entityManagerFactoryRef = "siteEntityManagerFactory",
					   transactionManagerRef = "siteTransactionManager",
					   basePackages = "fi.poltsi.vempain.site.repository")
public class SiteDatabaseConfiguration {
	@Bean
	@Primary
	@ConfigurationProperties("spring.site-datasource")
	public DataSourceProperties siteDataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean(name = "siteDataSource")
	@ConfigurationProperties(prefix = "spring.site-datasource.configuration")
	public DataSource siteDataSource() {
		return siteDataSourceProperties().initializeDataSourceBuilder()
										 .type(HikariDataSource.class)
										 .build();
	}

	@Bean(name = "siteEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean siteEntityManagerFactory(EntityManagerFactoryBuilder builder,
																		   @Qualifier("siteDataSource") DataSource dataSource) {
		return builder
				.dataSource(dataSource)
				.packages("fi.poltsi.vempain.site.entity")
				.persistenceUnit("site")
				.build();
	}

	@Bean(name = "siteTransactionManager")
	public PlatformTransactionManager siteTransactionManager(@Qualifier("siteEntityManagerFactory") EntityManagerFactory siteEntityManagerFactory) {
		return new JpaTransactionManager(siteEntityManagerFactory);
	}
}

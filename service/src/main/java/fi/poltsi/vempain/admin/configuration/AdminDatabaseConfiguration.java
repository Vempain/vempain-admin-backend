package fi.poltsi.vempain.admin.configuration;

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
@EnableJpaRepositories(entityManagerFactoryRef = "adminEntityManagerFactory",
					   transactionManagerRef = "adminTransactionManager",
					   basePackages = {"fi.poltsi.vempain.admin.repository", "fi.poltsi.vempain.auth.repository"})
public class AdminDatabaseConfiguration {
	@Primary
	@Bean
	@ConfigurationProperties("spring.admin-datasource")
	public DataSourceProperties adminDataSourceProperties() {
		return new DataSourceProperties();
	}

	@Primary
	@Bean(name = "adminDataSource")
	@ConfigurationProperties(prefix = "spring.admin-datasource.configuration")
	public DataSource adminDataSource() {
		return adminDataSourceProperties().initializeDataSourceBuilder()
										  .type(HikariDataSource.class)
										  .build();
	}

	@Primary
	@Bean(name = "adminEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean adminEntityManagerFactory(EntityManagerFactoryBuilder builder,
																			@Qualifier("adminDataSource") DataSource dataSource) {
		return builder
				.dataSource(dataSource)
				.packages("fi.poltsi.vempain.admin.entity", "fi.poltsi.vempain.auth.entity")
				.persistenceUnit("admin")
				.build();
	}

	@Primary
	@Bean(name = "adminTransactionManager")
	public PlatformTransactionManager adminTransactionManager(@Qualifier("adminEntityManagerFactory") EntityManagerFactory adminEntityManagerFactory) {
		return new JpaTransactionManager(adminEntityManagerFactory);
	}
}

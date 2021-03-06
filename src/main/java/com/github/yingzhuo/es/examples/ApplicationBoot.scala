/*
*  .        _           _   _                              _                                          _           __ _ _
* /\\   ___| | __ _ ___| |_(_) ___ ___  ___  __ _ _ __ ___| |__         _____  ____ _ _ __ ___  _ __ | | ___  ___ \ \ \ \
*( ( ) / _ \ |/ _` / __| __| |/ __/ __|/ _ \/ _` | '__/ __| '_ \ _____ / _ \ \/ / _` | '_ ` _ \| '_ \| |/ _ \/ __| \ \ \ \
* \\/ |  __/ | (_| \__ \ |_| | (__\__ \  __/ (_| | | | (__| | | |_____|  __/>  < (_| | | | | | | |_) | |  __/\__ \  ) ) ) )
*  '   \___|_|\__,_|___/\__|_|\___|___/\___|\__,_|_|  \___|_| |_|      \___/_/\_\__,_|_| |_| |_| .__/|_|\___||___/ / / / /
* =============================================================================================|_|=============== /_/_/_/
*/
package com.github.yingzhuo.es.examples

import java.util
import javax.persistence.EntityManagerFactory

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.github.yingzhuo.es.examples.model.auditing.AuditorProvider
import com.github.yingzhuo.es.examples.security.{AuthenticationInterceptor, AuthorizationInterceptor}
import com.github.yingzhuo.es.examples.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.{CachingConfigurerSupport, EnableCaching}
import org.springframework.cache.ehcache.{EhCacheCacheManager, EhCacheManagerFactoryBean}
import org.springframework.cache.interceptor.{KeyGenerator, SimpleKeyGenerator}
import org.springframework.cache.support.CompositeCacheManager
import org.springframework.context.annotation.{Bean, ComponentScan, Configuration}
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.AuditorAware
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories
import org.springframework.data.jpa.repository.config.{EnableJpaAuditing, EnableJpaRepositories}
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.web.servlet.config.annotation.{InterceptorRegistry, PathMatchConfigurer, WebMvcConfigurerAdapter}
import org.springframework.web.util.UrlPathHelper

object ApplicationBoot extends App {

    SpringApplication.run(classOf[ApplicationBoot], args: _*)

    @SpringBootApplication
    class ApplicationBoot

    @Configuration
    class ApplicationBootConfigBean {

        @Bean
        def applicationContextHolder(): AnyRef = ApplicationContextHolder()

        @Bean
        def idGenerator(): IdGenerator[String] = DefaultStringIdGenerator

        @Bean
        def passwordHasher(): PasswordHasher = DefaultPasswordHasher

    }

    @Configuration
    class ApplicationBootConfigJackson {

        @Autowired(required = false)
        def configObjectMapper(om: ObjectMapper): Unit = Option(om).foreach(_.registerModule(DefaultScalaModule))

    }

    @Configuration
    class ApplicationBootConfigMvc extends WebMvcConfigurerAdapter {

        override def addInterceptors(registry: InterceptorRegistry): Unit = {
            registry.addInterceptor(DebugLoggingInterceptor).addPathPatterns("/**")
        }

        override def configurePathMatch(configurer: PathMatchConfigurer): Unit = {
            val helper = Option(configurer.getUrlPathHelper) match {
                case Some(x) => x
                case None => new UrlPathHelper()
            }

            helper.setDefaultEncoding("UTF-8")
            helper.setRemoveSemicolonContent(false)
            configurer.setUrlPathHelper(helper)
        }
    }

    @Configuration
    @EnableJpaAuditing()
    @EnableJpaRepositories(Array("com.github.yingzhuo.es.examples"))
    @EnableTransactionManagement
    @ComponentScan(Array("com.github.yingzhuo.es.examples"))
    class ApplicationBootConfigJpa {

        @Bean
        def transactionManager(entityManagerFactory: EntityManagerFactory): PlatformTransactionManager = {
            new JpaTransactionManager(entityManagerFactory)
        }

        @Bean
        def auditorAware(): AuditorAware[String] = AuditorProvider
    }

    @Configuration
    @EnableElasticsearchRepositories(basePackages = Array("com.github.yingzhuo.es.examples"))
    class ApplicationBootConfigElasticsearch

    @Configuration
    @EnableCaching
    class ApplicationBootConfigCache extends CachingConfigurerSupport {

        @Bean
        def ehCacheManagerFactoryBean: EhCacheManagerFactoryBean = {
            val cacheManagerFactoryBean = new EhCacheManagerFactoryBean
            cacheManagerFactoryBean.setConfigLocation(new ClassPathResource("META-INF/ehcache.xml"))
            cacheManagerFactoryBean.setShared(true)
            cacheManagerFactoryBean
        }

        @Bean
        def ehCacheCacheManager = new EhCacheCacheManager(ehCacheManagerFactoryBean.getObject)

        override def keyGenerator: KeyGenerator = new SimpleKeyGenerator

        override def cacheManager: CacheManager = {
            val cacheManagers: util.List[CacheManager] = new util.LinkedList[CacheManager]
            if (ehCacheCacheManager != null) cacheManagers.add(ehCacheCacheManager)
            val cacheManager: CompositeCacheManager = new CompositeCacheManager
            cacheManager.setCacheManagers(cacheManagers)
            cacheManager.setFallbackToNoOpCache(false)
            cacheManager
        }
    }

    @Configuration
    class ApplicationBootConfigSecurity @Autowired()(val userService: UserService, val passwordHasher: PasswordHasher) extends WebMvcConfigurerAdapter {
        override def addInterceptors(registry: InterceptorRegistry): Unit = {
            registry.addInterceptor(new AuthenticationInterceptor(userService, passwordHasher)).addPathPatterns("/**")
            registry.addInterceptor(new AuthorizationInterceptor).addPathPatterns("/**")
        }
    }

}

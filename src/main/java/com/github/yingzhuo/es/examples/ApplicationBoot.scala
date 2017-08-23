package com.github.yingzhuo.es.examples

import java.util.UUID
import javax.persistence.EntityManagerFactory

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.github.yingzhuo.es.examples.tool.IdGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.{Bean, ComponentScan, Configuration}
import org.springframework.data.jpa.repository.config.{EnableJpaAuditing, EnableJpaRepositories}
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.web.servlet.config.annotation.{PathMatchConfigurer, WebMvcConfigurerAdapter}
import org.springframework.web.util.UrlPathHelper

object ApplicationBoot extends App {

    SpringApplication.run(classOf[ApplicationBoot], args: _*)

    @SpringBootApplication
    class ApplicationBoot

    @Configuration
    class ApplicationBootConfigBean {

        @Bean
        def idGenerator(): IdGenerator[String] = new IdGenerator[String] {
            override def generate: String = UUID.randomUUID().toString.replaceAll("-", "")
        }

    }

    @Configuration
    class ApplicationBootConfigJackson {

        @Autowired(required = false)
        def configObjectMapper(om: ObjectMapper): Unit = Option(om).foreach(_.registerModule(DefaultScalaModule))

    }

    @Configuration
    class ApplicationBootConfigMvc extends WebMvcConfigurerAdapter {
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
    @EnableJpaAuditing
    @EnableJpaRepositories(Array("com.github.yingzhuo.es.examples"))
    @EnableTransactionManagement
    @ComponentScan(Array("com.github.yingzhuo.es.examples"))
    class ApplicationBootConfigJpa {

        @Bean
        def transactionManager(entityManagerFactory: EntityManagerFactory): PlatformTransactionManager = {
            new JpaTransactionManager(entityManagerFactory)
        }
    }

}

package com.example.demo;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.spring.VelocityEngineFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThymeleafConfig {
  public static void main(String[] args) {
//    VelocityEngine ve = new VelocityEngine();
//    ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
//    ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
//    ve.init();
//    Template t = ve.getTemplate("templates/email.vm");
//    VelocityContext vc = new VelocityContext();
//    vc.put("helloWorld", "Hello World!!!");
//    StringWriter sw = new StringWriter();
//    t.merge(vc, sw);
//    System.out.println(sw);
  }
  @Bean
  public VelocityEngine velocityEngine() throws VelocityException, IOException {
    VelocityEngineFactoryBean factory = new VelocityEngineFactoryBean();
    Properties props = new Properties();
    props.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
    props.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());

    factory.setVelocityProperties(props);

    return factory.createVelocityEngine();
  }
}

package com.namnv.learner;

import com.google.inject.Guice;
import com.namnv.core.ClusterBootstrap;
import com.namnv.learner.config.ApplicationConfig;
import com.namnv.learner.module.RepoModule;
import com.namnv.learner.module.LearnerAppModule;
import org.yaml.snakeyaml.Yaml;

public class LearnerApplication {

  public static void main(String[] args) {
    var yaml = new Yaml();
    var inputStream = LearnerAppModule.class.getClassLoader().getResourceAsStream("config.yml");
    var applicationConfig = yaml.loadAs(inputStream, ApplicationConfig.class);
    var rootInjector =
        Guice.createInjector(
            new LearnerAppModule(applicationConfig), new RepoModule(applicationConfig));
    var stateMachineManager = rootInjector.getInstance(ClusterBootstrap.class);
    stateMachineManager.onStart();
  }
}

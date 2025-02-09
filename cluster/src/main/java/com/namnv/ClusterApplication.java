package com.namnv;

import com.google.inject.Guice;
import com.namnv.config.ApplicationConfig;
import com.namnv.core.ClusterBootstrap;
import com.namnv.module.ClusterAppModule;
import com.namnv.module.RepoModule;
import org.yaml.snakeyaml.Yaml;

import static com.namnv.util.ConfigUtils.getClusterNode;

public class ClusterApplication {

  public static void main(String[] args) {
    final int nodeID = args.length > 0 ? Integer.parseInt(args[0]) : getClusterNode();
    final int maxNodes = args.length > 0 ? Integer.parseInt(args[1]) : 1;

    var yaml = new Yaml();
    var inputStream = ClusterApplication.class.getClassLoader().getResourceAsStream("config.yml");
    var applicationConfig = yaml.loadAs(inputStream, ApplicationConfig.class);
    applicationConfig.setNodeID(nodeID);
    applicationConfig.setMaxNodes(maxNodes);

    var rootInjector =
        Guice.createInjector(
            new ClusterAppModule(applicationConfig), new RepoModule(applicationConfig));
    var stateMachineManager = rootInjector.getInstance(ClusterBootstrap.class);
    stateMachineManager.onStart();
  }
}

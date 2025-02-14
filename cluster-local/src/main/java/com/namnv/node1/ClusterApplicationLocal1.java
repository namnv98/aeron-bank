package com.namnv.node1;

import static com.namnv.util.ConfigUtils.getClusterNode;

import com.google.inject.Guice;
import com.namnv.config.ApplicationConfig;
import com.namnv.core.ClusterBootstrap;
import com.namnv.module.ClusterAppModuleLocal;
import com.namnv.module.RepoModuleLocal;
import org.yaml.snakeyaml.Yaml;

public class ClusterApplicationLocal1 {

  public static void main(String[] args) {
    final int nodeID = args.length > 0 ? Integer.parseInt(args[0]) : getClusterNode();
    final int maxNodes = args.length > 0 ? Integer.parseInt(args[1]) : 1;

    var yaml = new Yaml();
    var inputStream = ClusterApplicationLocal1.class.getClassLoader().getResourceAsStream("config.yml");
    var applicationConfig = yaml.loadAs(inputStream, ApplicationConfig.class);
    applicationConfig.setNodeID(1);
    applicationConfig.setMaxNodes(2);

    var rootInjector =
        Guice.createInjector(
            new ClusterAppModuleLocal(applicationConfig), new RepoModuleLocal(applicationConfig));
    var stateMachineManager = rootInjector.getInstance(ClusterBootstrap.class);
    stateMachineManager.onStart();
  }
}

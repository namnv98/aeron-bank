package com.namnv.node;

import com.google.inject.Guice;
import com.namnv.config.ApplicationConfig;
import com.namnv.core.ClusterBootstrap;
import com.namnv.module.ClusterAppModuleLocal;
import com.namnv.module.RepoModuleLocal;
import org.yaml.snakeyaml.Yaml;

import static com.namnv.util.ConfigUtils.getClusterNode;

public class ClusterApplicationLocal {

  public static void main(String[] args) {
    final int nodeID = args.length > 0 ? Integer.parseInt(args[0]) : getClusterNode();
    final int maxNodes = args.length > 0 ? Integer.parseInt(args[1]) : 1;

    var yaml = new Yaml();
    var inputStream = ClusterApplicationLocal.class.getClassLoader().getResourceAsStream("config.yml");
    var applicationConfig = yaml.loadAs(inputStream, ApplicationConfig.class);
    applicationConfig.setNodeID(0);
    applicationConfig.setMaxNodes(1);

    var rootInjector =
        Guice.createInjector(
            new ClusterAppModuleLocal(applicationConfig), new RepoModuleLocal(applicationConfig));
    var stateMachineManager = rootInjector.getInstance(ClusterBootstrap.class);
    stateMachineManager.onStart();
  }
}

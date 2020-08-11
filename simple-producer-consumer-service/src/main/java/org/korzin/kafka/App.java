package org.korzin.kafka;

import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.netflix.governator.configuration.PropertiesConfigurationProvider;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class App {
  public static final AtomicReference<Injector> injector = new AtomicReference<>();

  private static final String PROP_PREFIX = "ProdConsExample.";
  private static final int DEFAULT_CONTROL_PORT = 8085;

  public static void main(String[] args) throws IOException {

    try {

      Options options = createOptions();
      CommandLine line = new DefaultParser().parse(options, args);
      Properties properties = new Properties();

      if (line.hasOption('p')) {
        properties.load(new FileInputStream(line.getOptionValue('p')));
      }

      for (Option opt : line.getOptions()) {
        String name = opt.getOpt();
        String value = line.getOptionValue(name);
        String propName = PROP_PREFIX + opt.getArgName();
        properties.setProperty(propName, value);
      }

      List<Module> extensionModules = null;

      if (extensionModules == null) {
        // catch-all for either no configuration or empty configuration file
        extensionModules = Lists.newArrayList();
      }

      create(injector, properties, extensionModules.toArray(new Module[extensionModules.size()]));

      injector.get().getInstance(LifecycleManager.class).start();

      // shutdown hook :
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    try {
                      System.out.println(">>> shutdown hook.");
                      Closeables.close(injector.get().getInstance(LifecycleManager.class), true);
                      System.out.println("<<< shutdown hook.");
                    } catch (IOException e) {
                      // do nothing because Closeables.close will swallow IOException
                    }
                  }));

      injector.get().getInstance(ETLRunner.class).doWork();
    } catch (Throwable e) {
      e.printStackTrace();
      System.err.println("ProdConsExample startup failed: " + e.getMessage());
      System.exit(-1);
    } finally {
      Closeables.close(injector.get().getInstance(LifecycleManager.class), true);
    }
  }

  public static void create(
      AtomicReference<Injector> injector, final Properties properties, Module... modules)
      throws Exception {
    injector.set(
        LifecycleInjector.builder()
            .withBootstrapModule(
                binder ->
                    binder
                        .bindConfigurationProvider()
                        .toInstance(new PropertiesConfigurationProvider(properties)))
            .withModules(buildMainModule() /*StatusServer.createJerseyServletModule()*/)
            .withAdditionalModules(modules)
            .build()
            .createInjector());
  }

  private static Module buildMainModule() {
    return new AbstractModule() {
      @Override
      public void configure() {}
    };
  }

  private static void run(/*int port*/ ) throws IOException {
    // start web server, e.g. new Server???Control().start(port);
    injector.get().getInstance(ETLRunner.class).doWork();
  }

  private static int getControlPort(Options options) {
    Option opt = options.getOption("c");
    String value = opt.getValue();
    if (value == null) {
      return DEFAULT_CONTROL_PORT;
    }

    return Integer.parseInt(value);
  }

  @SuppressWarnings("static-access")
  private static Options createOptions() {

    Option propertyFile =
        Option.builder("p")
            .longOpt("appPropsPath")
            .argName("appPropsPath")
            .desc("Server property file path")
            .hasArg(true)
            .build();

    Options options = new Options();
    options.addOption(propertyFile);

    return options;
  }
}

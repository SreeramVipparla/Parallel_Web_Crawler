package com.udacity.webcrawler;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.google.inject.multibindings.Multibinder;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.parser.ParserModule;
import com.udacity.webcrawler.profiler.Profiler;

import javax.inject.Qualifier;
import javax.inject.Singleton;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Guice dependency injection module that installs all the required dependencies to run the web
 * crawler application. Callers should use it like this:
 *
 * <pre>{@code
 *   CrawlerConfiguration config = ...;
 *   WebCrawler crawler =
 *       Guice.createInjector(new WebCrawlerModule(config))
 *           .getInstance(WebCrawler.class);
 * }</pre>
 */
public final class WebCrawlerModule extends AbstractModule {

  private final CrawlerConfiguration config;

  /**
   * Installs a web crawler that conforms to the given {@link CrawlerConfiguration}.
   */
  public WebCrawlerModule(CrawlerConfiguration config) {
    this.config = Objects.requireNonNull(config);
  }

  @Override
  protected void configure() {
    /*
     Multibinder provides a way to implement the strategy pattern through dependency injection.

     This is how crawler is instantiated in WebCrawlerMain.  The exact process of picking
     sequential vs. parallel web crawler is still a complete mystery.
     */
    Multibinder<WebCrawler> multibinder =
        Multibinder.newSetBinder(binder(), WebCrawler.class, Internal.class);
    multibinder.addBinding().to(SequentialWebCrawler.class);
    multibinder.addBinding().to(ParallelWebCrawler.class);

    bind(Clock.class).toInstance(Clock.systemUTC());
    bind(Key.get(Integer.class, MaxDepth.class)).toInstance(config.getMaxDepth());
    bind(Key.get(Integer.class, PopularWordCount.class)).toInstance(config.getPopularWordCount());
    bind(Key.get(Duration.class, Timeout.class)).toInstance(config.getTimeout());
    bind(new Key<List<Pattern>>(IgnoredUrls.class){}).toInstance(config.getIgnoredUrls());


    install(
        new ParserModule.Builder()
            .setTimeout(config.getTimeout())
            .setIgnoredWords(config.getIgnoredWords())
            .build());
  }

  /*
  This provide function may be where the implementation is specified.

  If the configuration JSON includes an ImplementationOverride key
  then it streams through the set of WebCrawler implementations.
  Those implementations are added to a list in the MultiBinder statement
  above.  So when someone asks for a WebCrawler, they get the class with
  a name that matches ImplementationOverride.  Remember, these are a set
  of WebCrawler implementations, not a set of strings.
   */
  @Provides
  @Singleton
  @Internal
  WebCrawler provideRawWebCrawler(
      @Internal Set<WebCrawler> implementations,
      @TargetParallelism int targetParallelism) {
    String override = config.getImplementationOverride();
    if (!override.isEmpty()) {
      return implementations
          .stream()
          .filter(impl -> impl.getClass().getName().equals(override))
          .findFirst()
          .orElseThrow(() -> new ProvisionException("Implementation not found: " + override));
    }

    /*
    If no ImplementationOverride is specified then we assume a parallel
    implementation and ensure the parallelism in config is do-able with
    available processors.  targetParallelism is provided by the function
    below.  It doesn't really seem to work logically.  provideTargetParallelism
    returns any number greater than 0 that exists in the configuration.
    The filter statement below basically compares the config.maxParallelism
    to itself in a roundabout way.
     */
    return implementations
        .stream()
        .filter(impl -> targetParallelism <= impl.getMaxParallelism())
        .findFirst()
        .orElseThrow(
            () -> new ProvisionException(
                "No implementation able to handle parallelism = \"" +
                    config.getParallelism() + "\"."));
  }

  @Provides
  @Singleton
  @TargetParallelism
  int provideTargetParallelism() {
    if (config.getParallelism() >= 0) {
      return config.getParallelism();
    }
    return Runtime.getRuntime().availableProcessors();
  }

  /*
  Because this is Guice and injection, the @Internal annotation can give this
  function a WebCrawler object specified from the configuration.
   */
  @Provides
  @Singleton
  WebCrawler provideWebCrawlerProxy(Profiler wrapper, @Internal WebCrawler delegate) {
    return wrapper.wrap(WebCrawler.class, delegate);
  }

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  private @interface Internal {
  }
}

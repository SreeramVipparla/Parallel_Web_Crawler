package com.udacity.webcrawler.json;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/*
 * A static utility class that loads a JSON configuration file.
 *
 * JsonDeserialize annotation is used to inform Jackson of the
 * class that binds keys to variables.  This is similar to the
 * bind methods in Guice.
 *
 * In
 */
@JsonDeserialize(builder = CrawlerConfiguration.Builder.class)
public final class ConfigurationLoader {

  private final Path path;

  /**
   * Create a {@link ConfigurationLoader} that loads configuration from the given {@link Path}.
   *
   * <p>This is the Path to a JSON file.  Jackson will be used to parse the
   * JSON file to a CrawlerConfiguration object.</p>
   */
  public ConfigurationLoader(Path path) {
    this.path = Objects.requireNonNull(path);
  }

  /**
   * Loads configuration from this {@link ConfigurationLoader}'s path
   *
   * @return the loaded {@link CrawlerConfiguration}.
   */
  public CrawlerConfiguration load() {
    //Use try with resources to ensure closing
    try (Reader reader = Files.newBufferedReader(path)) {
      return read(reader);
  } catch (IOException e) {
      e.printStackTrace();
      return null;
  }
}
  /**
   * Loads crawler configuration from the given reader.
   *
   * @param reader a Reader pointing to a JSON string that contains crawler configuration.
   * @return a crawler configuration
   */
  public static CrawlerConfiguration read(Reader reader) {
    // This is here to get rid of the unused variable warning.
    Objects.requireNonNull(reader);
    //Create a mapper object for reading in the Json Config data
    ObjectMapper mapper = new ObjectMapper();

    //allow calling program to close stream
    mapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

    try {
      return mapper.readValue(reader, CrawlerConfiguration.Builder.class).build();
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }

    //return null to show fail if we get here;
    return null;
  }
}

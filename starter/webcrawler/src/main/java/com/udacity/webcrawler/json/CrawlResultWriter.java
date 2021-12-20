package com.udacity.webcrawler.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.checkerframework.common.returnsreceiver.qual.This;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Utility class to write a {@link CrawlResult} to file.
 */
public final class CrawlResultWriter {
  private final CrawlResult result;

  /**
   * Creates a new {@link CrawlResultWriter} that will write the given {@link CrawlResult}.
   */
  //This is a constructor that puts the result in the class's variable
  public CrawlResultWriter(CrawlResult result) {
    this.result = Objects.requireNonNull(result);
  }

  /**
   * Formats the {@link CrawlResult} as JSON and writes it to the given {@link Path}.
   *
   * <p>If a file already exists at the path, the existing file should not be deleted; new data
   * should be appended to it.
   *
   * @param path the file path where the crawl result data should be written.
   */
  //They abstracted the write method.  They separated the function that writes
  //the Json from the function that prepares the output.  Unfortunately, they
  //named them both write.  I'm not sure how this is a design feature.  I think
  //this is overloading which I know is a thing.
  public void write(Path path) {
    // This is here to get rid of the unused variable warning.
    Objects.requireNonNull(path);
    // TODO: Fill in this method.

    try(Writer writer = Files.newBufferedWriter(path)){
      write(writer);
    }catch (IOException e){
      System.out.println("There was an issue writing the file");
      e.printStackTrace();
    }
  }

  /**
   * Formats the {@link CrawlResult} as JSON and writes it to the given {@link Writer}.
   *
   * @param writer the destination where the crawl result data should be written.
   */
  @JsonDeserialize(builder = CrawlResult.Builder.class)
  public void write(Writer writer) throws IOException{
    // This is here to get rid of the unused variable warning.
    Objects.requireNonNull(writer);
    // TODO: Fill in this method.

    /*
    The ObjectMapper closes the stream with each writeValue.  We have to
    disable this default feature with AUTO_CLOSE_TARGET.
    https://github.com/msgpack/msgpack-java/issues/233
     */
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    objectMapper.writeValue(writer, this.result);
  }
}
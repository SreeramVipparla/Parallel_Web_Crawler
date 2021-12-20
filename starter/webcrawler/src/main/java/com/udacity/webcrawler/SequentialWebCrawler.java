package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A {@link WebCrawler} that downloads and processes one page at a time.
 */
final class SequentialWebCrawler implements WebCrawler {

  private final Clock clock;
  private final PageParserFactory parserFactory;
  private final Duration timeout;
  private final int popularWordCount;
  private final int maxDepth;
  private final List<Pattern> ignoredUrls;

  @Inject
  SequentialWebCrawler(
      Clock clock,
      PageParserFactory parserFactory,
      @Timeout Duration timeout,
      @PopularWordCount int popularWordCount,
      @MaxDepth int maxDepth,
      @IgnoredUrls List<Pattern> ignoredUrls) {
    this.clock = clock;
    this.parserFactory = parserFactory;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.maxDepth = maxDepth;
    this.ignoredUrls = ignoredUrls;
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {

    //Set a timeout
    Instant deadline = clock.instant().plus(timeout);

    //Repositories for word counts and visited urls.  In a parallel
    // implementation these will have to be concurrency-aware.
    Map<String, Integer> counts = new HashMap<>();  //Make this threadable
    Set<String> visitedUrls = new HashSet<>();      //Make this threadable

    //Initiate crawl down each url in list of roots.
    for (String url : startingUrls) {
      crawlInternal(url, deadline, maxDepth, counts, visitedUrls);
    }

    if (counts.isEmpty()) {
      return new CrawlResult.Builder()
          .setWordCounts(counts)
          .setUrlsVisited(visitedUrls.size())
          .build();
    }

    return new CrawlResult.Builder()
        .setWordCounts(WordCounts.sort(counts, popularWordCount))
        .setUrlsVisited(visitedUrls.size())
        .build();
  }

  //Recursive crawl function
  private void crawlInternal(
      String url,
      Instant deadline,
      int maxDepth,
      Map<String, Integer> counts,
      Set<String> visitedUrls) {

    //Check that we haven't timed out
    if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
      return;
    }

    //Skip urls that match the ignoredUrls pattern
    for (Pattern pattern : ignoredUrls) {
      if (pattern.matcher(url).matches()) {
        return;
      }
    }

    /*
    skip urls that have already been visited

    We should lock this Set here so that it is not modified
    by another thread
     */
    if (visitedUrls.contains(url)) {
      return;
    }

    //Add this url to the list of visited
    visitedUrls.add(url);
    //We will unlock visitedUrls here to keep execution running

    //Parse the page
    PageParser.Result result = parserFactory.get(url).parse();

    //Update word counts
    for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
      if (counts.containsKey(e.getKey())) {
        counts.put(e.getKey(), e.getValue() + counts.get(e.getKey()));
      } else {
        counts.put(e.getKey(), e.getValue());
      }
    }

    //Recurse down the tree of links within this url
    for (String link : result.getLinks()) {
      crawlInternal(link, deadline, maxDepth - 1, counts, visitedUrls);
    }
  }
}

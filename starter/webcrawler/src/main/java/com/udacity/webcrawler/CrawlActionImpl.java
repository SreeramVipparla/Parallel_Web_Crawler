package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

/**
 * RecursiveAction class that performs a crawl through a url.
 *
 * <p>Created through an embedded Builder class</p>
 *
 * <p>maxDepth and url are updated at each recursive iteration.
 * maxDepth is decremented, url is the root of further crawls through
 * a web page.</p>
 */
public final class CrawlActionImpl extends RecursiveAction {

    private String url;
    private int maxDepth;
    private final CrawlActionFrame cAF;


    @Inject
    public CrawlActionImpl(String url,
                            int maxDepth,
                            CrawlActionFrame cAF){

        this.url = url;
        this.maxDepth = maxDepth;
        this.cAF = cAF;
    }

    public void setUrl(String url) {
        this.url = Objects.requireNonNull(url);
    }
    public void setMaxDepth(int maxDepth){
        this.maxDepth = Objects.requireNonNull(maxDepth);
    }

    /**
     * Code to be executed in a RecursiveAction invocation
     */
    @Override
    protected void compute() {


        //Check that we haven't timed out
        if (maxDepth == 0 ||
                cAF.getClock().instant().isAfter(cAF.getDeadline())) {
            return;
        }

        //Skip urls that match the ignoredUrls pattern
        for (Pattern pattern : cAF.getIgnoredUrls()) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }

    /*
    skip urls that have already been visited.  Lock this Set in a synchronized
    block until the update is complete
     */
        Set<String> visitedUrls = cAF.getVisitedUrls();
        synchronized (visitedUrls){
            if (visitedUrls.contains(url)) {
                return;
            }

            //Add this url to the list of visited
            visitedUrls.add(url);
            //Unlock visitedUrls here to keep execution running
        }

        //Get results from this URL

        PageParser.Result result = cAF.getParserFactory().get(url).parse();

        //Record the results by updating word counts
        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            Map<String, Integer> counts = cAF.getCounts();
            if (counts.containsKey(e.getKey())) {
                counts.put(e.getKey(), e.getValue() + counts.get(e.getKey()));
            } else {
                counts.put(e.getKey(), e.getValue());
            }
        }

        //Results also included a list of embedded URLs
        // Recurse down the list of links
        for (String link : result.getLinks()) {
             CrawlActionImpl crawlAction =
                    new CrawlActionImpl(link, maxDepth-1, cAF);
            cAF.getPool().invoke(crawlAction);
        }
    }
}

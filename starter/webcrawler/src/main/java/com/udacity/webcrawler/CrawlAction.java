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
import java.util.concurrent.RecursiveTask;
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
public final class CrawlAction extends RecursiveAction {

    private final String url;
    private final Instant deadline;
    private final int maxDepth;
    private final Clock clock;
    private final List<Pattern> ignoredUrls;
    private final PageParserFactory parserFactory;
    private final Map<String, Integer> counts;
    private final Set<String> visitedUrls;
    private final ForkJoinPool pool;


    @Inject //This injection is for parserFactory
    private CrawlAction(String url,
                        Instant deadline,
                        int maxDepth,
                        Clock clock,
                        List<Pattern> ignoredUrls,
                        PageParserFactory parserFactory,
                        Map<String, Integer> counts,
                        Set<String> visitedUrls,
                        ForkJoinPool pool){

        this.url = url;
        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.clock = clock;
        this.ignoredUrls = ignoredUrls;
        this.parserFactory = parserFactory;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.pool = pool;
    }

    public static final class Builder {
        private String url;
        private Instant deadline;
        private int maxDepth;
        private Clock clock;
        private List<Pattern> ignoredUrls;
        private PageParserFactory parserFactory;
        private Map<String, Integer> counts;
        private Set<String> visitedUrls;
        private ForkJoinPool pool;

        public Builder setUrl(String url) {
            this.url = Objects.requireNonNull(url);
            return this;
        }

        public Builder setDeadline(Instant deadline){
            this.deadline = Objects.requireNonNull(deadline);
            return this;
        }

        public Builder setMaxDepth(int maxDepth){
            this.maxDepth = Objects.requireNonNull(maxDepth);
            return this;
        }

        public Builder setClock(Clock clock){
            this.clock = Objects.requireNonNull(clock);
            return this;
        }

        public Builder setIgnoredUrls(List<Pattern> ignoredUrls){
            this.ignoredUrls = Objects.requireNonNull(ignoredUrls);
            return this;
        }

        public Builder setParserFactory(PageParserFactory parserFactory){
            this.parserFactory = parserFactory;
            return this;
        }

        public Builder setCounts(Map<String, Integer> counts){
            this.counts = counts;
            return this;
        }

        public Builder setVisitedUrls(Set<String> visitedUrls){
            this.visitedUrls = visitedUrls;
            return this;
        }

        public Builder setPool(ForkJoinPool pool){
            this.pool = pool;
            return this;
        }

        public CrawlAction build(){
            return new CrawlAction(
                    url,
                    deadline,
                    maxDepth,
                    clock,
                    ignoredUrls,
                    parserFactory,
                    counts,
                    visitedUrls,
                    pool);
        }
    }

    /**
     * Code to be executed in a RecursiveAction invocation
     */
    @Override
    protected void compute() {

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
    Lock this Set in a synchronized block until
    the update is complete
     */
        synchronized (visitedUrls){
            if (visitedUrls.contains(url)) {
                return;
            }

            //Add this url to the list of visited
            visitedUrls.add(url);
            //Unlock visitedUrls here to keep execution running
        }

        //Get results from this URL
        PageParser.Result result = parserFactory.get(url).parse();

        //Record the results by updating word counts
        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            if (counts.containsKey(e.getKey())) {
                counts.put(e.getKey(), e.getValue() + counts.get(e.getKey()));
            } else {
                counts.put(e.getKey(), e.getValue());
            }
        }

        //Results also included a list of embedded URLs
        // Recurse down the list of links
        for (String link : result.getLinks()) {
            CrawlAction crawlAction = new Builder()
                    .setMaxDepth(maxDepth-1)
                            .setUrl(link)
                                    .build();
            pool.invoke(crawlAction);
        }
    }

}

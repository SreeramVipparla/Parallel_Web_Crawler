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
public final class CrawlActionFrame {

    private final Instant deadline;
    private final Clock clock;
    private final List<Pattern> ignoredUrls;
    private final PageParserFactory parserFactory;
    private final Map<String, Integer> counts;
    private final Set<String> visitedUrls;
    private final ForkJoinPool pool;


    @Inject
    private CrawlActionFrame(Instant deadline,
                             Clock clock,
                             List<Pattern> ignoredUrls,
                             PageParserFactory parserFactory,
                             Map<String, Integer> counts,
                             Set<String> visitedUrls,
                             ForkJoinPool pool){

        this.deadline = deadline;
        this.clock = clock;
        this.ignoredUrls = ignoredUrls;
        this.parserFactory = parserFactory;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.pool = pool;
    }

    public static final class Builder {
        private Instant deadline;
        private Clock clock;
        private List<Pattern> ignoredUrls;
        private PageParserFactory parserFactory;
        private Map<String, Integer> counts;
        private Set<String> visitedUrls;
        private ForkJoinPool pool;

        public Builder setDeadline(Instant deadline){
            this.deadline = Objects.requireNonNull(deadline);
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

        public CrawlActionFrame build(){
            return new CrawlActionFrame(
                    deadline,
                    clock,
                    ignoredUrls,
                    parserFactory,
                    counts,
                    visitedUrls,
                    pool);
        }
    }

    public Clock getClock() {
        return clock;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public ForkJoinPool getPool() {
        return pool;
    }

    public PageParserFactory getParserFactory() {
        return parserFactory;
    }

    public List<Pattern> getIgnoredUrls() {
        return ignoredUrls;
    }

    public Set<String> getVisitedUrls() {
        return visitedUrls;
    }

    public Map<String, Integer> getCounts() {
        return counts;
    }
}

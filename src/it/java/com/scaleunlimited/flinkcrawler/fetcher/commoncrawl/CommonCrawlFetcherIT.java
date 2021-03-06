package com.scaleunlimited.flinkcrawler.fetcher.commoncrawl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Test;

import com.scaleunlimited.flinkcrawler.parser.ParserResult;
import com.scaleunlimited.flinkcrawler.parser.SimplePageParser;
import com.scaleunlimited.flinkcrawler.pojos.ExtractedUrl;
import com.scaleunlimited.flinkcrawler.pojos.FetchResultUrl;
import com.scaleunlimited.flinkcrawler.pojos.FetchStatus;
import com.scaleunlimited.flinkcrawler.pojos.ValidUrl;
import com.scaleunlimited.flinkcrawler.urls.SimpleUrlNormalizer;

import crawlercommons.fetcher.FetchedResult;
import crawlercommons.fetcher.http.BaseHttpFetcher;
import crawlercommons.fetcher.http.UserAgent;

public class CommonCrawlFetcherIT {

    private static final String CACHE_DIR = "./target/commoncrawlcache/";

    @Test
    public void testFetchGetMostRecentResult() throws Exception {
        BaseHttpFetcher fetcher = makeFetcher(1);

        // This URL has 5 entries in the 2017-17 index. The most recent
        // one is a 200 status code, with final URL = https://www.cloudera.com/
        FetchedResult result = fetcher.get("http://cloudera.com/");
        assertEquals(200, result.getStatusCode());
        assertEquals("http://cloudera.com/", result.getBaseUrl());
        assertEquals("https://www.cloudera.com/", result.getFetchedUrl());
        
        SimplePageParser parser = new SimplePageParser();
        parser.open(null); // We don't use the RuntimeContext in the parser's open method so we can probably get away with passing in null here.
        ParserResult parseResult = parser.parse(new FetchResultUrl(new ValidUrl(result.getBaseUrl()),
                FetchStatus.FETCHED, result.getFetchTime(), result.getFetchedUrl(), result
                        .getHeaders(), result.getContent(), result.getContentType(), result
                        .getResponseRate()));

        ExtractedUrl[] outlinks = parseResult.getExtractedUrls();
        assertTrue(outlinks.length > 0);
    }

    @Test
    public void testIgnoringHttpsEntry() throws Exception {
        BaseHttpFetcher fetcher = makeFetcher(1);

        // We don't pick the entry for https://www.linkedin.com/, because
        // the protocol is different. So we get the redirect to (what seems
        // bogus) https://www.linkedin.com/hp/.
        FetchedResult result = fetcher.get("http://www.linkedin.com/");
        assertEquals(200, result.getStatusCode());
        assertEquals("http://www.linkedin.com/", result.getBaseUrl());
        assertEquals("https://www.linkedin.com/hp/", result.getFetchedUrl());
    }

    @Ignore
    @Test
    // Enable this test to try pulling a particular URL out of the common crawl dataset.
    public void testSpecificUrl() throws Exception {
        BaseHttpFetcher fetcher = makeFetcher(1);

        FetchedResult result = fetcher.get("https://customer.xfinity.com/");
        assertEquals(200, result.getStatusCode());
    }

    @Test
    public void testTwitter() throws Exception {
        BaseHttpFetcher fetcher = makeFetcher(1);

        FetchedResult result = fetcher.get("http://www.twitter.com/");

        // Redirects to https://www.twitter.com/, which isn't in the index.
        assertEquals(HttpStatus.SC_NOT_FOUND, result.getStatusCode());
        assertEquals(1, result.getNumRedirects());
    }

    @Test
    public void testMultiThreading() throws Exception {
        BaseHttpFetcher fetcher = makeFetcher(3);

        final String[] urlsToFetch = normalize(
                "http://cloudera.com/", 
                "http://cnn.com/",
                "http://uspto.gov/", 
                "http://www.google.com/", 
                "http://www.scaleunlimited.com/",
                "http://www.linkedin.com/", 
                "http://www.pinterest.com/",
                "http://www.instagram.com/");

        final Thread[] threads = new Thread[urlsToFetch.length];
        final FetchedResult[] results = new FetchedResult[urlsToFetch.length];

        for (int i = 0; i < urlsToFetch.length; i++) {
            final int index = i;
            threads[i] = new Thread(new Runnable() {

                @Override
                public void run() {
                    String url = urlsToFetch[index];
                    try {
                        results[index] = fetcher.get(url);
                    } catch (Exception e) {
                        System.out.println("Exception fetching url: " + e.getMessage());
                    }
                }
            });
        }

        // Start them all at the same time.
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }

        boolean allDone = false;
        while (!allDone) {
            Thread.sleep(100L);

            allDone = true;
            for (int i = 0; i < results.length; i++) {
                if (threads[i].isAlive()) {
                    allDone = false;
                    break;
                }
            }
        }

        for (int i = 0; i < results.length; i++) {
            FetchedResult result = results[i];
            assertNotNull("Failed to fetch URL " + urlsToFetch[i], result);
            assertEquals("Error fetching " + result.getBaseUrl(), HttpStatus.SC_OK,
                    result.getStatusCode());
        }
    }

    private String[] normalize(String... urls) {
        SimpleUrlNormalizer normalizer = new SimpleUrlNormalizer();

        String[] result = new String[urls.length];
        for (int i = 0; i < urls.length; i++) {
            result[i] = normalizer.normalize(urls[i]);
        }

        return result;
    }

    private BaseHttpFetcher makeFetcher(int numThreads) throws Exception {
        CommonCrawlFetcherBuilder builder = new CommonCrawlFetcherBuilder(numThreads,
                new UserAgent("", "", ""), "2017-17", CACHE_DIR);
        BaseHttpFetcher fetcher = builder.build();
        return fetcher;
    }
}

package fsft.wikipedia;

import io.github.fastily.jwiki.core.*;
import io.github.fastily.jwiki.dwrap.*;
import io.github.fastily.jwiki.util.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;

public class WikiMediator {
    private final Wiki wiki;
    private final Map<String, List<LocalDateTime>> requestHistory;
    private final Map<String, Integer> queryCount;

    public WikiMediator() {
        this.wiki = new Wiki.Builder().build();
        this.requestHistory = new HashMap<>();
        this.queryCount = new HashMap<>();
    }

    /**
     *  Given a searchTerm, return up to limit page titles that match
     *  the query string (per Wikipedia's search service).
     *
     * @param searchTerm the term to search for
     * @param limit the maximum number of page titles to return
     * @return a list of page titles that match the query string
     */
    public List<String> search(String searchTerm, int limit) {
        try {
            ArrayList<String> searchResults = wiki.search(searchTerm, limit);
            recordRequest(searchTerm);

            return searchResults;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     *  Given a pageTitle, return the text associated
     *  with the Wikipedia page that matches pageTitle
     *
     * @param pageTitle the term to search for
     * @return a String the text of the page with the title given
     *         or an empty string if there is no text or something
     *         went wrong.
     */
    public String getPage(String pageTitle) {
        String pageContent = wiki.getPageText(pageTitle);

        recordRequest(pageTitle);

        return pageContent;
    }

    /**
     *  Return the most common Strings (searchTerms or pageTitles)
     *  used in search and getPage requests in the most recent time
     *  window specified by duration, with items being sorted in
     *  non-increasing count order. When many requests have been made,
     *  return only limit items.
     *
     * @param duration the most recent time window
     * @param limit the most number of items to return
     * @return the most common Strings used in queries within duration
     */
    public List<String> zeitgeist(Duration duration, int limit) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Long> frequencyMap = new HashMap<>();

        for (Map.Entry<String, List<LocalDateTime>> entry : requestHistory.entrySet()) {
            List<LocalDateTime> timestamps = entry.getValue().stream()
                    .filter(timestamp -> Duration.between(timestamp, now).compareTo(duration) <= 0)
                    .toList();

            if (!timestamps.isEmpty()) {
                frequencyMap.put(entry.getKey(), (long) timestamps.size());
            }
        }

        return frequencyMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     *  Returns the maximum number of requests seen
     *  in any time window of length specified by duration.
     *  Requests include any of search, getPage, zeitgeist, and peakLoad.
     *
     * @param duration the length of a duration
     * @return the maximum number of requests seen in any time window of length duration
     */
    public int peakLoad(Duration duration) {
        LocalDateTime now = LocalDateTime.now();
        List<LocalDateTime> allTimestamps = new ArrayList<>();

        for (List<LocalDateTime> timestamps : requestHistory.values()) {
            allTimestamps.addAll(timestamps);
        }

        allTimestamps.sort(Comparator.naturalOrder());
        int maxRequests = 0;

        for (int i = 0; i < allTimestamps.size(); i++) {
            LocalDateTime startTime = allTimestamps.get(i);
            int requestCount = 0;

            for (int j = i; j < allTimestamps.size(); j++) {
                if (Duration.between(startTime, allTimestamps.get(j)).compareTo(duration) <= 0) {
                    requestCount++;
                } else {
                    break;
                }
            }

            maxRequests = Math.max(maxRequests, requestCount);
        }

        return maxRequests;
    }

    private void recordRequest(String query) {
        LocalDateTime now = LocalDateTime.now();
        requestHistory.putIfAbsent(query, new ArrayList<>());
        requestHistory.get(query).add(now);
        queryCount.put(query, queryCount.getOrDefault(query, 0) + 1);
    }
}


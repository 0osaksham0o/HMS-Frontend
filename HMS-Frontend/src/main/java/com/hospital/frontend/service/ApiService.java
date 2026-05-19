package com.hospital.frontend.service;

import com.hospital.frontend.model.PageResult;

import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ApiService {

    @Autowired private RestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Value("${backend.base-url}") private String base;

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    @SuppressWarnings("unchecked")
    public PageResult getPage(String path, String embeddedKey, int page) {
        String url = base + path + "?page=" + page + "&size=5";
        return fetchPage(url, embeddedKey);
    }

    /** Calls the /search endpoint with ?name=<term> for DB-level paginated search. */
    public PageResult getPageSearch(String path, String name, int page) {
        String url = base + path + "/search?name=" + java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8)
                + "&page=" + page + "&size=5";
        return fetchPage(url, null);
    }

    @SuppressWarnings("unchecked")
    private PageResult fetchPage(String url, String embeddedKey) {
        try {
            ResponseEntity<Map<String, Object>> resp =
                    restTemplate.exchange(url, HttpMethod.GET, null, MAP_TYPE);
            Map<String, Object> body = resp.getBody();
            PageResult r = new PageResult();

            if (body != null && body.containsKey("_embedded")) {
                Map<String, Object> embedded = (Map<String, Object>) body.get("_embedded");
                List<Map<String, Object>> rawItems = null;
                if (embeddedKey != null) {
                    rawItems = (List<Map<String, Object>>) embedded.get(embeddedKey);
                } else {
                    for (Object v : embedded.values()) {
                        if (v instanceof List) { rawItems = (List<Map<String, Object>>) v; break; }
                    }
                }
                if (rawItems == null) rawItems = Collections.emptyList();

                List<Map<String, Object>> items = new java.util.ArrayList<>();
                for (Map<String, Object> item : rawItems) {
                    Map<String, Object> mutable = new java.util.LinkedHashMap<>(item);
                    try {
                        Map<String, Object> links = (Map<String, Object>) mutable.get("_links");
                        if (links != null) {
                            Object self = links.get("self");
                            String href = null;
                            if (self instanceof Map) href = (String) ((Map<?, ?>) self).get("href");
                            else if (self instanceof String) href = (String) self;
                            if (href != null) {
                                String idStr = href.substring(href.lastIndexOf('/') + 1);
                                try { mutable.put("_selfId", Integer.parseInt(idStr)); }
                                catch (NumberFormatException ex) { mutable.put("_selfId", idStr); }
                            }
                        }
                    } catch (Exception ignored) {}
                    items.add(mutable);
                }
                r.setItems(items);

                Map<String, Object> pg = (Map<String, Object>) body.get("page");
                r.setCurrentPage(((Number) pg.get("number")).intValue());
                r.setTotalPages(((Number) pg.get("totalPages")).intValue());
                r.setTotalElements(((Number) pg.get("totalElements")).longValue());

            } else if (body != null && body.containsKey("content")) {
                r.setItems((List<Map<String, Object>>) body.get("content"));
                r.setCurrentPage(((Number) body.get("number")).intValue());
                r.setTotalPages(((Number) body.get("totalPages")).intValue());
                r.setTotalElements(((Number) body.get("totalElements")).longValue());
            } else {
                r.setItems(Collections.emptyList());
                r.setTotalPages(1);
            }

            r.setFirst(r.getCurrentPage() == 0);
            r.setLast(r.getCurrentPage() >= r.getTotalPages() - 1);
            return r;
        } catch (Exception e) {
            PageResult r = new PageResult();
            r.setItems(Collections.emptyList());
            r.setFirst(true); r.setLast(true); r.setTotalPages(1);
            return r;
        }
    }

    public Map<String, Object> getOne(String path) {
        try {
            return restTemplate.exchange(base + path, HttpMethod.GET, null, MAP_TYPE).getBody();
        } catch (Exception e) { return new HashMap<>(); }
    }

    /**
     * Fetches all items from a paged endpoint using a large page size.
     * Returns the raw item list — used for populating dropdowns.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getList(String path) {
        String url = base + path + "?page=0&size=1000";
        try {
            ResponseEntity<Map<String, Object>> resp =
                    restTemplate.exchange(url, HttpMethod.GET, null, MAP_TYPE);
            Map<String, Object> body = resp.getBody();
            if (body == null) return Collections.emptyList();
            // Spring Data REST _embedded format
            if (body.containsKey("_embedded")) {
                Map<String, Object> embedded = (Map<String, Object>) body.get("_embedded");
                for (Object v : embedded.values()) {
                    if (v instanceof List) return (List<Map<String, Object>>) v;
                }
            }
            // Plain pageable format
            if (body.containsKey("content"))
                return (List<Map<String, Object>>) body.get("content");
            return Collections.emptyList();
        } catch (Exception e) { return Collections.emptyList(); }
    }

    public void post(String path, Map<String, Object> data) {
        try {
            restTemplate.postForObject(base + path, new HttpEntity<>(data), Map.class);
        } catch (HttpStatusCodeException ex) {
            throw new RuntimeException(extractMessage(ex.getResponseBodyAsString()));
        }
    }

    public void put(String path, Map<String, Object> data) {
        try {
            restTemplate.put(base + path, new HttpEntity<>(data));
        } catch (HttpStatusCodeException ex) {
            throw new RuntimeException(extractMessage(ex.getResponseBodyAsString()));
        }
    }

    public void delete(String path) {
        try {
            restTemplate.delete(base + path);
        } catch (HttpStatusCodeException ex) {
            throw new RuntimeException(extractMessage(ex.getResponseBodyAsString()));
        }
    }

    /**
     * Extracts the human-readable "message" field from a backend JSON error response.
     * Falls back to the raw response body if parsing fails.
     */
    private String extractMessage(String responseBody) {
        try {
            Map<?, ?> map = objectMapper.readValue(responseBody, Map.class);
            Object msg = map.get("message");
            return msg != null ? msg.toString() : responseBody;
        } catch (Exception ignored) {
            return responseBody;
        }
    }
}

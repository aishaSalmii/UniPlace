package com.halcyon.uniplace.engine;

import com.halcyon.uniplace.domain.Posting;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Caches Engine match scores; a score depends only on the posting and the student's skills. */
@Service
public class MatchScoreService {

    private final EngineClient engine;
    private final EngineRequestMapper mapper;
    private final Map<String, Integer> cache = new ConcurrentHashMap<>();

    public MatchScoreService(EngineClient engine, EngineRequestMapper mapper) {
        this.engine = engine;
        this.mapper = mapper;
    }

    public int score(List<String> studentSkills, Posting posting) {
        String key = key(studentSkills, posting);
        Integer cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        int score = engine.match(mapper.toMatchRequest(studentSkills, posting)).score();
        cache.put(key, score);
        return score;
    }

    public void evict(Posting posting) {
        cache.keySet().removeIf(k -> k.startsWith(posting.getId() + "|"));
    }

    public void clear() {
        cache.clear();
    }

    static String key(List<String> studentSkills, Posting posting) {
        List<String> normalised = new ArrayList<>();
        for (String s : posting.getRequiredSkills()) {
            normalised.add(s.trim().toLowerCase(Locale.ROOT));
        }
        normalised.sort(String::compareTo);
        return posting.getId() + "|" + String.join(",", normalised);
    }
}

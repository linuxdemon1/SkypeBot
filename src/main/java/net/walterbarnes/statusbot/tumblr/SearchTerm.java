/*
 * Copyright (c) 2016.
 * This file is part of SourceBot.
 *
 * SourceBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SourceBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SourceBot.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.walterbarnes.statusbot.tumblr;

import com.tumblr.jumblr.types.Post;
import net.walterbarnes.statusbot.config.types.BlogConfig;
import net.walterbarnes.statusbot.search.SearchInclusion;
import net.walterbarnes.statusbot.search.SearchRule;
import net.walterbarnes.statusbot.util.LogHelper;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class SearchTerm implements ISearchTerm {
    private final PostCache cache = new PostCache(TimeUnit.HOURS.toSeconds(2));
    private final String term;
    private final SearchRule.SearchType type;
    public long lastTime;
    int searched;
    private int lastPostCount = 0;
    private Tumblr client = null;
    private BlogConfig blog = null;

    SearchTerm(String term, SearchRule.SearchType type) {
        this.term = term;
        this.type = type;
    }

    @Override
    public PostCache getCache() {
        return cache;
    }

    @Override
    public String getSearchTerm() {
        return getType() + ":" + getTerm();
    }

    @Override
    public Map<Post, String> getPosts(List<String> blogBlacklist, List<String> tagBlacklist, SearchInclusion rule) throws SQLException {
        if (client == null && blog == null) {
            throw new IllegalStateException("The blog and client must be set before posts can be searched");
        }
        List<Long> postBlacklist = getBlog().getPosts();

        int postNum = lastPostCount > 0 ? lastPostCount : (rule.getSampleSize() == 0 ? getBlog().getSampleSize() : rule.getSampleSize());

        lastTime = System.currentTimeMillis() / 1000;
        searched = 0;

        long start = System.currentTimeMillis();

        cache.validate();

        Map<Post, String> out = new HashMap<>();

        for (Post p : cache) {
            out.put(p, rule.getFullTerm());
        }

        LogHelper.info(String.format("Searching %s %s", getType(), getTerm()));
        while (out.size() < postNum) {
            List<Post> posts = getPostSet();

            if (posts.size() == 0 || posts.isEmpty()) break;
            out.putAll(rule.filterPosts(posts, postBlacklist, tagBlacklist, blogBlacklist, this));
        }
        long end = System.currentTimeMillis() - start;

        LogHelper.info(String.format("Searched %s %s, selected %d posts out of %d searched (%f%%), took %d ms", getType(), getTerm(),
                out.size(), searched, ((double) (((float) out.size()) / ((float) searched)) * 100), end));

        getBlog().addStat(rule.getType().toString(), getTerm(), (int) end, searched, out.size());
        lastPostCount = out.size();
        return out;
    }

    public BlogConfig getBlog() {
        return blog;
    }

    public SearchTerm setBlog(BlogConfig blog) {
        this.blog = blog;
        return this;
    }

    private SearchRule.SearchType getType() {
        return type;
    }

    String getTerm() {
        return term;
    }

    Tumblr getClient() {
        return client;
    }

    public SearchTerm setClient(Tumblr client) {
        this.client = client;
        return this;
    }
}

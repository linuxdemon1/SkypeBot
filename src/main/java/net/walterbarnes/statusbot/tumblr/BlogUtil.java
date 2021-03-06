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

import java.util.List;

public class BlogUtil {
    public static boolean olderThan(Tumblr client, String url, long time) {
        List<Post> posts;
        int offset = 0;
        while ((posts = client.blogPosts(url, offset + client.blogInfo(url).getPostCount() - 1)).size() > 0) {
            offset += posts.size();
            for (Post post : posts) {
                if (System.currentTimeMillis() / 1000 - post.getTimestamp() > time) {
                    return true;
                }
            }
        }
        return false;
    }
}

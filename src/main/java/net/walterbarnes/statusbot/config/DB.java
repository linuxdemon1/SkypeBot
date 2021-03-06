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

package net.walterbarnes.statusbot.config;

import net.walterbarnes.statusbot.config.types.BlogConfig;
import net.walterbarnes.statusbot.config.types.UserConfig;
import net.walterbarnes.statusbot.tumblr.Tumblr;
import net.walterbarnes.statusbot.util.LogHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class DB implements AutoCloseable {
    private static final long cacheTime = TimeUnit.MINUTES.toMillis(10);
    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String pass;
    private final Tumblr client;
    private Driver driver;
    private String scheme;
    private Connection connection;

    public DB(Tumblr client, String host, int port, String database, String user, String pass) {
        this.client = client;
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.pass = pass;
    }

    public static long getCacheTime() {
        return cacheTime;
    }

    public DB setDriver(String classPath) {
        try {
            driver = (Driver) Class.forName(classPath).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            LogHelper.error(e);
            throw new RuntimeException("Unable to start JDBC driver, exiting...");
        }
        return this;
    }

    public DB setScheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    public void connect() {
        if (driver == null || scheme == null)
            throw new IllegalStateException("Driver and scheme must be configured before a connection can be made");
        try {
            connection = DriverManager.getConnection(String.format("%s://%s:%s/%s", scheme, host, port, database), user, pass);
        } catch (SQLException e) {
            LogHelper.error(e);
            throw new RuntimeException("Database Error Occurred, exiting...");
        }
    }

    public Optional<UserConfig> getUserForName(String name) throws SQLException {
        Optional<UserConfig> user = Optional.empty();
        try (PreparedStatement st = connection.prepareStatement("SELECT id FROM users WHERE name = ?")) {
            st.setString(1, name);
            try (ResultSet rs = st.executeQuery()) {
                boolean firstRun = true;
                while (rs.next()) {
                    if (!firstRun)
                        throw new IllegalStateException("Multiple users exist with name '" + name + "'");
                    firstRun = false;
                    user = Optional.of(new UserConfig(client, connection, rs.getString("id")));
                }
            }
        }
        return user;
    }

    public Optional<UserConfig> getUserForId(String uid) throws SQLException {
        Optional<UserConfig> user = Optional.empty();
        try (PreparedStatement st = connection.prepareStatement("SELECT id FROM users WHERE id = ?")) {
            st.setString(1, uid);
            try (ResultSet rs = st.executeQuery()) {
                boolean firstRun = true;
                while (rs.next()) {
                    if (!firstRun)
                        throw new IllegalStateException("Multiple users exist with uid '" + uid + "'");
                    firstRun = false;
                    user = Optional.of(new UserConfig(client, connection, rs.getString("id")));
                }
            }
        }
        return user;
    }

    public List<BlogConfig> getAllBlogs() {
        List<BlogConfig> blogs = new ArrayList<>();
        try (
                PreparedStatement st = connection.prepareStatement("SELECT id FROM blogs");
                ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                blogs.add(new BlogConfig(client, connection, rs.getString("id")));
            }
        } catch (SQLException e) {
            LogHelper.error(e);
            throw new RuntimeException("Database Error Occurred, exiting...");
        }
        return blogs;
    }

    @Override
    public void close() throws SQLException {
        if (connection != null)
            connection.close();
    }
}

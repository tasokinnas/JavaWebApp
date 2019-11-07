package edu.boisestate.cs410.bugtracker.web;

import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.*;
import spark.template.pebble.PebbleTemplateEngine;

import java.security.SecureRandom;
import java.sql.*;
import java.util.*;
import java.util.Date;

import static java.sql.Connection.TRANSACTION_SERIALIZABLE;

/**
 * Server for the bug tracker database.
 */
public class BugTrackerServer {
    private static final Logger logger = LoggerFactory.getLogger(BugTrackerServer.class);

    private final PoolingDataSource<? extends Connection> pool;
    private final Service http;
    private final TemplateEngine engine;

    //Constructor
    public BugTrackerServer(PoolingDataSource<? extends Connection> pds, Service svc) {
        pool = pds;
        http = svc;
        // new Classpathloader() needs to be added for mac/linux
        engine = new PebbleTemplateEngine(new ClasspathLoader());

        /*
        // Please keep the routes in the same order as the route
        // handler functions, by order of first use.
        // Order:
        //  1. Get
        //  2. Post
        */

        // Get routes
        ///////////////////////////////////////////////////////////
        // Home page route
        http.get("/", this::indexPage, engine);
        // logout route
        http.get("/logout", this::logout);
        // Register user route :: Creates a new user
        http.get("/registeruser", this::registerUserPage, engine);
        // Register bug route :: Create a new bug
        http.get("/registerbug", this::registerBugPage, engine);
        // User Preferences route
        http.get("/userprefs", this::userPrefsPage, engine);
        // Bug List route
        http.get("/buglist", this::bugListPage, engine);
        // About page route
        http.get("/about", this::aboutPage, engine);
        // Main Bug page :: shows bug information, allows for simple
        // interactions with the bug
        http.get("/bugs/:bugid", this::bugInfoPage, engine);
        // Search for the bugs
        http.get("/searchbug", this::searchBugs, engine);
        // List milestones
        http.get("/milestonelist", this::milestoneList, engine);
        // Add Milestone page
        http.get("/addmilestone", this::addMilestone, engine);
        // Milestone Detail
        http.get("/milestone/:milestoneid", this::milestoneDetail, engine);


        // Post routes
        /////////////////////////////////////////////////////////////
        // Login route :: Logs in the user
        http.post("/login", this::login);
        // Create User route :: Creates a new user
        http.post("/createuser", this::createUserPage);
        // Create Bug route :: Create a new bug
        http.post("/createbug", this::createBugPage);
        // Update User route :: Update existing user
        http.post("/updateuser", this::updateUserPage);
        // Add new milestone
        http.post("/createmilestone", this::createMilestone);
        // Subscribe to tag
        http.post("/subscribetag", this::subscribeTag);

    }

    //////////////////////////////////////////////////////////
    /// Begin Get Routes Section /////////////////////////////
    //////////////////////////////////////////////////////////

    ModelAndView indexPage(Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();

        //Get the user's information, do not require a logged in user
        User user = getUser(request, fields);

        // initialize CSRF token
        String token = request.session().attribute("csrf_token");
        if (token == null) {
            SecureRandom rng = new SecureRandom();
            byte[] bytes = new byte[8];
            rng.nextBytes(bytes);
            token = Base64.getEncoder().encodeToString(bytes);
            request.session(true).attribute("csrf_token", token);
        }
        fields.put("csrf_token", token);

        ArrayList<HashMap<String,Object>> userBugs = new ArrayList<>();
        ArrayList<HashMap<String,Object>> tagBugs = new ArrayList<>();
        if(user != null){
            String getUserBugs = "SELECT aa.bug_id,\n" +
                    "       aa.bug_title,\n" +
                    "       aa.bug_body,\n" +
                    "       aa.bug_status,\n" +
                    "       aa.create_date,\n" +
                    "       aa.close_date,\n" +
                    "       aa.user_id,\n" +
                    "       aa.milestone_id\n" +
                    "FROM bugs aa\n" +
                    "WHERE aa.user_id = ?" +
                    " ORDER BY aa.create_date DESC;";

            String getTagBugs = "SELECT DISTINCT\n" +
                    "  cc.bug_id,\n" +
                    "  cc.bug_title,\n" +
                    "  cc.bug_body,\n" +
                    "  cc.bug_status,\n" +
                    "  cc.create_date,\n" +
                    "  cc.close_date,\n" +
                    "  cc.user_id,\n" +
                    "  cc.milestone_id\n" +
                    " FROM tags aa\n" +
                    "  JOIN tag_bug_xref bb\n" +
                    "    ON aa.tag_id = bb.tag_id\n" +
                    "  JOIN bugs cc\n" +
                    "    ON bb.bug_id = cc.bug_id\n" +
                    "  JOIN user_tag_subscription dd\n" +
                    "    ON dd.tag_id = aa.tag_id\n" +
                    "  JOIN users ee\n" +
                    "    ON ee.user_id = dd.user_id\n" +
                    " WHERE ee.user_id = ?" +
                    " ORDER BY cc.create_date DESC ;";
            try (Connection cxn = pool.getConnection()){
                 try(PreparedStatement stmt = cxn.prepareStatement(getUserBugs)) {
                    stmt.setInt(1, user.getUser_id());
                    try(ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            HashMap<String, Object> bug = new HashMap<String, Object>();
                            int cur_bug_id = rs.getInt("bug_id");
                            bug.put("id", cur_bug_id);
                            bug.put("title", rs.getString("bug_title"));
                            bug.put("body", rs.getString("bug_body"));
                            bug.put("status", rs.getString("bug_status"));
                            bug.put("create_date", rs.getString("create_date"));
                            bug.put("close_date", rs.getString("close_date"));
                            bug.put("user_id", rs.getString("user_id"));
                            bug.put("milestone_id", rs.getString("milestone_id"));
                            userBugs.add(bug);
                        }
                    }
                }
                try(PreparedStatement stmt = cxn.prepareStatement(getTagBugs)) {
                    stmt.setInt(1, user.getUser_id());
                    try(ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            HashMap<String, Object> bug = new HashMap<String, Object>();
                            int cur_bug_id = rs.getInt("bug_id");
                            bug.put("id", cur_bug_id);
                            bug.put("title", rs.getString("bug_title"));
                            bug.put("body", rs.getString("bug_body"));
                            bug.put("status", rs.getString("bug_status"));
                            bug.put("create_date", rs.getString("create_date"));
                            bug.put("close_date", rs.getString("close_date"));
                            bug.put("user_id", rs.getString("user_id"));
                            bug.put("milestone_id", rs.getString("milestone_id"));
                            tagBugs.add(bug);
                        }
                    }
                }
            }
        }

        fields.put("userBugs", userBugs);
        fields.put("tagBugs", tagBugs);

        return new ModelAndView(fields, "index.html");
    }

    String logout(Request request, Response response) {
        request.session().removeAttribute("userId");
        response.redirect("/", 303);
        return "Goodbye";
    }

    ModelAndView registerUserPage(Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();
        return new ModelAndView(fields, "registeruser.html");
    }

    ModelAndView registerBugPage(Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();
        User user = getUser(request, fields);
        if(user == null){
            this.logout(request,response);
            return null;
        }

        return new ModelAndView(fields, "registerbug.html");
    }

    ModelAndView userPrefsPage(Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();

        User user = getUser(request, fields);
        if(user == null){
            this.logout(request,response);
            return null;
        }
        // initialize CSRF token
        String token = request.session().attribute("csrf_token");
        if (token == null) {
            SecureRandom rng = new SecureRandom();
            byte[] bytes = new byte[8];
            rng.nextBytes(bytes);
            token = Base64.getEncoder().encodeToString(bytes);
            request.session(true).attribute("csrf_token", token);
        }
        fields.put("csrf_token", token);

        return new ModelAndView(fields, "userprefs.html");
    }

    ModelAndView bugListPage(Request request, Response response) throws SQLException {
        Map<String, Object> fields = new HashMap<>();

        User user = getUser(request, fields);
        if(user == null){
            this.logout(request,response);
            return null;
        }

        // initialize CSRF token
        String token = request.session().attribute("csrf_token");
        if (token == null) {
            SecureRandom rng = new SecureRandom();
            byte[] bytes = new byte[8];
            rng.nextBytes(bytes);
            token = Base64.getEncoder().encodeToString(bytes);
            request.session(true).attribute("csrf_token", token);
        }
        fields.put("csrf_token", token);

        if (user != null) {
            List<Map<String, Object>> bugs = new ArrayList<>();
            try (Connection cxn = pool.getConnection()) {
                try (Statement stmt = cxn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT bug_title, bug_id, bug_status, create_date \n" +
                             "FROM bugs\n" +
                             "ORDER BY create_date DESC")) {

                    while (rs.next()) {
                        Map<String, Object> bug = new HashMap<>();
                        int cur_bug_id = rs.getInt("bug_id");
                        bug.put("id", cur_bug_id);
                        bug.put("title", rs.getString("bug_title"));
                        bug.put("status", rs.getString("bug_status"));
                        bug.put("createdate", rs.getDate("create_date"));
                        try (PreparedStatement tagStmt = cxn.prepareStatement("select cc.tag \n" +
                                "     from bugs aa\n" +
                                "     join tag_bug_xref bb\n" +
                                "     on aa.bug_id = bb.bug_id\n" +
                                "     join tags cc\n" +
                                "     on cc.tag_id = bb.tag_id\n" +
                                "     where aa.bug_id = ?")) {
                            tagStmt.setInt(1, cur_bug_id);
                            ResultSet tagRs = tagStmt.executeQuery();
                            ArrayList<String> tags = new ArrayList<>();
                            while (tagRs.next()) {
                                tags.add(tagRs.getString("tag"));
                            }

                            StringBuilder sb = new StringBuilder();
                            for (String tag : tags)
                            {
                                sb.append(tag);
                                sb.append(" ");
                            }
                            bug.put("tags", sb.toString());
                        }

                        bugs.add(bug);
                    }
                    //fields.put("totalDonors", rs.getBigDecimal("total_donors"));
                }
                fields.put("bugs", bugs);
            }
        }
        return new ModelAndView(fields, "buglist.html");
    }

    ModelAndView aboutPage(Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();
        //Todo: Get about page info
        return new ModelAndView(fields, "about.html");
    }

    ModelAndView bugInfoPage(Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();
        int bug_id = Integer.parseInt(request.params("bugid"));

        User user = getUser(request, fields);
        if(user == null){
            this.logout(request, response);
            return null;
        }

        String query = "SELECT aa.bug_id,\n" +
              "       aa.bug_title,\n" +
              "       aa.bug_body,\n" +
              "       aa.bug_status,\n" +
              "       aa.create_date,\n" +
              "       aa.close_date,\n" +
              "       aa.user_id,\n" +
              "       aa.milestone_id\n" +
              "FROM bugs aa\n" +
              "WHERE aa.bug_id = ?";
        try (Connection cxn = pool.getConnection();
          PreparedStatement stmt = cxn.prepareStatement(query)) {
         stmt.setInt(1, bug_id);
         try(ResultSet rs = stmt.executeQuery()) {
             HashMap<String, Object> bug = new HashMap<String, Object>();
             if(!rs.next()){
                 throw new IllegalStateException("Bug Not Found.");
             }
             int cur_bug_id = rs.getInt("bug_id");
             bug.put("id", cur_bug_id);
             bug.put("title", rs.getString("bug_title"));
             bug.put("body", rs.getString("bug_body"));
             bug.put("status", rs.getString("bug_status"));
             bug.put("create_date", rs.getString("create_date"));
             bug.put("close_date", rs.getString("close_date"));
             bug.put("user_id", rs.getString("user_id"));
             bug.put("milestone_id", rs.getString("milestone_id"));
             try (PreparedStatement tagStmt = cxn.prepareStatement("select cc.tag \n" +
                     "     from bugs aa\n" +
                     "     join tag_bug_xref bb\n" +
                     "     on aa.bug_id = bb.bug_id\n" +
                     "     join tags cc\n" +
                     "     on cc.tag_id = bb.tag_id\n" +
                     "     where aa.bug_id = ?")) {
                 tagStmt.setInt(1, cur_bug_id);
                 ResultSet tagRs = tagStmt.executeQuery();
                 ArrayList<String> tags = new ArrayList<>();
                 while (tagRs.next()) {
                     tags.add(tagRs.getString("tag"));
                 }

                 StringBuilder sb = new StringBuilder();
                 for (String tag : tags)
                 {
                     sb.append(tag);
                     sb.append(" ");
                 }
                 bug.put("tags", sb.toString());
                 bug.put("tagList", tags);
             }
             fields.put("bug", bug);
         }
        }
        return new ModelAndView(fields, "bugInfo.html");
    }

    ModelAndView searchBugs(Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();

        User user = getUser(request, fields);
        if(user == null){
            this.logout(request, response);
            return null;
        }

        //If no search term just show the full list...
        String searchTerm = request.queryParams("searchterm");
        if (searchTerm == null || searchTerm.isEmpty()) {
            searchTerm = "";
        }

        String searchQuery = "SELECT bug_title, bug_id, bug_status, create_date "+
                "FROM bugs " +
                "WHERE bug_title like '%' || ? || '%' " +
                "ORDER BY create_date DESC ";
        List<Map<String, Object>> bugs = new ArrayList<>();
        try (Connection cxn = pool.getConnection();
             PreparedStatement stmt = cxn.prepareStatement(searchQuery)) {
            stmt.setString(1, searchTerm);
            stmt.execute();
            try (ResultSet rs = stmt.getResultSet()) {
                while(rs.next()){
                    Map<String, Object> bug = new HashMap<>();
                    int cur_bug_id = rs.getInt("bug_id");
                    bug.put("id", cur_bug_id);
                    bug.put("title", rs.getString("bug_title"));
                    bug.put("status", rs.getString("bug_status"));
                    bug.put("createdate", rs.getDate("create_date"));
                    try (PreparedStatement tagStmt = cxn.prepareStatement("select cc.tag \n" +
                            "     from bugs aa\n" +
                            "     join tag_bug_xref bb\n" +
                            "     on aa.bug_id = bb.bug_id\n" +
                            "     join tags cc\n" +
                            "     on cc.tag_id = bb.tag_id\n" +
                            "     where aa.bug_id = ?")) {
                        tagStmt.setInt(1, cur_bug_id);
                        ResultSet tagRs = tagStmt.executeQuery();
                        ArrayList<String> tags = new ArrayList<>();
                        while (tagRs.next()) {
                            tags.add(tagRs.getString("tag"));
                        }

                        StringBuilder sb = new StringBuilder();
                        for (String tag : tags)
                        {
                            sb.append(tag);
                            sb.append(" ");
                        }
                        bug.put("tags", sb.toString());
                    }

                    bugs.add(bug);
                }
            }
        }
        fields.put("bugs", bugs);

        return new ModelAndView(fields, "buglist.html");
    }

    ModelAndView milestoneList(Request request, Response response) throws SQLException {
        Map<String, Object> fields = new HashMap<>();

        User user = getUser(request, fields);
        if(user == null){
            this.logout(request,response);
            return null;
        }

        // initialize CSRF token
        String token = request.session().attribute("csrf_token");
        if (token == null) {
            SecureRandom rng = new SecureRandom();
            byte[] bytes = new byte[8];
            rng.nextBytes(bytes);
            token = Base64.getEncoder().encodeToString(bytes);
            request.session(true).attribute("csrf_token", token);
        }
        fields.put("csrf_token", token);

        List<Map<String, Object>> milestones = new ArrayList<>();
        try (Connection cxn = pool.getConnection()) {
            try (Statement stmt = cxn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT \n" +
                         "  aa.milestone_id, \n" +
                         "  aa.milestone_name, \n" +
                         "  aa.milestone_description, \n" +
                         "  count(bug_id) AS bug_count \n" +
                         " FROM milestones aa \n" +
                         "  LEFT JOIN bugs bb \n" +
                         "    ON aa.milestone_id = bb.milestone_id \n" +
                         " GROUP BY aa.milestone_id, aa.milestone_name, aa.milestone_description;")) {
                while (rs.next()) {
                    Map<String, Object> milestone = new HashMap<>();
                    //int cur_milestone_id = rs.getInt("bug_id");
                    milestone.put("id", rs.getInt("milestone_id"));
                    milestone.put("name", rs.getString("milestone_name"));
                    milestone.put("description", rs.getString("milestone_description"));
                    milestone.put("bugCount", rs.getString("bug_count"));
                    milestones.add(milestone);
                }
            }

        }
        fields.put("milestones", milestones);
        return new ModelAndView(fields, "milestonelist.html");
    }

    ModelAndView addMilestone(Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();

        User user = getUser(request, fields);
        if(user == null){
            this.logout(request, response);
            return null;
        }
        return new ModelAndView(fields, "addmilestone.html");
    }

    ModelAndView milestoneDetail(Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();
        int milestone_id = Integer.parseInt(request.params("milestoneid"));

        User user = getUser(request, fields);
        if(user == null){
            this.logout(request, response);
            return null;
        }

        String query = "SELECT\n" +
                "  milestone_id,\n" +
                "  milestone_name,\n" +
                "  milestone_description\n" +
                "FROM milestones\n" +
                "WHERE milestone_id = ?;";

        String bugListQuery = "SELECT aa.bug_id,\n" +
                "       aa.bug_title,\n" +
                "       aa.bug_body,\n" +
                "       aa.bug_status,\n" +
                "       aa.create_date,\n" +
                "       aa.close_date,\n" +
                "       aa.user_id,\n" +
                "       aa.milestone_id\n" +
                "FROM bugs aa\n" +
                "WHERE aa.milestone_id = ?";

        try (Connection cxn = pool.getConnection();
             PreparedStatement stmt = cxn.prepareStatement(query)) {
            stmt.setInt(1, milestone_id);
            try(ResultSet rs = stmt.executeQuery()) {
                HashMap<String, Object> milestone = new HashMap<String, Object>();
                if(!rs.next()){
                    throw new IllegalStateException("Milestone Not Found.");
                }
                milestone.put("milestone_id", milestone_id);
                milestone.put("milestone_name", rs.getString("milestone_name"));
                milestone.put("milestone_description", rs.getString("milestone_description"));
                try (PreparedStatement bugStmt = cxn.prepareStatement(bugListQuery)) {
                    bugStmt.setInt(1, milestone_id);
                    ResultSet bugRs = bugStmt.executeQuery();
                    ArrayList<HashMap<String, Object>> bugs = new ArrayList<>();
                    while (bugRs.next()) {
                        HashMap<String, Object> bug = new HashMap<>();
                        bug.put("id", bugRs.getInt("bug_id"));
                        bug.put("title", bugRs.getString("bug_title"));
                        bug.put("body", bugRs.getString("bug_body"));
                        bug.put("status", bugRs.getString("bug_status"));
                        bug.put("create_date", bugRs.getString("create_date"));
                        bug.put("close_date", bugRs.getString("close_date"));
                        bug.put("user_id", bugRs.getString("user_id"));
                        bug.put("milestone_id", bugRs.getString("milestone_id"));
                        bugs.add(bug);
                    }
                    milestone.put("bugs", bugs);
                }
                fields.put("milestone", milestone);
            }
        }
        return new ModelAndView(fields, "milestoneInfo.html");
    }

    /////////////////////////////////////////////////////////
    /// End Get Routes Section///////////////////////////////
    /////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////
    /// Begin Post Routes Section ////////////////////////////
    //////////////////////////////////////////////////////////

    String login(Request request, Response response) throws SQLException {
        String name = request.queryParams("username");
        if (name == null || name.isEmpty()) {
            http.halt(400, "No user name provided");
        }
        String password = request.queryParams("password");
        if (password == null || password.isEmpty()) {
            http.halt(400, "No password provided");
        }

        String userQuery = "SELECT user_id, user_password FROM users WHERE user_name = ?";

        try (Connection cxn = pool.getConnection();
             PreparedStatement stmt = cxn.prepareStatement(userQuery)) {
            stmt.setString(1, name);
            logger.debug("looking up user {}", name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    logger.debug("found user {}", name);
                    String hash = rs.getString("user_password");
                    if (BCrypt.checkpw(password, hash)) {
                        logger.debug("user {} has valid password", name);
                        request.session(true).attribute("userId", rs.getLong("user_id"));
                        response.redirect("/", 303);
                        return "Hi!";
                    } else {
                        logger.debug("invalid password for user {}", name);
                    }
                } else {
                    logger.debug("no user {} found", name);
                }
            }
        }

        http.halt(400, "invalid username or password");
        return null;
    }

    String createUserPage(Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();

        String userName = request.queryParams("username");
        if (userName == null || userName.isEmpty()) {
            http.halt(400, "No user name provided");
        }

        String email = request.queryParams("email");
        if (email == null || email.isEmpty()) {
            http.halt(400, "No email provided");
        }

        String displayName = request.queryParams("displayname");
        if (displayName == null || displayName.isEmpty()) {
            http.halt(400, "No display name provided");
        }

        String password = request.queryParams("password");
        if (password == null || password.isEmpty()) {
            http.halt(400, "No password provided");
        }
        if (!password.equals(request.queryParams("confirm"))) {
            http.halt(400, "Password and confirmation do not match.");
        }
        String pwHash = BCrypt.hashpw(password, BCrypt.gensalt(10));

        String existingUserQuery = "Select *\n" +
                "FROM users\n" +
                "WHERE lower(user_name) = lower(?)";
        try (Connection cxn = pool.getConnection();
             PreparedStatement stmt = cxn.prepareStatement(existingUserQuery)) {
            stmt.setString(1, userName);
            stmt.execute();
            try (ResultSet rs = stmt.getResultSet()) {
                if(rs.next()){
                    http.halt(400, "User already exists with the user name: " + userName);
                }
            }
        }

        String addUser = "INSERT INTO users (user_name, user_email, display_name, user_password) " +
                "VALUES (?, ?, ?, ?) " +
                "RETURNING user_id";

        long userId;

        try (Connection cxn = pool.getConnection();
             PreparedStatement stmt = cxn.prepareStatement(addUser)) {
            stmt.setString(1, userName);
            stmt.setString(2, email);
            stmt.setString(3, displayName);
            stmt.setString(4, pwHash);

            stmt.execute();
            try (ResultSet rs = stmt.getResultSet()) {
                rs.next();
                userId = rs.getLong(1);
                logger.info("added user {} with id {}, email: {}, display name: {}", userName, userId, email, displayName);
            }
        }

        Session session = request.session(true);
        session.attribute("userId", userId);

        response.redirect("/", 303);
        return "User Created.";
    }

    String updateUserPage(Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();

        User user = getUser(request, fields);
        if(user == null){
            this.logout(request, response);
            return null;
        }

        String userName = request.queryParams("username");
        if (userName == null || userName.isEmpty()) {
            http.halt(400, "No user name provided");
        }

        String email = request.queryParams("email");
        if (email == null || email.isEmpty()) {
            http.halt(400, "No email provided");
        }

        String displayName = request.queryParams("displayname");
        if (displayName == null || displayName.isEmpty()) {
            http.halt(400, "No display name provided");
        }

        String password = request.queryParams("password");
        if (password == null || password.isEmpty()) {
            http.halt(400, "No password provided");
        }
        if (!password.equals(request.queryParams("confirm"))) {
            http.halt(400, "Password and confirmation do not match.");
        }
        String pwHash = BCrypt.hashpw(password, BCrypt.gensalt(10));

        String updateQuery  = "UPDATE users  \n" +
                "    SET user_name = ?\n" +
                "      , user_email = ?\n" +
                "      , display_name = ?\n" +
                "      , user_password = ?\n" +
                "      WHERE user_id =  ?  ";

        try (Connection cxn = pool.getConnection()) {
            // put in the URL
            boolean succeeded = false;
            cxn.setTransactionIsolation(TRANSACTION_SERIALIZABLE);
            cxn.setAutoCommit(false);
            try {
                int retryCount = 5;
                while (retryCount > 0) {
                    try {
                        try (PreparedStatement stmt = cxn.prepareStatement(updateQuery)) {
                            stmt.setString(1, userName);
                            stmt.setString(2, email);
                            stmt.setString(3, displayName);
                            stmt.setString(4, pwHash);
                            stmt.setInt(5, user.getUser_id());
                            stmt.execute();
                        }
                        cxn.commit();
                        succeeded = true;
                        retryCount = 0;
                        logger.info("successfully updated user");
                    } catch (SQLException ex) {
                        if (ex.getErrorCode() / 1000 == 23) {
                            logger.info("integrity error updating the database entity, retrying", ex);
                            retryCount--;
                        } else {
                            logger.info("other error encountered updating to database, aborting", ex);
                            throw ex;
                        }
                    } finally {
                        if (!succeeded) {
                            cxn.rollback();
                            http.halt(500, "Failed to update user");
                            return "failed to update user, try again";
                        }
                    }
                }
            } finally {
                cxn.setAutoCommit(true);
            }
        }

        response.redirect("/", 303);
        return "successfully updated user";
    }

    String createBugPage(Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();

        User user = getUser(request, fields);
        if(user == null){
            this.logout(request, response);
            return null;
        }

        String bugTitle = request.queryParams("bug_title");
        if (bugTitle == null || bugTitle.isEmpty()) {
            http.halt(400, "No bug title provided");
        }

        String bugStatus = request.queryParams("bug_status");
        if (bugStatus == null || bugStatus.isEmpty()) {
            http.halt(400, "No bug status provided");
        }

        String bugBody = request.queryParams("bug_body");
        if (bugBody == null || bugBody.isEmpty()) {
            http.halt(400, "No bug body provided");
        }

        String bugTags = request.queryParams("bug_tags");
        Set<String> tags = new HashSet<>();

        for(String t : bugTags.split("\\s")){
            tags.add(t);
        }

        int bugId = -1;
        String insertQuery = "INSERT INTO bugs ( " +
                "bug_title , " +
                "bug_body , " +
                "bug_status , " +
                "create_date , " +
                "close_date , " +
                "user_id , " +
                "milestone_id) " +
                "VALUES (?,?,?,?,?,?,?)" +
                "RETURNING bug_id";

        try (Connection cxn = pool.getConnection();
             PreparedStatement stmt = cxn.prepareStatement(insertQuery)) {
            stmt.setString(1, bugTitle);
            stmt.setString(2, bugBody);
            stmt.setString(3, bugStatus);
            stmt.setDate(4, new java.sql.Date(new Date().getTime()));
            stmt.setDate(5, null);
            stmt.setInt(6, user.getUser_id());
            stmt.setObject(7, null);

            stmt.execute();
            try (ResultSet rs = stmt.getResultSet()) {
                rs.next();
                bugId = rs.getInt(1);
            }

        //Lets add some tags!!

        String insertTagQuery = "insert into tags (tag) \n" +
                "values ( ? );";
        String selectTagQuery = "select tag_id from tags where tag = ?; ";

        String insertTagBugXref = "insert into tag_bug_xref (tag_id, bug_id) \n" +
                "values ( ?, ? ) ";


            for(String tag : tags){
                try(PreparedStatement insStatement = cxn.prepareStatement(insertTagQuery);PreparedStatement selStmt = cxn.prepareStatement(selectTagQuery)) {
                    insStatement.setString(1, tag);
                    try{
                        insStatement.execute();
                    }
                    catch (Exception ex){

                    }

                    selStmt.setString(1, tag);
                    selStmt.execute();

                    try (ResultSet rs = selStmt.getResultSet()) {
                        if (!rs.next()) {
                            http.halt(500, "Failed to create tag.");
                        }
                        int tag_id = rs.getInt("tag_id");
                        try (PreparedStatement xref_stmt = cxn.prepareStatement(insertTagBugXref)) {
                            xref_stmt.setInt(1, tag_id);
                            xref_stmt.setInt(2, bugId);
                            try {
                                xref_stmt.execute();
                            }
                            catch (Exception ex){
                                http.halt(500, "Failed to add tags to bug. " + ex.getMessage());
                            }
                        }
                    }
                }
            }
        }

        response.redirect("/bugs/" + bugId, 303);
        return "Bug Added";
    }

    String createMilestone(Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();

        User user = getUser(request, fields);
        if(user == null){
            this.logout(request, response);
            return null;
        }

        String milestoneName = request.queryParams("milestone_name");
        if (milestoneName == null || milestoneName.isEmpty()) {
            http.halt(400, "No milestone name provided");
        }

        String milestoneDesc = request.queryParams("milestone_description");
        if (milestoneDesc == null || milestoneDesc.isEmpty()) {
            http.halt(400, "No milestone description provided");
        }

        int milestoneId = -1;
        String insertQuery = "INSERT INTO milestones " +
                "(milestone_name, milestone_description) \n" +
                "    VALUES ( ?, ? ) " +
                "RETURNING milestone_id;";

        try (Connection cxn = pool.getConnection();
             PreparedStatement stmt = cxn.prepareStatement(insertQuery)) {
            stmt.setString(1, milestoneName);
            stmt.setString(2, milestoneDesc);


            stmt.execute();
            try (ResultSet rs = stmt.getResultSet()) {
                rs.next();
                milestoneId = rs.getInt(1);
            }
        }

        response.redirect("/milestone/" + milestoneId, 303);
        return "Milestone Added";
    }

    String subscribeTag(Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();

        User user = getUser(request, fields);
        if(user == null){
            this.logout(request, response);
            return null;
        }

        String tag_name = request.queryParams("tag_name");
        if (tag_name == null || tag_name.isEmpty()) {
            http.halt(400, "No tag provided");
        }

        int tagId = -1;

        String getTagQuery = "SELECT aa.tag_id " +
                "FROM tags aa "  +
                "WHERE aa.tag = ? ;";

        try (Connection cxn = pool.getConnection();
             PreparedStatement stmt = cxn.prepareStatement(getTagQuery)) {
            stmt.setString(1, tag_name);
            stmt.execute();
            ResultSet rs = stmt.executeQuery();
            if(!rs.next()){
                http.halt(500, "Tag not found");
            }
            tagId = rs.getInt("tag_id");
        }

        if(tagId == -1)
            http.halt(500, "Tag not found");

        String insertQuery = "INSERT INTO user_tag_subscription (user_id, tag_id)\n" +
                "VALUES (? , ?);";

        try (Connection cxn = pool.getConnection();
             PreparedStatement stmt = cxn.prepareStatement(insertQuery)) {
            stmt.setInt(1, user.getUser_id());
            stmt.setInt(2, tagId);
            try {
                stmt.execute();
            } catch(Exception ex){
                //Ignoring constraint violations
            }
        }

        response.redirect("/", 303);
        return "Subscribed to tag";
    }


    //////////////////////////////////////////////////////////
    /// End Post Routes Section //////////////////////////////
    //////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////
    /// Begin Helper Section /////////////////////////////////
    //////////////////////////////////////////////////////////

    //Get the user object and populate the map.
    private User getUser(Request request, Map<String, Object> fields) throws SQLException {
        Long uid = request.session().attribute("userId");
        if (uid == null) {
            return null;
        }

        User user = null;
        String userQuery = "select\n" +
                " aa.user_id\n" +
                ",aa.user_name\n" +
                ",aa.user_email\n" +
                ",aa.display_name\n" +
                ",aa.tag_id\n" +
                ",aa.bug_id \n" +
                "from users aa\n" +
                "where aa.user_id = ?";
        try (Connection cxn = pool.getConnection();
             PreparedStatement stmt = cxn.prepareStatement(userQuery)) {
            stmt.setLong(1, uid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    user = new User(rs.getInt("user_id")
                            , rs.getString("user_name")
                            , rs.getString("user_email")
                            , rs.getString("display_name")
                            , rs.getInt("tag_id")
                            , rs.getInt("bug_id"));
                }
            }
        }

        fields.put("user", user);
        return user;
    }

    //////////////////////////////////////////////////////////
    /// End Helper Section ///////////////////////////////////
    //////////////////////////////////////////////////////////

    //Method is not used... would like to remove
    // public String redirectToFolder(Request request, Response response) {
    //     String path = request.pathInfo();
    //     response.redirect(path + "/", 301);
    //     return "Redirecting to " + path + "/";
    // }
}

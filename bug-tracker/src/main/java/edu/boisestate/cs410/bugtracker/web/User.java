package edu.boisestate.cs410.bugtracker.web;

/**
 * A user in the bugtracker system.
 */
public class User {
    private int user_id;
    private String user_name;
    private String user_email;
    private String display_name;
    private int tag_id;
    private int bug_id;

    public User(int user_id, String user_name, String user_email, String display_name, int tag_id, int bug_id) {
        this.user_id = user_id;
        this.user_name = user_name;
        this.user_email = user_email;
        this.display_name = display_name;
        this.tag_id = tag_id;
        this.bug_id = bug_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getUser_email() {
        return user_email;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public int getTag_id() {
        return tag_id;
    }

    public void setTag_id(int tag_id) {
        this.tag_id = tag_id;
    }

    public int getBug_id() {
        return bug_id;
    }

    public void setBug_id(int bug_id) {
        this.bug_id = bug_id;
    }

    public String getUser_name() {
        return user_name;
    }

    public int getUser_id() {
        return user_id;
    }
}



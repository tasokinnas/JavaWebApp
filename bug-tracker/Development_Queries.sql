INSERT INTO bugs ( bug_title , bug_body , bug_status , create_date , close_date , user_id , milestone_id)
VALUES (?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?) -- SELECT *
-- FROM users;

SELECT aa.bug_id,
       aa.bug_title,
       aa.bug_body,
       aa.bug_status,
       aa.create_date,
       aa.close_date,
       aa.user_id,
       aa.milestone_id
FROM bugs aa
WHERE aa.bug_id = ?
    SELECT aa.user_id,
           aa.user_name,
           aa.user_email,
           aa.display_name,
           aa.tag_id,
           aa.bug_id
    FROM users aa WHERE aa.user_id = 1
    DELETE
    FROM users WHERE user_id =1
    UPDATE users
    SET user_name = 't' ,
        user_email = 't' ,
        display_name = 't' ,
        user_password = 't' WHERE user_id = 1

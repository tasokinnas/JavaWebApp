--PROJECT SAMPLE QUERIES
 --QUERY 1: A list of the top 20 users to whom fixed the most bugs.

SELECT debugger_id count(bug_id) AS Total_bugs_fixed
FROM bugs
JOIN Milestones USING (milestone_id)
WHERE description = 'complete'
GROUP BY debugger_id
ORDER BY Total_bugs_fixed DESC LIMIT 20;

--QUERY 2: A list of bugs that contain the tag 'SQL' and the number of users that subscribe to each bug

SELECT bug_id,
       tag_id,
       tag_name,
       count(user_id) AS num_subscribers
FROM User_Subscribe_Bugs
JOIN bugs USING (bug_id)
JOIN tags USING (tag_id)
WHERE tag_name='SQL'
GROUP BY bug_id,
         tag_id,
         tag_name
ORDER BY num_subscribers;

--QUERY 3: A list of bugs that have been assigned by are not started, sorted from oldest to newest

SELECT bug_id,
       title,
       create_date
FROM bugs
JOIN milestones
WHERE milestone.description='not started'
ORDER BY create_date ASC;

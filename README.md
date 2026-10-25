Task Manager application 

1. splited into two main classes task and task manager and we have featueres each one do

TaskManger :

- Add a task
- Find a task by id instantly (no looping through all tasks)
- List all tasks
- Mark a task as done


Tasks:
- id, a title, a status (pending or done) and tags
- Task types   BugTask (has a severity: low, medium, high)  ,   FeatureTask (has an estimate in hours).


2. Sorting
- TaskManager can list tasks sorted in different ways: by title, by status (pending first), or in the order they were added.
- Each way of sorting is its own class, and they all share the same interface.
- Adding a new sort order later should only mean adding one new class, with no changes to TaskManager.

3. Design patterns:
- we use (factory method) in Task diffrent types creation
- we use (Strategy pattern) in sorting with diffrent ways 
- we use (Strategy pattern) again in filtering with diffrent rules

4. Filtering
- TaskManager can list only the tasks that match a filter: by status, by tag, or by bug severity.
- Filters can be combined with AndFilter (for example: tagged "urgent" AND still pending).
- A filter and a sort can be used together: listTasks(filter, sortStrategy).
- Each filter is its own class sharing the TaskFilter interface, so adding a new filter later only means adding one new class, with no changes to TaskManager.

and aslo we save changes by github and git version control



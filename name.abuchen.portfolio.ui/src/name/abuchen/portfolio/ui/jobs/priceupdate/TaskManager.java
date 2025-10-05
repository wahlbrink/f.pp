package name.abuchen.portfolio.ui.jobs.priceupdate;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import name.abuchen.portfolio.model.Security;

/* packiage */ final class TaskManager
{
    private static class Group
    {
        private final String id;
        private final List<Task> allTasks;

        private final Deque<Task> remainingTasks;

        private int maxWorker;
        private int currentWorker;

        // number of attempts before failing permanently
        private int maxAttempts;

        private int nFinished;

        Group(String id, List<Task> tasks, int maxParallel)
        {
            this.id = id;
            this.allTasks = tasks;
            this.remainingTasks = new ArrayDeque<>(tasks);

            this.maxWorker = Math.min(maxParallel, tasks.size());

            // this is not 100% correct because the list of tasks could contain
            // different feeds. However, for now only CoinGecko has a different
            // number of attempts and CoinGecko is grouped into one list
            this.maxAttempts = tasks.getFirst().getFeed().getMaxRateLimitAttempts();
        }

        void onTaskFinished(Task task)
        {
            nFinished++;
        }

        Duration getMaxDuration()
        {
            var max = allTasks.stream().mapToLong((t) -> t.duration).filter((d) -> (d >= 0)).max();
            return (max.isPresent()) ? Duration.ofNanos(max.getAsLong()) : null;
        }

        Duration getMeanDuration()
        {
            var mean = allTasks.stream().mapToLong((t) -> t.duration).filter((d) -> (d >= 0)).average();
            return (mean.isPresent()) ? Duration.ofNanos((long) mean.getAsDouble()) : null;
        }

        @Override
        public String toString()
        {
            return id + " tasks= " + remainingTasks.size() + '/' + allTasks.size(); //$NON-NLS-1$
        }

    }

    private final List<Group> allGroups;

    private final ArrayList<Group> remainingGroups;
    private int nextGroupIndex;
    private final IdentityHashMap<RunTaskGroupJob, Group> workerGroup = new IdentityHashMap<>();

    public TaskManager(List<Task> tasks)
    {
        this.allGroups = tasks.stream().collect(Collectors.groupingBy(t -> t.groupingCriterion)).entrySet().stream()
                        .map((e) -> new Group(e.getKey(), e.getValue(), getMaxParallel(e.getKey()))).toList();
        this.remainingGroups = new ArrayList<>(allGroups);
    }

    public int getMaxParallel()
    {
        return allGroups.stream().mapToInt((group) -> group.maxWorker).sum();
    }

    private int getMaxParallel(String groupingCriterion)
    {
        return 3;
    }

    public synchronized Task pollTask(RunTaskGroupJob worker, Task previousTask)
    {
        Task task = null;
        Group group = workerGroup.get(worker);
        if (group != null)
        {
            if (previousTask != null)
                group.onTaskFinished(previousTask);

            if (group.currentWorker > group.maxWorker)
            {
                group.currentWorker--;
            }
            else
            {
                task = pollGroupTask(group);
            }
        }
        if (task == null)
        {
            int iteration = 0;
            while (task == null && iteration < 2 && !remainingGroups.isEmpty())
            {
                if (nextGroupIndex == 0)
                    prioritizeGroups();

                group = remainingGroups.get(nextGroupIndex);
                nextGroupIndex++;
                if (nextGroupIndex >= remainingGroups.size())
                {
                    iteration++;
                    nextGroupIndex = 0;
                }

                if (group.currentWorker < group.maxWorker)
                {
                    group.currentWorker++;
                    workerGroup.put(worker, group);
                    task = pollGroupTask(group);
                }
            }
        }
        if (task == null)
            workerGroup.remove(worker);
        return task;
    }

    private void removeGroup(Group group)
    {
        int idx = remainingGroups.indexOf(group);
        if (idx >= 0)
        {
            remainingGroups.remove(idx);
            if (nextGroupIndex > idx)
                nextGroupIndex--;
        }
    }
    
    private void prioritizeGroups()
    {
        remainingGroups.sort(Comparator.comparingInt((group) -> -group.remainingTasks.size()));
    }

    private Task pollGroupTask(Group group)
    {
        switch (group.remainingTasks.size())
        {
            case 0:
                return null;
            case 1:
                removeGroup(group);
                //$FALL-THROUGH$
            default:
                return group.remainingTasks.pollFirst();
        }
    }

    public synchronized int onRateLimitExceeded(RunTaskGroupJob worker)
    {
        Group group = workerGroup.get(worker);
        group.maxAttempts--;
        if (group.maxWorker > 1)
            group.maxWorker = Math.max(group.currentWorker - 1, 1);
        return group.maxAttempts;
    }

    public synchronized void setGroupError(RunTaskGroupJob worker, Function<Security, String> message)
    {
        Group group = workerGroup.get(worker);
        for (var task : group.remainingTasks)
            task.status.setStatus(UpdateStatus.ERROR, message.apply(task.security));
        group.remainingTasks.clear();
    }

}

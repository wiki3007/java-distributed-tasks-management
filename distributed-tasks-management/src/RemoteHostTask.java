import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread task executed by remote host
 */
public class RemoteHostTask implements Callable<String>{
    /**
     * Global Task ID for automatic ID assignment when creating new tasks
     */
    private static int globalTaskId = 0;
    /**
     * taskId - id of task, taskPriority - priority of given task, yields to higher priority tasks, lower value = higher priority
     */
    private int taskId, taskPriority;

    private String result = "!";  // ! is beyond what the task can generate, so just assume this means empty
    /**
     * Flag that pauses the task if a task of higher priority is currently being executed
     */
    boolean lowPriorityFlag = false;
    /**
     * Current status of the task, can be "Created", "LowPriorityWait" or "Done"
     */
    String status = "Created";
    private Instant startServer;
    /**
     * Time wasted during LowPriorityWait
     */
    private long timeWasted = 0;
    /**
     * Total time taken by task to complete from the client perspective
     */
    private long timeTakenClient = 0;
    /**
     * Total time taken by task to complete from the server perspective
     */
    private long timeTakenServer = 0;


    /**
     * RemoteHostTask constructor, manual ID input
     * @param taskId ID of task
     * @param taskPriority Priority of task, higher number means higher priority

    RemoteHostTask(int taskId, int taskPriority)
    {
        this.taskId = taskId;
        this.taskPriority = taskPriority;
    }
    */

    /**
     * RemoteHostTask constructor, automatic ID input
     * @param taskPriority Priority of task, higher number means higher priority
     */
    RemoteHostTask(int taskId, int taskPriority)
    {
        //System.out.println("task started");
        this.taskId = taskId;

        this.taskPriority = taskPriority;
        System.out.println("NEW TASK WITH ID " + taskId);
        startServer = Instant.now();
    }

    /**
     * RemoteHostTask taskId getter
     * @return taskId
     */
    public int getTaskId() {
        return taskId;
    }

    /**
     * RemoteHostTask taskId setter
     * @param taskId new taskId
     */
    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    /**
     * RemoteHostTask taskPriority getter
     * @return taskPriority, higher number means higher priority
     */
    public int getTaskPriority() {
        return taskPriority;
    }

    /**
     * RemoteHostTask taskPriority setter
     * @param taskPriority taskPriority, higher number means higher priority
     */
    public void setTaskPriority(int taskPriority) {
        this.taskPriority = taskPriority;
    }

    /**
     * RemoteHostTask toString override
     * @return RemoteHostTask id and priority
     */
    @Override
    public String toString() {
        return "RemoteHostTask [" +
                "taskId: " + taskId +
                ", taskPriority: " + taskPriority +
                ']';
    }

    /**
     * Get status of thread
     * @return Can be "Started" if task started but hasn't gotten to the main loop yet, "Working" if the task is currently being executed or "LowPriorityWait" if a task of higher priority is being executed
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * Get the current result of task
     * @return The currently calculated result, if it's just "!" that means it's empty, otherwise refer to the status if it's the full result or not
     */
    public String getResult()
    {
        return result;
    }

    /**
     * Gets the time it took to complete the task, includes time wasted waiting
     * @return Time taken given in milliseconds, returns 0 if task not yet finished, otherwise the databased would be swamped with update requests
     */
    public long getServerTimeTaken()
    {
        return timeTakenServer;
    }

    /**
     * Gets the time it took to complete the task, doesn't include time wasted waiting
     * @return Time taken given in milliseconds, returns 0 if task not yet finished, otherwise the databased would be swamped with update requests
     */
    public long getClientTimeTaken()
    {
        return timeTakenClient;
    }


    /**
     * Thread locks up when lowPriorityFlag is set when Remote Host Master Thread receives
     * a task with higher priority.
     * Timeout is a minute, after which thread self checks whether to wake up or not
     * @throws InterruptedException If thread interrupted while locked
     */
    public void lowPriorityWait() throws InterruptedException {
        Instant startWait = Instant.now();
        while (lowPriorityFlag)
        {
            status = "LowPriorityWait";
            //System.out.print("");
            //wait(5000); // can't have this sleep either, but it just bricks the task. WHAT'S THE POINT OF SLEEPS AND WAITS IF THEY JUST KILL THE GODDAMN THREAD
        }
        Instant endWait = Instant.now();
        timeWasted += Duration.between(startWait, endWait).toMillis();
        status = "Working";
    }

    /**
     * Main execution function for Remote Host Task
     * @return Calculated string
     */
    @Override
    public String call() {
        Instant start = null;
        try
        {
            start = Instant.now();
            lowPriorityWait();
            status = "Working";
            Random rng = new Random();
            //System.out.println("TASK BEFORE SLEEP");
            int length = rng.nextInt(50, 150); // randomize length of result
            for (int i=0; i<length; i++)
            {
                lowPriorityWait();
                Thread.sleep(rng.nextInt(1, 25)); // simulates doing whatever calculation, this no longer breaks the thread for no apparent reason, thanks for making me look insane java
                int asciiCode;
                String charToAdd;
                do {
                    asciiCode = rng.nextInt(35, 122);
                    charToAdd = String.valueOf((char) asciiCode);
                }
                while (asciiCode == 92); // get rid of this stupid \ slash thingy, it breaks quoatation marks and probably a million other things I haven't notice, but mainly quotation marks


                if (!result.equals("!")) // again, ! means empty since its ASCII code is outside the rng scope
                {
                    result = result.concat(charToAdd); // random ASCII character form # to z
                }
                else result = charToAdd;
                //System.out.println(result);
            }
            lowPriorityWait();
            //System.out.println(result);
            return result;
        }
        catch (InterruptedException interrupted)
        {
            result += "!----!Task interrupted!----!";
            return result;
        }
        catch (CancellationException cancelled)
        {
            result += "!----!Task cancelled!----!";
            return result;
        }
        finally {
            Instant end = Instant.now();
            assert start != null;
            timeTakenClient = Duration.between(start, end).toMillis() - timeWasted;
            timeTakenServer = Duration.between(startServer, end).toMillis();
            System.out.println();
            status = "Done";
            //notifyAll();
            //System.out.println("TASK REPORTING " + taskId + " " + status + " " + result);
            System.out.println("TASKEND " + taskId);
        }

    }
}

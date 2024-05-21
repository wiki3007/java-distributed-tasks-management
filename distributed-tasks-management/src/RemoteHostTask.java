import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

/**
 * Thread task executed by remote host
 */
public class RemoteHostTask implements Callable<String> {
    /**
     * Global Task ID for automatic ID assignment when creating new tasks
     */
    private static int globalTaskId = 0;
    /**
     * taskId - id of task, taskPriority - priority of given task, yields to higher priority tasks
     */
    private int taskId, taskPriority;

    private String result = "";
    boolean lowPriorityFlag = false;

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
    RemoteHostTask(int taskPriority)
    {
        this.taskId = globalTaskId++;
        this.taskPriority = taskPriority;
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
     * Thread locks up when lowPriorityFlag is set when Remote Host Master Thread receives
     * a task with higher priority.
     * Timeout is a minute, after which thread self checks whether to wake up or not
     * @throws InterruptedException If thread interrupted while locked
     */
    public void lowPriorityWait() throws InterruptedException {
        while (lowPriorityFlag)
        {
            wait(60000);
        }
    }

    /**
     * Main execution function for Remote Host Task
     * @return Calculated string
     */
    @Override
    public String call() {
        try
        {
            lowPriorityWait();
            Random rng = new Random();
            int length = rng.nextInt(50, 150); // randomize length of result
            for (int i=0; i<length; i++)
            {
                lowPriorityWait();
                Thread.sleep(rng.nextInt(1, 10) * 1000L); // simulates doing whatever calculation
                result += rng.nextInt(35, 122); // random ASCII character form # to z
            }
            lowPriorityWait();
            return result;
        }
        catch (InterruptedException interrupted)
        {
            result += "+----+Task interrupted+----+";
            return result;
        }
        catch (CancellationException cancelled)
        {
            result += "+----+Task cancelled+----+";
            return result;
        }
        finally {
            notifyAll();
        }
    }
}

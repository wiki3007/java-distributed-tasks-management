import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread task executed by remote host
 */
public class RemoteHostTask implements Callable<String>{
    //private static final long serialversionUID = 15643123L;
    private static ReentrantLock lock = new ReentrantLock();
    /**
     * Global Task ID for automatic ID assignment when creating new tasks
     */
    private static int globalTaskId = 0;
    /**
     * taskId - id of task, taskPriority - priority of given task, yields to higher priority tasks
     */
    private int taskId, taskPriority;

    private String result = "!";  // ! is beyond what the task can generate, so just assume this means empty
    boolean lowPriorityFlag = false;
    String status = "Created";

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
        //System.out.println("tast started");
        //lock.lock();
        try
        {
            this.taskId = taskId;
        }
        finally {
            //lock.unlock();
        }
        this.taskPriority = taskPriority;
        System.out.println("NEW TASK WITH ID " + taskId);
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

    public String getStatus()
    {
        return status;
    }

    public String getResult()
    {
        return result;
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
            status = "LowPriorityWait";
            //System.out.print("");
            //wait(5000); // can't have this sleep either, but it just bricks the task. WHAT'S THE POINT OF SLEEPS AND WAITS IF THEY JUST KILL THE GODDAMN THREAD
        }
        status = "Working";
    }

    /**
     * Main execution function for Remote Host Task
     * @return Calculated string
     */
    @Override
    public String call() {
        try
        {
            status = "Working";
            lowPriorityWait();
            Random rng = new Random();
            //System.out.println("TASK BEFORE SLEEP");
            int length = rng.nextInt(50, 150); // randomize length of result
            for (int i=0; i<length; i++)
            {
                lowPriorityWait();
                Thread.sleep(rng.nextInt(1, 25)); // simulates doing whatever calculation, this breaks the loop too, because why not
                int asciiCode;
                String charToAdd;
                do {
                    asciiCode = rng.nextInt(35, 122);
                    charToAdd = String.valueOf((char) asciiCode);
                }
                while (asciiCode == 92); // get rid of this stupid \ slash thingy, it breaks quoatation marks and probably a million other things I haven't notice, but mainly quotation marks


                if (!result.equals("!")) // again, ! means empty since it's ASCII 33
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
            status = "Done";
            //notifyAll();
            //System.out.println("TASK REPORTING " + taskId + " " + status + " " + result);
            System.out.println("TASKEND " + taskId);
        }

    }
}

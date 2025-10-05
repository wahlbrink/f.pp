package name.abuchen.portfolio.ui.jobs.priceupdate;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import name.abuchen.portfolio.online.AuthenticationExpiredException;
import name.abuchen.portfolio.online.FeedConfigurationException;
import name.abuchen.portfolio.online.RateLimitExceededException;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

/* package */ final class RunTaskGroupJob extends Job
{
    private final TaskManager tasks;
    private final PriceUpdateRequest request;

    RunTaskGroupJob(String name, TaskManager tasks, PriceUpdateRequest request)
    {
        super(name);
        this.tasks = tasks;
        this.request = request;

        // setRule(new GroupingCriterionSchedulingRule(name));
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        Task task = null;
        boolean retry = false;
        while (true)
        {
            if (retry)
                retry = false;
            else
                task = tasks.pollTask(this, task);

            if (task == null)
                return Status.OK_STATUS;

            task.status.setStatus(UpdateStatus.LOADING, null);

            try
            {
                long startTime = System.nanoTime();
                UpdateStatus status;
                try
                {
                    status = task.update();
                }
                finally
                {
                    task.duration = System.nanoTime() - startTime;
                }

                task.status.setStatus(status, null);

                task.security.getEphemeralData().touchFeedLastUpdate();

                if (status == UpdateStatus.MODIFIED)
                    request.markDirty();
            }
            catch (AuthenticationExpiredException e)
            {
                task.status.setStatus(UpdateStatus.ERROR, Messages.MsgAuthenticationExpired);
                tasks.setGroupError(this, (security) -> Messages.MsgAuthenticationExpired);
            }
            catch (FeedConfigurationException e)
            {
                task.security.getEphemeralData().setHasPermanentError();
                task.status.setStatus(UpdateStatus.ERROR, e.getMessage());

                PortfolioPlugin.log(MessageFormat.format(Messages.MsgInstrumentWithConfigurationIssue,
                                task.security.getName()), e);
            }
            catch (RateLimitExceededException e)
            {
                int remainingAttemts = tasks.onRateLimitExceeded(this);
                if (remainingAttemts >= 0 && e.getRetryAfter().isPositive())
                {
                    retry = true;
                    task.status.setStatus(UpdateStatus.WAITING, MessageFormat.format(
                                    Messages.MsgRateLimitExceededAndRetrying, task.security.getName(),
                                    remainingAttemts));

                    try
                    {
                        Thread.sleep(e.getRetryAfter().toMillis());
                    }
                    catch (InterruptedException ie)
                    {
                        Thread.currentThread().interrupt();
                    }
                }
                else
                {
                    task.status.setStatus(UpdateStatus.ERROR,
                                    MessageFormat.format(Messages.MsgRateLimitExceeded, task.security.getName()));
                    tasks.setGroupError(this, (security) -> MessageFormat.format(Messages.MsgRateLimitExceeded,
                                    security.getName()));
                }
            }
            catch (Exception e)
            {
                task.status.setStatus(UpdateStatus.ERROR, e.getMessage());
                PortfolioPlugin.log(e);
            }
        }
    }

}

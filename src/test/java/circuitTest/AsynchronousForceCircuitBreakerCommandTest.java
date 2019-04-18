package circuitTest;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Test;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;

public class AsynchronousForceCircuitBreakerCommandTest {
	private final static int ASYNC_COMMAND_COUNT = 10;
	private final static int RATIO = 3;
	private final static int REQUEST_VOLUME_THRESHOLD = 5;
	private final static int ERROR_THRESHOLD_PERCENTAGE = 50;
	private final static int INTERVAL_IN_MILLISECONDS = 1000;
	private final static int ROLLING_WINDOW_IN_MILLISECONDS = INTERVAL_IN_MILLISECONDS * ASYNC_COMMAND_COUNT;

	@Test
	public void testForceOpen() throws InterruptedException {

		// Here are creating 10 async Hystrix Commands with 2/3rd failing and 1/3rd
		// passing
		// When creating we have set CircuitBreakerErrorThresholdPercentage is 50
		// meaning
		// 50% of the request should fail for the circuit to open
		// Also When creating we have set CircuitBreakerRequestVolumeThreshold to 5
		// meaning
		// even during commands from 0-2 2/3rd ie (2) fail which is > 50% it will still
		// not open the circuit. as the threshold is 5
		// Concluding that both these conditions are to be met for the circuit to open.
		//
		// In plain English
		// "When number of request in the given Rolling Window goes over 5 and the Error
		// Percentage is over 50% open the circuit"
		//
		for (int i = 0; i < ASYNC_COMMAND_COUNT; ++i) {
			// Here if pass true means run() method will pass else it will fail in run
			// method
			// 2/3rd will fail and 1/3rd will pass
			ForceCircuitAsyncCommand f = new ForceCircuitAsyncCommand("CMD" + i, i % RATIO == 0 ? true : false);

			// Execute goes to fall back directly hence should return FALSE
			// assertEquals(Boolean.FALSE, f3.execute());
			Future<Boolean> fs = f.queue();

			// Even though the run method passed the rolling window period is
			// not finished
			// hence circuit will still be OPEN
			// assertEquals(Boolean.TRUE, f3.isCircuitBreakerOpen());

			try {
				fs.get();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		ForceCircuitAsyncCommand f = new ForceCircuitAsyncCommand("CMD_FINAL", true);
		// Just to make sure more than threshold request have completed
		while (f.getMetrics().getHealthCounts().getErrorPercentage() < ERROR_THRESHOLD_PERCENTAGE) {
			Thread.sleep(INTERVAL_IN_MILLISECONDS);
		}
		
		// both conditions would have been completed hence the circuit should be open.
		assertEquals(Boolean.TRUE, f.isCircuitBreakerOpen());

		// We let the rolling window period elapse
		Thread.sleep(ROLLING_WINDOW_IN_MILLISECONDS);

		// Here we are passing true means run() method will pass
		// As time has elapsed, execute goes to run hence should return TRUE
		assertEquals(Boolean.TRUE, f.execute());
		// As the run method passed the rolling window period is
		// finished
		// hence circuit will still be CLOSED
		assertEquals(Boolean.FALSE, f.isCircuitBreakerOpen());

	}

	private class ForceCircuitAsyncCommand extends HystrixCommand<Boolean> {

		private final boolean doFail;
		private final String strObjName;

		public ForceCircuitAsyncCommand(String strObjName, boolean doFail) {
			super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("TestGroup"))
					.andCommandKey(HystrixCommandKey.Factory.asKey("TestKey"))
					.andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("TestPool"))
					.andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
							.withCircuitBreakerRequestVolumeThreshold(REQUEST_VOLUME_THRESHOLD)
							.withMetricsRollingStatisticalWindowInMilliseconds(ROLLING_WINDOW_IN_MILLISECONDS)
							.withMetricsRollingPercentileWindowInMilliseconds(ROLLING_WINDOW_IN_MILLISECONDS)
							.withCircuitBreakerErrorThresholdPercentage(ERROR_THRESHOLD_PERCENTAGE)
							.withMetricsHealthSnapshotIntervalInMilliseconds(INTERVAL_IN_MILLISECONDS)));

			this.doFail = doFail;
			this.strObjName = strObjName;
		}

		@Override
		public Boolean run() {
			System.out.println("Run[" + strObjName + "]Will[" + (doFail ? "pass" : "fail") + "]");
			// Actual call/execution for which circuit is to be done goes in this method
			if (!doFail) {
				try {
					Thread.sleep(INTERVAL_IN_MILLISECONDS*2);

				} catch (InterruptedException e) {
					System.err.println(e.getMessage());
				}
			}
			return Boolean.TRUE;
		}

		@Override
		public Boolean getFallback() {
			System.out.println("Fallback[" + strObjName + "]isOpen[" + isCircuitBreakerOpen() + "]Heatlh["
					+ getMetrics().getHealthCounts());
			// In this method we handle the fall back case
			// May be return cached information or default value etc
			return Boolean.FALSE;
		}
	}

}
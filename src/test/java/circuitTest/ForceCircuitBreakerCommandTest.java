package circuitTest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;

public class ForceCircuitBreakerCommandTest {

	private final static int REQUEST_VOLUME_THRESHOLD = 1;
	private final static int ERROR_THRESHOLD_PERCENTAGE = 0;
	private final static int INTERVAL_IN_MILLISECONDS = 100;
	private final static int ROLLING_WINDOW_IN_MILLISECONDS = 1000;

	@Test
	public void testForceOpen() throws InterruptedException {
		// Using { } to keep the FakeCommand object scope limited
		{
			// Here we are passing true means run() method will PASS
			ForceCircuitSyncCommand f1 = new ForceCircuitSyncCommand(true);
			// Execute goes to run hence should return TRUE
			assertEquals(Boolean.TRUE, f1.execute());
			// As run method was successful circuit will remain CLOSE
			assertEquals(Boolean.FALSE, f1.isCircuitBreakerOpen());
		}

		{
			// Here we are passing false means run() method will FAIL
			ForceCircuitSyncCommand f2 = new ForceCircuitSyncCommand(false);
			// Execute goes to fall back due to run failure hence should return FALSE
			assertEquals(Boolean.FALSE, f2.execute());
			// As run method failed circuit will OPEN
			assertEquals(Boolean.TRUE, f2.isCircuitBreakerOpen());
		}

		{
			// Here we are passing true means run() method will PASS
			ForceCircuitSyncCommand f3 = new ForceCircuitSyncCommand(true);
			// Execute goes to fall back directly hence should return FALSE
			assertEquals(Boolean.FALSE, f3.execute());
			// Even though the run method passed the rolling window period is
			// not finished
			// hence circuit will still be OPEN
			assertEquals(Boolean.TRUE, f3.isCircuitBreakerOpen());
		}

		// We let the time elapse
		Thread.sleep(ROLLING_WINDOW_IN_MILLISECONDS * 10);

		{
			// Here we are passing true means run() method will PASS
			ForceCircuitSyncCommand f4 = new ForceCircuitSyncCommand(true);
			// As time has elapsed, execute goes to run hence should return TRUE
			assertEquals(Boolean.TRUE, f4.execute());
			// As the run method passed the rolling window period is
			// finished
			// hence circuit will still be CLOSED
			assertEquals(Boolean.FALSE, f4.isCircuitBreakerOpen());
		}

	}

	private class ForceCircuitSyncCommand extends HystrixCommand<Boolean> {

		private final boolean doFail;

		public ForceCircuitSyncCommand(boolean doFail) {
			super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("TestGroup"))
					.andCommandKey(HystrixCommandKey.Factory.asKey("TestKey"))
					.andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("TestPool"))
					.andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
							.withCircuitBreakerRequestVolumeThreshold(REQUEST_VOLUME_THRESHOLD)
							.withMetricsRollingStatisticalWindowInMilliseconds(ROLLING_WINDOW_IN_MILLISECONDS)
							.withMetricsRollingPercentileWindowInMilliseconds(INTERVAL_IN_MILLISECONDS)
							.withCircuitBreakerErrorThresholdPercentage(ERROR_THRESHOLD_PERCENTAGE)
							.withMetricsHealthSnapshotIntervalInMilliseconds(INTERVAL_IN_MILLISECONDS)));

			this.doFail = doFail;
		}

		@Override
		public Boolean run() {
			System.out.println("Run");

			// Actual call/execution for which circuit is to be done goes in this method
			if (!doFail) {
				try {
					Thread.sleep(INTERVAL_IN_MILLISECONDS * 20);
				} catch (InterruptedException e) {
					System.err.println(e.getMessage());
				}
			}
			return Boolean.TRUE;
		}

		@Override
		public Boolean getFallback() {
			System.out.println("Fallback");

			// In this method we handle the fall back case
			// May be return cached information or default value etc
			return Boolean.FALSE;
		}
	}

}
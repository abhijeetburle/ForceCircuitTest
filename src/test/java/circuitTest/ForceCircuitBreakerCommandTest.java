package circuitTest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;

public class ForceCircuitBreakerCommandTest {

	@Test
	public void testForceOpen() throws InterruptedException {
		// Using { } to keep the FakeCommand object scope limited
		{
			// Here we are passing false means run() method will NOT FAIL
			ForceCircuitCommand f1 = new ForceCircuitCommand(false);
			// Execute goes to run hence should return TRUE
			assertEquals(Boolean.TRUE, f1.execute());
			// As run method was successful circuit will remain CLOSE
			assertEquals(Boolean.FALSE, f1.isCircuitBreakerOpen());
		}

		{
			// Here we are passing true means run() method will fail
			ForceCircuitCommand f2 = new ForceCircuitCommand(true);
			// Execute goes to fall back due to run failure hence should return FALSE
			assertEquals(Boolean.FALSE, f2.execute());
			// As run method failed circuit will OPEN
			assertEquals(Boolean.TRUE, f2.isCircuitBreakerOpen());
		}

		{
			// Here we are passing true means run() method will pass
			ForceCircuitCommand f3 = new ForceCircuitCommand(false);
			// Execute goes to fall back directly hence should return FALSE
			assertEquals(Boolean.FALSE, f3.execute());
			// Even though the run method passed the rolling statistical window period is
			// not finished
			// hence circuit will still be OPEN
			assertEquals(Boolean.TRUE, f3.isCircuitBreakerOpen());
		}

	}

	private class ForceCircuitCommand extends HystrixCommand<Boolean> {

		private final boolean doFail;

		public ForceCircuitCommand(boolean doFail) {
			super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("TestGroup"))
					.andCommandKey(HystrixCommandKey.Factory.asKey("TestKey"))
					.andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("TestPool")).andCommandPropertiesDefaults(
							HystrixCommandProperties.Setter().withCircuitBreakerRequestVolumeThreshold(1)
									.withMetricsRollingStatisticalWindowInMilliseconds(1000)
									.withCircuitBreakerErrorThresholdPercentage(0)
									.withMetricsHealthSnapshotIntervalInMilliseconds(100)));

			this.doFail = doFail;
		}

		@Override
		public Boolean run() {
			if (doFail) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return Boolean.TRUE;
		}

		@Override
		public Boolean getFallback() {
			return Boolean.FALSE;
		}
	}

}
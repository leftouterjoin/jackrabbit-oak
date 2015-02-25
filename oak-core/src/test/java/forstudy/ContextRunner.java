package forstudy;

import java.lang.reflect.Method;

import org.junit.internal.runners.InitializationError;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.notification.RunNotifier;
import java.lang.reflect.Method;

public class ContextRunner extends JUnit4ClassRunner {

	public ContextRunner(Class<?> klass) throws InitializationError {
		super(klass);
	}

	@Override
	protected void invokeTestMethod(Method method, RunNotifier notifier) {
		TestContext.start(method);
		super.invokeTestMethod(method, notifier);
		TestContext.end();
	}

	public static class TestContext {

		private static final ThreadLocal<TestContext> threadLocal = new ThreadLocal<TestContext>();

		public static void start(Method m) {
			threadLocal.set(new TestContext(m));
		}

		public static void end() {
			threadLocal.remove();
		}

		public static TestContext getContext() {
			return threadLocal.get();
		}

		private Method testMethod;

		private TestContext(Method testMethod) {
			this.testMethod = testMethod;
		}

		public Method getTestMethod() {
			return testMethod;
		}
	}
}
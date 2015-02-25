package forstudy;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;

import org.junit.Test;

public class TestcaseStats {

	@Test
	public void statsTestcaseAnnotation() throws Exception {
		statsAllClass(System.out);
	}

	private void statsAllClass(final OutputStream out) {
		try {
			final PrintWriter pw = new PrintWriter(out);
			new ClassFinder2() {
				@Override
				public void found(String className) {
					try {
						Class<?> clazz = Class.forName(className);
						Method[] methods = clazz.getDeclaredMethods();
						for (Method m : methods) {
							TestSpec testSpec = m.getAnnotation(TestSpec.class);
							if (testSpec != null)
								statsTestSpec(pw, m, testSpec);
						}
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}.traverse(new File("./target/test-classes"));
		} catch (Throwable e) {
			System.err.println(e.toString());
			throw new RuntimeException(e);
		}
	}

	void statsTestSpec(PrintWriter pw, Method m, TestSpec testSpec) {
		pw.printf("テストケースクラス: %s\n", m.getDeclaringClass().getName());
		pw.printf("\tテストケースメソッド: %s\n", m.getName());
		for (int i = 0; i < testSpec.objective().length; i++) {
			pw.printf("\t\t目的[%2d]: %s\n", i, testSpec.objective()[i]);
		}
		for (int i = 0; i < testSpec.confirmatins().length; i++) {
			TestSpec.Confirmatin c = testSpec.confirmatins()[i];
			pw.printf("\t\t\t操作[%2d]: %s\n", i, c.operation());
			for (String s : c.expected())
				pw.printf("\t\t\t\t期待値: %s\n", s);
		}
		pw.flush();
	}
}

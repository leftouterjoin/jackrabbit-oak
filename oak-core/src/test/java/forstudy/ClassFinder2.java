package forstudy;

import java.io.File;
import java.util.Stack;

public class ClassFinder2 {

	// Call back me.
	public void found(String className) {
	}

	public void traverse(File root) {
		// 再帰処理代わりのスタック
		Stack<File> processeeStack = new Stack<File>();
		processeeStack.push(root);

		while (!processeeStack.isEmpty()) {
			File current = processeeStack.pop();
			if (current.isDirectory()) {
				File[] files = current.listFiles();
				for (File f : files)
					processeeStack.push(f);
			} else {
				if (current.getName().endsWith(".class")) {
					String fileName = current.getAbsolutePath().substring(
							1 + root.getAbsolutePath().length());
					String className = fileName2className(fileName);
					found(className);
				}
			}
		}
	}

	private String fileName2className(String fileName) {
		int dot = fileName.lastIndexOf(".");
		if (dot > 0) {
			return fileName.substring(0, dot).replaceAll("\\\\", ".");
		}
		return fileName;
	}
}

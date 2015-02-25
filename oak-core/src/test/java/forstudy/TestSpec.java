package forstudy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * テスト仕様を表すアノテーション<br>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TestSpec {
	public @interface Confirmatin {
		/**
		 * 操作を設定します。<br>
		 *
		 * @return
		 */
		String operation();

		/**
		 * 期待値を設定します。<br>
		 *
		 * @return
		 */
		String[] expected() default {};
	}

	/**
	 * テストケースの種別の列挙子<br>
	 */
	public enum Type {
		/**
		 * 正常系
		 */
		NORMAL,
		/**
		 * 異常系(テスト対象から例外が投げられる)
		 */
		ABNORMAL, ;
	}

	/**
	 * テストケースのIDを設定します。<br>
	 *
	 * @return
	 */
	String caseId() default "";

	/**
	 * テストケースの種別を設定します。<br>
	 *
	 * @see {@link Type Type}
	 * @return
	 */
	Type type() default Type.NORMAL;

	/**
	 * テストケースのタイトルを設定します。<br>
	 *
	 * @return
	 */
	String title() default "";

	/**
	 * テストケースの目的を設定します。<br>
	 *
	 * @return
	 */
	String[] objective();

	/**
	 * テストケースの前提条件を設定します。<br>
	 *
	 * @return
	 */
	String[] precondition() default {};

	/**
	 * テストケースの事後条件を設定します。<br>
	 *
	 * @return
	 */
	String[] postcondition() default {};

	/**
	 * テストケースの確認項目を設定します。<br>
	 *
	 * @return
	 */
	Confirmatin[] confirmatins();

	/**
	 * テストケースの備考を設定します。<br>
	 *
	 * @return
	 */
	String remarks() default "";

	/**
	 * テストケースの作成者を設定します。<br>
	 *
	 * @return
	 */
	String author() default "";

	/**
	 * テストケースの作成日を設定します。<br>
	 *
	 * @return
	 */
	String createDate() default "";
}

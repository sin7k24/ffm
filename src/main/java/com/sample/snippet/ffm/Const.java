package com.sample.snippet.ffm;

import java.util.Arrays;
import java.util.List;

/**
 * ffmパッケージのデフォルト定義値を設定するクラスです。
 * <p/>
 *
 * 各定数フィールドを設定することでデフォルト動作を変更可能です。
 */
public class Const {

	/** 列デリミタです。空白「 」、タブ「\t」、カンマ「,」を指定します。 デフォルト：半角空白 */
	public static final String DEFAULT_DELIMITER = " ";

	/** ソートに使用するキー列番号を0始まりで指定します。デフォルト：1 */
	public static final int DEFAULT_SORT_KEY = 1;

	/** ジョインに使用するキー列番号を0始まりで指定します。デフォルト：1  */
	public static final int DEFAULT_JOIN_KEY = 1;

	/** ジョイン時に省略する左表キー列番号を0始まりで指定します。 */
	public static final List<Integer> DEFAULT_OMIT_LEFT_JOIN_KEYS = Arrays.asList();

	/** ジョイン時に省略する右表キー列番号を0始まりで指定します。 */
	public static final List<Integer> DEFAULT_OMIT_RIGHT_JOIN_KEYS = Arrays.asList(0, 1);

	/** {@link Sorter#sortToFile()}で出力されるテンポラリファイルのプレフィクスです。 */
	public static final String SORTED_FILE_PREFIX = "sort";

	/** {@link Sorter#sortToFile()}で出力されるテンポラリファイルのサフィックスです。 */
	public static final String SORTED_FILE_SUFFIX = ".tmp";
}

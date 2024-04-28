package com.sample.snippet.ffm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * ファイルの行ソートを行うクラスです。<p/>
 *
 *
 */
public class Sorter implements Callable<File> {

	/** ソート対象ファイルパス */
	private String filePath;

	/** ソート比較に使用する列番号 */
	private int sortKey;

	/** 列分割デリミタ */
	private String delimiter;

	/** ソートフィルタ */
	private LineFilter lineFilter;

	/**
	 * 以下の条件でインスタンスを生成します。
	 *
	 * <ol>
	 * <li>ソート対象ファイル：引数filePath</li>
	 * <li>ソートキー列番号：Const.DEFAULT_SORT_KEY</li>
	 * <li>列デリミタ：Const.DEFAULT_DELIMITER</li>
	 * </ol>
	 *
	 * @param filePath ソート対象ファイル
	 */
	public Sorter(String filePath) {
		this(filePath, Const.DEFAULT_SORT_KEY, Const.DEFAULT_DELIMITER);
	}

	/**
	 * 以下の条件でインスタンスを生成します。
	 *
	 * <ol>
	 * <li>ソート対象ファイル：引数filePath</li>
	 * <li>ソートキー列番号：引数sortKey</li>
	 * <li>列デリミタ：Const.DEFAULT_DELIMITER</li>
	 * </ol>
	 *
	 * @param filePath ソート対象ファイル
	 * @param sortKey ソートキーにする列番号
	 */
	public Sorter(String filePath, int sortKey) {
		this(filePath, sortKey, Const.DEFAULT_DELIMITER);
	}

	/**
	 * 以下の条件でインスタンスを生成します。
	 *
	 * <ol>
	 * <li>ソート対象ファイル：引数filePath</li>
	 * <li>ソートキー列番号：引数sortKey</li>
	 * <li>列デリミタ：引数delimiter</li>
	 * </ol>
	 *
	 * @param filePath ソート対象ファイル
	 * @param sortKey ソートキーにする列番号
	 * @param delimiter 列を分割する区切り文字
	 */
	public Sorter(String filePath, int sortKey, String delimiter) {
		this.filePath = filePath;
		this.sortKey = sortKey;
		this.delimiter = delimiter;
		this.lineFilter = new LineFilter();
	}

	/**
	 * スレッド実行エントリポイントです。<p/>
	 *
	 * sortToFile()メソッドをスレッド上で実行し、ソート結果ファイルを返却します。
	 *
	 * @return ソート後のFileインスタンス
	 */
	@Override
	public File call() throws Exception {
		return this.sortToFile();
	}

	/**
	 * ソートフィルタ追加メソッドです。<p/>
	 *
	 * 下記のフィルタ設定の場合、「0カラム値が100、1カラム値が2000以上」
	 * の条件を満たさない行はソート対象から外れます。
	 *
	 * <pre>
	 * new Sorter("temp.tmp")
	 *     .filter(0, EQ, 100)
	 *     .filter(1, GTE, 2000)
	 *     .sort();
	 * </pre>
	 *
	 * 比較オペレータは下記クラスを参照<p/>
	 *
	 * {@link Operator}
	 *
	 * @param column 比較対象カラム
	 * @param operand 比較オペレータ
	 * @param value 比較値
	 * @return thisインスタンス
	 */
	public Sorter filter(int column, Operator operand, Object value) {
		this.lineFilter.filter(column, operand, value);

		return this;
	}

	/**
	 * ソート対象ファイルを読み込み、List<String>に追加します。<p/>
	 *
	 * List完成後、ファイルはクローズされます。
	 * {@link Sorter#filter(int, Operator, Object)}によってソートフィルタが設定されている場合、
	 * このフィルタ条件を満たさない行はListに追加されません。
	 *
	 * @return 全行、もしくはfilter条件を満たした行を収めたListインスタンス
	 * @throws IOException ソート対象ファイル読み込み失敗時
	 */
	private List<String> readFile() throws IOException {
		List<String> list = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(this.filePath));

		String line = null;
		while ((line = br.readLine()) != null) {
			if (!this.lineFilter.filter(line)) {
				continue;
			}

			list.add(line);
		}
		br.close();

		return list;
	}

	/**
	 * ソート処理を開始します。<p/>
	 *
	 * ファイルから読み込んだ行リストを修正マージソートメソッドに処理委譲します。
	 *  {@link Sorter#sortByComparator(List)}
	 *
	 * @return ソート後リスト
	 * @throws IOException ソート対象ファイル読み込み失敗時
	 */
	public List<String> sort() throws IOException {
		List<String> list = readFile();
		list = sortByComparator(list);

		return list;
	}

	/**
	 * ソート処理後、結果リストを一時ファイルに書き出します。<p/>
	 *
	 * <ul>
	 * <li>一時ファイルのプレフィクス：Const.SORTED_FILE_PREFIX
	 * <li>一時ファイルのサフィックス：Const.SORTED_FILE_SUFFIX
	 * </ul>
	 *
	 * ファイル出力後、リストをclear。一時ファイルに対してdeleteOnExit()を呼び出します。
	 *
	 * @return ソート結果一時ファイル
	 * @throws IOException ソート対象ファイル読み込み失敗時
	 */
	public File sortToFile() throws IOException {
		List<String> list = sort();
		File retFile = File.createTempFile(Const.SORTED_FILE_PREFIX, Const.SORTED_FILE_SUFFIX);

		BufferedWriter bw = new BufferedWriter(new FileWriter(retFile));
		for (String line : list) {
			bw.write(line);
			bw.newLine();
		}
		bw.close();

		retFile.deleteOnExit();
		list.clear();

		return retFile;
	}

	/**
	 * 引数listに対して修正マージソートを行います。<p/>
	 *
	 * 列分割デリミタは{@link #delimiter}が、
	 * 比較に使用する列番号は{@link #sortKey}が使用されます。
	 *
	 * @param list ソート対象リスト
	 * @return ソート後リスト
	 */
	private List<String> sortByComparator(List<String> list) {
		list.sort(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				// 必要最小限のsplitにすることで処理速度を上げる
				o1 = o1.split(delimiter, sortKey + 2)[sortKey];
				o2 = o2.split(delimiter, sortKey + 2)[sortKey];

				int diff = o1.compareTo(o2);

				return diff;
			}
		});
		return list;
	}
}

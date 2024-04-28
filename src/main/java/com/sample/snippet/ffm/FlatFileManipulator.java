package com.sample.snippet.ffm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 表ファイルのソート、ジョイン、検索を行うフロントエンドです。<p/>
 *
 * <pre>
 * FlatFileManipulator facade = new FlatFileManipulator("左表ファイルパス", "右表ファイルパス");
 *
 * // 左右表のソート時に条件を追加（ジョイン結果によるメモリ枯渇対策）
 * facade.leftSortFilter(6, EQ, "東京都").leftSortFilter(1, GTE, "1250053");
 * facade.rightSortFilter(6, EQ, "東京都");
 *
 * // ソート実行
 * facade.sort();
 *
 * // ジョイン結果全件取得
 * List<String> joinedList = facade.join();
 *
 * // ジョイン結果の検索フィルタ設定
 * facade.searchFilter(1, EQ, "1250053");
 *
 * // ジョイン結果に対して検索
 * List<String> resultList = facade.search();
 * </pre>
 *
 * <strong>留意事項</strong><p/>
 *
 * ジョイン結果がJavaヒープサイズを超えるような巨大データになる場合、OutOfMemoryが発生してJVMが停止するか、
 * またはOOMが発生しなくても頻繁にGCが発生し、処理速度が急激に低下してしまいます。
 * このようなケースでは以下のJVMオプションを指定してください。
 *
 * <pre>
 * -XX:+UseG1GC -XX:+UseStringDeduplication
 * </pre>
 *
 * UseStringDeduplicationを有効にすることで、参照されなくなった文字列がガベージコレクション対象に入るようになり、
 * G1による並列マークスイープGCを有効にすることで、FullGCが発生する頻度を低下させることが出来ます。
 */
public class FlatFileManipulator {

	/** 左表ファイルインスタンス */
	private File leftFile;

	/** 右表ファイルインスタンス */
	private File rightFile;

	/** 左表用ソート */
	private Sorter leftSorter;

	/** 右表用ソート */
	private Sorter rightSorter;

	/** ソートされた左表ファイル */
	private File leftSorted;

	/** ソートされた右表ファイル */
	private File rightSorted;

	/** ジョインされた行リスト */
	private List<String> joinedList;

	/** ジョインされた行リストに対する検索フィルタ */
	private LineFilter searchFilter;

	/**
	 * 以下の条件でインスタンスを生成します。<p/>
	 *
	 * <ol>
	 * <li>左表ファイルパス：引数leftFilePath</li>
	 * <li>右表ファイルパス：引数rightFilePath</li>
	 * </ol>
	 *
	 * @param leftFilePath 左表ファイルパス
	 * @param rightFilePath 右表ファイルパス
	 */
	public FlatFileManipulator(String leftFilePath, String rightFilePath) {
		this.leftFile = new File(leftFilePath);
		this.rightFile = new File(rightFilePath);

		// ソーター
		this.leftSorter = new Sorter(leftFilePath);
		this.rightSorter = new Sorter(rightFilePath);

		this.searchFilter = new LineFilter();
	}

	/**
	 * 左ソート用フィルタを追加します。<p/>
	 *
	 * @param column 比較対象列番号
	 * @param operator 比較演算子
	 * @param value 比較する値
	 * @return thisインスタンス
	 */
	public FlatFileManipulator leftSortFilter(
			int column,
			Operator operator,
			Object value)
	{
		this.leftSorter.filter(column, operator, value);

		return this;
	}

	/**
	 * 右ソート用フィルタを追加します。<p/>
	 *
	 * @param column 比較対象列番号
	 * @param operator 比較演算子
	 * @param value 比較する値
	 * @return thisインスタンス
	 */
	public FlatFileManipulator rightSortFilter(
			int column,
			Operator operator,
			Object value)
	{
		this.rightSorter.filter(column, operator, value);

		return this;
	}

	/**
	 * 左右ファイルをソートします。<p/>
	 *
	 * 1ファイル1スレッドを割り当てて並列にソート処理を行います。
	 *
	 * @throws IOException ソートファイル読み込み失敗時
	 * @throws InterruptedException ソートスレッドに割り込みが発生した時
	 * @throws ExecutionException 中断したスレッドにアクセスした時
	 */
	public void sort() throws IOException,
			InterruptedException,
			ExecutionException
	{
		// 左右ファイル分の2スレッドプール作成
		ExecutorService threadPool = Executors.newFixedThreadPool(2);

		// ソートスレッド起動
		Future<File> left = threadPool.submit(this.leftSorter);
		Future<File> right = threadPool.submit(this.rightSorter);

		// ソートスレッド処理待機後、結果取得
		this.leftSorted = left.get();
		this.rightSorted = right.get();

		threadPool.shutdown();
	}

	/**
	 * 左右ファイルのソート是非を選択してソートします。<p/>
	 *
	 * sort(true, true)はsort()と同義です。
	 *
	 * @param left true：左ファイルをソートする
	 * @param right true：右ファイルをソートする
	 * @throws IOException ソートファイル読み込み失敗時
	 * @throws InterruptedException ソートスレッドに割り込みが発生した時
	 * @throws ExecutionException 中断したスレッドにアクセスした時
	 */
	public void sort(boolean left, boolean right) throws IOException,
			InterruptedException, ExecutionException
	{
		if (left && right) {
			sort();
			return;
		}

		if (left) {
			this.leftSorted = this.leftSorter.sortToFile();
		} else {
			this.leftSorted = this.leftFile;
		}

		if (right) {
			this.rightSorted = this.rightSorter.sortToFile();
		} else {
			this.rightSorted = this.rightFile;
		}
	}

	/**
	 * 左右ファイルをマージジョインします。<p/>
	 *
	 * @return ジョインされた行リスト
	 * @throws IOException ジョインファイル読み込み失敗時
	 */
	public List<String> join() throws IOException {

		MergeJoin joiner = new MergeJoin(this.leftSorted, this.rightSorted);
		this.joinedList = joiner.join();

		return joinedList;
	}

	/**
	 * ジョインされた行リストに対する検索フィルタを追加します。<p/>
	 *
	 * @param column 比較対象列番号
	 * @param operator 比較演算子
	 * @param value 比較する値
	 * @return thisインスタンス
	 * @see LineFilter, Operator
	 */
	public FlatFileManipulator searchFilter(int column, Operator operand,
			Object value) {
		this.searchFilter.filter(column, operand, value);

		return this;
	}

	/**
	 * ジョインされた行リストを検索します。<p/>
	 *
	 * このメソッド実行前に、{@link #searchFilter(int, Operator, Object)}で検索フィルタを追加しておく必要があります。
	 *
	 * @return 検索結果行リスト
	 */
	public List<String> search() {
		List<String> resultList = new ArrayList<String>();

		for (String line : this.joinedList) {
			if (!this.searchFilter.filter(line)) {
				continue;
			}
			resultList.add(line);
		}
		return resultList;
	}
}

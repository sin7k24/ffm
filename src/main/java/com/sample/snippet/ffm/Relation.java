package com.sample.snippet.ffm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 一表ファイルを表現するクラスです。<p/>
 *
 * temp.tmpファイルが以下の内容の場合
 * <pre>
 * 001 aaa bbb ccc
 * 002 aaa ddd eee
 * 003 ggg hhh iii
 * </pre>
 *
 * 下記コードで読み込むと
 * <pre>
 * int[] format = new int[]{0, 2, 3};
 * Relation left = new Relation("temp.tmp", 1, true, format);
 *
 * left.advance();
 * List<Row> rows = left.getConsecutiveRows();
 * for(Row row : rows) {
 *     System.out.println(row.getLine());
 * }
 * </pre>
 *
 * 結果は以下になります。
 * <pre>
 * 001 bbb ccc
 * 002 ddd eee
 * </pre>
 */
public class Relation {

	/** 表ファイルインスタンス */
	private File file;

	/** ジョインに使用するキー列番号 */
	private int joinKey;

	/** 左表か否かの真偽値 */
	private boolean isLeft;

	/** 出力列フォーマット */
	private int[] format;

	/** formatが指定されているかどうかの真偽値 */
	private boolean withFormat;

	/** 右記参照。{@link Const#DEFAULT_OMIT_LEFT_JOIN_KEYS} */
	private List<Integer> defaultOmitJoinKeys;

	/** 列分割デリミタ */
	private String delimiter;

	/** 対象ファイルリーダ */
	private BufferedReader reader;

	/** キー値列が変わった際、次回のadvance()で使用される最初の行 */
	private String prevLine;

	/** キー値列が同値で連続した行のリスト */
	private List<Row> consecutiveRows;

	/** ファイル終端真偽値 */
	private boolean isEof;

	/**
	 * {@link #joinKey}列値が同値の連続した行を返却します。<p/>
	 *
	 * {@link #advance()}がコールされると、次の連続した行で内容が更新されます。
	 *
	 * @return joinKey列値が同じ連続した行リスト
	 */
	public List<Row> getConsecutiveRows() {
		return this.consecutiveRows;
	}

	/**
	 * 以下の条件でインスタンスを生成します。<p/>
	 *
	 * <ol>
	 * <li>対象ファイル：引数file</li>
	 * <li>ジョインキー列番号：引数joinKey</li>
	 * <li>左表判定：引数isLeft</li>
	 * <li>出力列フォーマット：全列</li>
	 * <li>列分割デリミタ：Const.DEFAULT_DELIMITER</li>
	 * </ol>
	 *
	 * @param file 対象ファイル
	 * @param joinKey ジョインに使用するキー列番号
	 * @param isLeft 左表か否かの真偽値
	 * @throws IOException 対象ファイル読み込み失敗時
	 */
	public Relation(File file, int joinKey, boolean isLeft) throws IOException {
		this(file, joinKey, isLeft, new int[]{-1}, Const.DEFAULT_DELIMITER);
	}

	/**
	 * 以下の条件でインスタンスを生成します。<p/>
	 *
	 * <ol>
	 * <li>対象ファイル：引数file</li>
	 * <li>ジョインキー列番号：引数joinKey</li>
	 * <li>左表判定：引数isLeft</li>
	 * <li>出力列フォーマット：引数format</li>
	 * <li>列分割デリミタ：Const.DEFAULT_DELIMITER</li>
	 * </ol>
	 *
	 * @param file 対象ファイル
	 * @param joinKey ジョインに使用するキー列番号
	 * @param isLeft 左表か否かの真偽値
	 * @throws IOException 対象ファイル読み込み失敗時
	 */
	public Relation(File file, int joinKey, boolean isLeft, int[] format) throws IOException {
		this(file, joinKey, isLeft,format, Const.DEFAULT_DELIMITER);
	}

	/**
	 * 以下の条件でインスタンスを生成します。<p/>
	 *
	 * <ol>
	 * <li>対象ファイル：引数file</li>
	 * <li>ジョインキー列番号：引数joinKey</li>
	 * <li>左表判定：引数isLeft</li>
	 * <li>出力列フォーマット：引数format</li>
	 * <li>列分割デリミタ：引数delimiter</li>
	 * </ol>
	 *
	 * @param file 対象ファイル
	 * @param joinKey ジョインに使用するキー列番号
	 * @param isLeft 左表か否かの真偽値
	 * @param delimiter
	 * @throws IOException 対象ファイル読み込み失敗時
	 */
	public Relation(File file, int joinKey, boolean isLeft, int[] format,  String delimiter)
			throws IOException {
		this.file = file;
		this.joinKey = joinKey;
		this.isLeft = isLeft;
		this.format = format;
		this.withFormat = this.format[0] == -1 ? false : true;
		this.delimiter = delimiter;
		this.reader = new BufferedReader(new FileReader(this.file));
		this.consecutiveRows = new ArrayList<Row>();

		if(isLeft) {
			this.defaultOmitJoinKeys = Const.DEFAULT_OMIT_LEFT_JOIN_KEYS;
		}else{
			this.defaultOmitJoinKeys = Const.DEFAULT_OMIT_RIGHT_JOIN_KEYS;
		}
	}

	/**
	 * joinKey値が連続した行を取得します。<p/>
	 *
	 * joinKeyが1で、以下のファイルを読み取る場合、全行を読み取るには3回のadvance()が必要です。
	 * <pre>
	 * advance() 1回目
	 * 001, aaa, 823470
	 * 002, aaa, 021749
	 *
	 * advance() 2回目
	 * 003, bbb, 120934
	 *
	 * advance() 3回目
	 * 004, ccc, 029375
	 * 005, ccc, 184565
	 * 006, ccc, 947917
	 *
	 * adovance() false返却
	 * </pre>
	 *
	 * 取得した行のリストは{@link #getConsecutiveRows()}で取得します。
	 *
	 * @return true : 読み取り継続、false : 終端に達した場合、または取得した行が無かった場合
	 * @throws IOException 対象ファイル読み込み失敗時
	 */
	public boolean advance() throws IOException {
		this.consecutiveRows.clear();

		if (this.isEof == true) {
			this.reader.close();
			return false;
		}

		String prevKey = null;
		while (true) {
			String line = null;

			if(this.prevLine != null) {
				line = this.prevLine;
			}else {
				line = this.reader.readLine();
			}
			this.prevLine = null;

			if(line == null) {
				this.isEof = true;
				break;
			}

			String[] columns = line.split(this.delimiter);
			String key = columns[this.joinKey];

			if (prevKey != null && !prevKey.equals(key)) {
				this.prevLine = line;
				break;
			}

			line = format(columns);
			Row row = new Row(line, key);

			this.consecutiveRows.add(row);

			prevKey = key;
		}

		return this.consecutiveRows.size() > 0 ? true  : false;
	}

	/**
	 * 出力形式をフォーマットします。<p/>
	 *
	 * <ol>
	 * <li>コンストラクタでint[] formatが指定されていた場合</li><p/>
	 *
	 * <ol>
	 * <li>format配列分ループ</li>
	 * <li>引数columnsからformatで指定された列番号値を取得</li>
	 * <li>sb.append()で結合</li>
	 * </ol>
	 *
	 * <li>formatが未指定の場合</li>
	 * <ol>
	 * <li>引数columns分ループ</li>
	 * <li>{@link #defaultOmitJoinKeys}に含まれる列番号の場合はcontinue </li>
	 * <li>sb.append()で結合</li>
	 * </ol>
	 * </ol>
	 * @param columns
	 * @return
	 */
	private String format(String[] columns) {

		StringBuilder sb = new StringBuilder();

		if(this.withFormat) {
			for(int c : this.format) {
				sb.append(columns[c]);
				sb.append(this.delimiter);
			}
		}else{
			for(int i=0; i<columns.length; i++) {
				String column = columns[i];
				if(this.defaultOmitJoinKeys.contains(i)){
					continue;
				}
				sb.append(column);
				sb.append(this.delimiter);
			}
		}

		if(!isLeft) {
			sb.replace(sb.length()-1, sb.length(), "");
		}

		return sb.toString();
	}

	/**
	 * ファイルへのinpustreamを明示的にclose()します。<p/>
	 *
	 * {@link #advance()}でEOFに到達しなかったケースではclose()されません。
	 * このケースが発生する可能性が有る場合は明示的にこのメソッドをコールします。
	 *
	 * @throws IOException
	 */
	public void close() throws IOException {
		this.reader.close();
	}

	/**
	 * ファイル内の一行を表す内部クラスです。<p/>
	 *
	 */
	public class Row {
		/** format、列omit済みの行文字列 */
		private String line;

		/** joinKeyで指定された列の値 */
		private String key;

		/**
		 * 行文字列を返却します。<p/>
		 *
		 * @return 行文字列
		 */
		public String getLine() {
			return this.line;
		}

		/**
		 * joinKey列値を返却します。<p/>
		 *
		 * @return キー列値
		 */
		public String getKey() {
			return this.key;
		}

		/**
		 * 以下の条件でインスタンスを生成します。<p/>
		 *
		 * <ol>
		 * <li>行文字列：引数line</li>
		 * <li>joinKey列値：引数key</li>
		 * </ol>
		 *
		 * @param line 行文字列
		 * @param key joinKey列値
		 */
		public Row(String line, String key) {
			this.line = line;
			this.key = key;
		}
	}
}


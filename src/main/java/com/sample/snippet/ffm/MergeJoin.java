package com.sample.snippet.ffm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 二つの表ファイルをマージジョインするクラスです。<p/>
 *
 */
public class MergeJoin {

	/** 左表ファイル */
	private File leftFile;

	/** 右表ファイル */
	private File rightFile;

	/**
	 * 以下の条件でインスタンスを生成します。<p/>
	 *
	 * <ol>
	 * <li>左表ファイル：引数leftPath</li>
	 * <li>右表ファイル：引数rightPath</li>
	 * </ol>
	 *
	 * @param leftPath 左表ファイルパス
	 * @param rightPath 右表ファイルパス
	 */
	public MergeJoin(String leftPath, String rightPath) {
		this(new File(leftPath), new File(rightPath));
	}

	/**
	 * 以下の条件でインスタンスを生成します。<p/>
	 *
	 * <ol>
	 * <li>左表ファイル：引数leftPath</li>
	 * <li>右表ファイル：引数rightPath</li>
	 * </ol>
	 *
	 * @param leftFile 左表ファイルインスタンス
	 * @param rightFile 右表ファイルインスタンス
	 */
	public MergeJoin(File leftFile, File rightFile) {
		this.leftFile = leftFile;
		this.rightFile = rightFile;
	}

	/**
	 * 以下の条件でジョインを開始します。<p/>
	 *
	 * <ul>
	 * <li>ジョインに使用する左表キー列番号：Const.DEFAULT_JOIN_KEY</li>
	 * <li>ジョインに使用する右表キー列番号：Const.DEFAULT_JOIN_KEY</li>
	 * <li>ジョイン後の左表出力列フォーマット：指定無し</li>
	 * <li>ジョイン後の右表出力列フォーマット：指定無し</li>
	 * <li>列分割デリミタ：Const.DEFAULT_DELIMITER</li>
	 * </ul>
	 *
	 * @return ジョイン後のリスト
	 * @throws IOException 左表、右表ファイル読み込み時
	 */
	public List<String> join() throws IOException {

		return join(Const.DEFAULT_JOIN_KEY,
				Const.DEFAULT_JOIN_KEY,
				new int[] { -1 },
				new int[] { -1 },
				Const.DEFAULT_DELIMITER);
	}

	/**
	 * 以下の条件でジョインを開始します。<p/>
	 *
	 * <ul>
	 * <li>ジョインに使用する左表キー列番号：引数joinKey</li>
	 * <li>ジョインに使用する右表キー列番号：引数joinKey</li>
	 * <li>ジョイン後の左表出力列フォーマット：指定無し</li>
	 * <li>ジョイン後の右表出力列フォーマット：指定無し</li>
	 * <li>列分割デリミタ：Const.DEFAULT_DELIMITER</li>
	 * </ul>
	 *
	 * ジョインキー列番号がConst.DEFAULT_JOIN_KEY以外、かつ左右で等しい場合に使用します。
	 *
	 * @param joinKey 左右ファイルのジョインキー列番号
	 * @return ジョイン後のリスト
	 * @throws IOException 左表、右表ファイル読み込み時
	 */
	public List<String> join(int joinKey) throws IOException {

		return join(joinKey,
				joinKey,
				new int[] { -1 },
				new int[] { -1 },
				Const.DEFAULT_DELIMITER);
	}

	/**
	 * 以下の条件でジョインを開始します。<p/>
	 *
	 * <ul>
	 * <li>ジョインに使用する左表キー列番号：引数joinKey</li>
	 * <li>ジョインに使用する右表キー列番号：引数joinKey</li>
	 * <li>ジョイン後の左表出力列フォーマット：引数leftFormat</li>
	 * <li>ジョイン後の右表出力列フォーマット：引数rightFormat</li>
	 * <li>列分割デリミタ：Const.DEFAULT_DELIMITER</li>
	 * </ul>
	 *
	 * @param joinKey 左右ファイルのジョインキー列番号
	 * @param leftFormat ジョイン後の左ファイル出力列番号を指定する配列
	 * @param rightFormat ジョイン後の右ファイル出力列番号を指定する配列
	 * @return ジョイン後のリスト
	 * @throws IOException 左表、右表ファイル読み込み時
	 */
	public List<String> join(int joinKey,
			int[] leftFormat,
			int[] rightFormat) throws IOException
	{
		return join(joinKey,
				joinKey,
				leftFormat,
				rightFormat,
				Const.DEFAULT_DELIMITER);
	}

	/**
	 * 以下の条件でジョインを開始します。<p/>
	 *
	 * <ul>
	 * <li>ジョインに使用する左表キー列番号：引数leftJoinKey</li>
	 * <li>ジョインに使用する右表キー列番号：引数rightjoinKey</li>
	 * <li>ジョイン後の左表出力列フォーマット：引数leftFormat</li>
	 * <li>ジョイン後の右表出力列フォーマット：引数rightFormat</li>
	 * <li>列分割デリミタ：Const.DEFAULT_DELIMITER</li>
	 * </ul>
	 *
	 * ジョインキー列番号が左右で異なる場合、
	 * 列分割デリミタがConst.DEFAULT_DELIMITERでは無い場合に使用します。
	 *
	 * @param leftJoinKey 左ファイルのジョインキー列番号
	 * @param rightJoinKey 右ファイルのジョインキー列番号
	 * @param leftFormat ジョイン後の左ファイル出力列番号を指定する配列
	 * @param rightFormat ジョイン後の右ファイル出力列番号を指定する配列
	 * @param delimiter 列分割デリミタ
	 * @return ジョイン後のリスト
	 * @throws IOException IOException 左表、右表ファイル読み込み時
	 */
	public List<String> join(int leftJoinKey,
			int rightJoinKey,
			int[] leftFormat,
			int[] rightFormat,
			String delimiter) throws IOException
	{
		// 左表インスタンス生成
		Relation left = new Relation(this.leftFile,
				leftJoinKey,
				true,
				leftFormat,
				delimiter);

		// 右表インスタンス生成
		Relation right = new Relation(this.rightFile,
				rightJoinKey,
				false,
				rightFormat,
				delimiter);

		boolean leftHasNext = left.advance();
		boolean rightHasNext = right.advance();

		List<String> joinList = new ArrayList<String>();

		while (leftHasNext && rightHasNext) {
			String leftKey = left.getConsecutiveRows().get(0).getKey();
			String rightKey = right.getConsecutiveRows().get(0).getKey();

			int diff = leftKey.compareTo(rightKey);

			if (diff == 0) {
				// joinKey値が等しい場合は結合
				int leftSize = left.getConsecutiveRows().size();
				int rightSize = right.getConsecutiveRows().size();
				for (int i = 0; i < leftSize; i++) {
					for (int j = 0; j < rightSize; j++) {
						// 1:1文字列結合はString#concat()が最速
						joinList.add(left
								.getConsecutiveRows()
								.get(i)
								.getLine()
								.concat(right.getConsecutiveRows().get(j)
										.getLine()));
					}
				}
				leftHasNext = left.advance();
				rightHasNext = right.advance();
			} else if (diff < 0) {
				// 左表キー値が右表キー値より小さい場合、左行読み込みを進める
				leftHasNext = left.advance();
			} else if (diff > 0) {
				// 右表キー値が左表キー値より小さい場合、右行読み込みを進める
				rightHasNext = right.advance();
			}
		}

		// 結合終了後、左右ファイルへのinputstreamを確実に閉じる
		left.close();
		right.close();

		return joinList;
	}
}

package com.sample.snippet.ffm;

import static com.sample.snippet.ffm.Operator.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 行フィルタリングを行うクラスです。<p/>
 *
 * filterメソッドで指定されたcolumn列値をvalueとoperand比較した結果、trueであれば行として採用されます。
 *
 */
public class LineFilter {

	/** compareTo結果が0の場合にマッチする演算子値の合計 */
	public static final int DIFF_ZERO = EQ.getValue() + LTE.getValue() + GTE.getValue();

	/** compareTo結果が負数の場合にマッチする演算子値の合計 */
	public static final int DIFF_NEGATIVE = LT.getValue() + LTE.getValue();

	/** compareTo結果が正数の場合にマッチする演算子値の合計 */
	public static final int DIFF_POSITIVE = GT.getValue() + GTE.getValue();

	/** フィルタ条件リスト */
	private List<Filter> filters;

	/** 列分割デリミタ */
	private String delimiter;

	/** 列分割の最大数（不要なsplit処理を抑止） */
	private int splitLimit;

	/**
	 * 以下の条件でインスタンスを生成します。<p/>
	 *
	 * <ol>
	 * <li>列分割デリミタ：Const.DEFAULT_DELIMITER</li>
	 * </ol>
	 */
	public LineFilter() {
		this(Const.DEFAULT_DELIMITER);
	}

	/**
	 * 以下の条件でインスタンスを生成します。<p/>
	 *
	 * <ol>
	 * <li>列分割デリミタ：引数delimiter</li>
	 * </ol>
	 *
	 * @param delimiter 列分割デリミタ
	 */
	public LineFilter(String delimiter) {
		this.delimiter = delimiter;
		this.filters = new ArrayList<Filter>();
	}

	/**
	 * フィルタ条件を追加します。<p/>
	 *
	 * @param column 比較対象列番号
	 * @param operand 比較演算子
	 * @param value 比較する値
	 * @return thisインスタンス
	 */
	public LineFilter filter(int column, Operator operand, Object value) {
		Filter filter = new Filter(column, operand, value);
		this.filters.add(filter);

		// split必要最大数を特定
		this.splitLimit = Math.max(this.splitLimit, column);

		return this;
	}

	/**
	 * 引数lineが設定されたフィルタ条件にマッチするか検査します。<p/>
	 *
	 * フィルタ条件が設定されていない場合は常にtrueを返却します。
	 *
	 * @param line デリミタで分割された対象行文字列
	 * @return true：条件にマッチ、false：アンマッチ
	 */
	public boolean filter(String line) {
		if(filters.size() == 0) {
			return true;
		}

		// フィルタ比較処理に必要な最小限カラム数に分割
		String[] columns = line.split(this.delimiter, this.splitLimit + 2);

		for (Filter filter : this.filters) {
			// 対象カラム値、比較値
			String target = columns[filter.getColumn()];
			Object value = filter.getValue();

			// 対象カラム値と比較値をcompareTo
			int diff = 0;
			if (value instanceof Integer) {
				diff = Integer.decode(target).compareTo((Integer)value);
			}else if(value instanceof String) {
				diff = target.compareTo(value.toString());
			}

			// 設定された比較演算子
			Operator operand = filter.getOperator();

			// 論理積が0の場合、フィルタ条件はアンマッチ
			int condition = 0;
			if (diff == 0) {
				condition = DIFF_ZERO;
			}else if (diff < 0) {
				condition = DIFF_NEGATIVE;
			}else if (diff > 0) {
				condition = DIFF_POSITIVE;
			}

			if((condition & operand.getValue()) == 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * フィルタ条件を一つ保持するクラスです。<p/>
	 */
	public class Filter {
		/** 比較対象列番号 */
		private int column;

		/** 比較演算子 */
		private Operator operator;

		/** 比較する値 */
		private Object value;

		/**
		 * 比較対象列番号を返却します。<p/>
		 *
		 * @return 比較対象列番号
		 */
		public int getColumn() {
			return this.column;
		}

		/**
		 * 比較演算子を返却します。<p/>
		 *
		 * @return 比較演算子
		 */
		public Operator getOperator() {
			return this.operator;
		}

		/**
		 * 比較する値を返却します。<p/>
		 *
		 * @return 比較する値
		 */
		public Object getValue() {
			return this.value;
		}

		/**
		 * 以下の条件でインスタンスを生成します。<p/>
		 *
		 * <ol>
		 * <li>比較対象列番号：引数column</li>
		 * <li>比較演算子：引数operator</li>
		 * <li>比較する値：引数value</li>
		 * </ol>
		 *
		 * @param column
		 * @param operator
		 * @param value
		 */
		public Filter(int column, Operator operator, Object value) {
			this.column = column;
			this.operator = operator;
			this.value = value;
		}
	}
}

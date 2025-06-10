package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_LST = "commodity.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	//商品別集計ファイル名
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "が存在しません";
	private static final String FILE_INVALID_FORMAT = "のフォーマットが不正です";
	private static final String FILE_NAME_NOT_SEQUENTIAL = "売上ファイル名が連番になっていません";
	private static final String SALES_AMOUNT_EXCEEDED_10FIGURES = "合計金額が10桁を超えました";
	private static final String BRANCH_CODE_INVALID = "の支店コードが不正です";
	private static final String COMMODITY_FILE = "商品定義ファイル";
	private static final String BRANCH_FILE = "支店定義ファイル";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();
		// 支店定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales, "^[0-9]{3}$", BRANCH_FILE)) {
			return;
		}
		Map<String, String> commodityNames = new HashMap<>();
		Map<String, Long> commoditySales = new HashMap<>();
		if (!readFile(args[0], FILE_NAME_COMMODITY_LST, commodityNames, commoditySales,
				"^[A-Za-z0-9]{8}$", COMMODITY_FILE)) {
			return;
		}
		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		File[] files = new File(args[0]).listFiles();
		List<File> rcdFiles = new ArrayList<>();
		String regex = "^[0-9]{8}.rcd$";
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile() && files[i].getName().matches(regex)) {
				rcdFiles.add(files[i]);
			}
		}
		Collections.sort(rcdFiles);
		for (int i = 0; i < rcdFiles.size() -1; i++) {
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));
			if ((latter - former) != 1) {
				System.out.println(FILE_NAME_NOT_SEQUENTIAL);
				return;
			}
		}
		for(File file : rcdFiles) {
			if (!readRcdFile(args[0], file.getName(), branchSales, commoditySales)){
				return;
			}
		}
		// 支店別集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}
		if (!writeFile(args[0], FILE_NAME_COMMODITY_OUT, commodityNames, commoditySales)) {
			return;
		}
	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> names,
			Map<String, Long> sales, String regex, String errorMessage) {
		BufferedReader br = null;
		try {
			File file = new File(path, fileName);
			if (!file.exists()) {
				System.out.println(errorMessage + FILE_NOT_EXIST);
				return false;
			}
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);
			String line;
			// 一行ずつ読み込む
			while((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				String[] items = line.split(",");
				if (items.length != 2 || !items[0].matches(regex)) {
					System.out.println(errorMessage + FILE_INVALID_FORMAT);
					return false;
				}
				names.put(items[0], items[1]);
				sales.put(items[0], 0L);
			}
		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	private static boolean readRcdFile(String path, String fileName, Map<String, Long> branchSales,
			Map<String, Long> commoditySales) {
		BufferedReader br = null;
		try {
			File file = new File(path, fileName);
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);
			String line;
			List<String> rcd = new ArrayList<>();
			// 一行ずつ読み込む
			while ((line = br.readLine()) != null) {
				rcd.add(line);
			}
			if (rcd.size() != 3) {
				System.out.println(fileName + FILE_INVALID_FORMAT);
				return false;
			}
			String branchCode = rcd.get(0);
			String commodityName = rcd.get(1);
			String saleAmount = rcd.get(2);
			if (!branchSales.containsKey(branchCode)) {
				System.out.println(fileName + BRANCH_CODE_INVALID);
				return false;
			}
			if (!commoditySales.containsKey(commodityName)) {
				System.out.println(fileName + BRANCH_CODE_INVALID);
				return false;
			}
			if (!saleAmount.matches("^[0-9]+$")) {
				System.out.println(UNKNOWN_ERROR);
				return false;
			}
			Long branchSaleAmount = branchSales.get(branchCode) + Long.parseLong(saleAmount);
			Long commoditySaleAmount = commoditySales.get(commodityName) + Long.parseLong(saleAmount);
			if (branchSaleAmount >= 10000000000L || commoditySaleAmount >= 10000000000L) {
				System.out.println(SALES_AMOUNT_EXCEEDED_10FIGURES);
				return false;
			}
			branchSales.put(branchCode, branchSaleAmount);
			commoditySales.put(commodityName, commoditySaleAmount);
		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName,
			Map<String, String> names, Map<String, Long> sales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)
		File file = new File(path, fileName);
		BufferedWriter bw = null;
		try {
			String result;
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);
			for (String key : names.keySet()) {
				result = key + "," + names.get(key) + "," + Long.toString(sales.get(key));
				bw.write(result);
				bw.newLine();
			}
		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}
}
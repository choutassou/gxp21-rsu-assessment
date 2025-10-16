package com.accdev.gxp21;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.accdev.gxp21.db.AnalysisSchemaAccessHelper;
import com.accdev.gxp21.db.IraiSchemaAccessHelper;
import com.accdev.gxp21.util.CtCalculator;
import com.accdev.gxp21.util.JPAUtil;

/**
 * RSU評価アプリケーションのメインクラス。
 */
public class Assessment {

    private static Logger logger; // 静的初期化を削除し、後で初期化

    private static Properties props;
    private static EntityManagerFactory emfAnalysis;
    private static LocalTime triggerTime;
    private static boolean hasExecutedToday = false;
    private static ScheduledExecutorService scheduler;
    /*
    private static final double adjustments[] = { 10, 10.1, 10.2, 10.3, 10.4, 10.8, 11.0, 11.2, 11.4, 11.6, 11.8, 12.0,
            12.2, 12.4, 12.6, 12.8,
            13.0 };
    private static final double thresholds[] = { 6.5, 6.55, 6.60, 6.65, 6.70, 6.75, 6.80, 6.85, 6.90, 6.95, 7.00, 7.05,
            7.10,
            7.15, 7.20, 7.25, 7.30, 7.35, 7.40, 7.45, 7.50 };
    */

    private static final double adjustments[] = { 10.8, 10.9, 11.0, 11.1, 11.2, 11.3, 11.4 };
    private static final double thresholds[] = { 6.76, 6.78, 6.80, 6.82, 6.84, 6.86, 6.88, 6.89 };

    //private static final double adjustments[] = { 11.0, 11.2 };
    //private static final double thresholds[] = { 6.63, 6.65, 6.70, 6.75 };

    /**
     * アプリケーションのメインエントリーポイント。
     * プロパティの読み込み、JPA設定の初期化、スケジューラーの開始を行います。
     * 
     * @param args コマンドライン引数
     * @throws Exception 初期化時に発生する例外
     */
    public static void main(String[] args) throws Exception {
        // ① プロパティ読み込み
        loadProperties();

        // ② ログ設定の初期化（Loggerより前に実行）
        initializeLogging();

        // ③ Loggerの初期化（システムプロパティ設定後）
        logger = LoggerFactory.getLogger(Assessment.class);

        // ⑤ interval の取得（秒数）
        int intervalSeconds = Integer.parseInt(props.getProperty("interval", "600"));

        logger.info("Starting Assessment application.");
        logger.info("Check interval: {} seconds", intervalSeconds);

        // ⑥ スケジューラーの開始
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(Assessment::checkAndExecute, 0, intervalSeconds, TimeUnit.SECONDS);

        // ⑦ シャットダウンフックの追加
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down application...");
            shutdown();
        }));

        // ⑧ メインスレッドを維持
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            logger.warn("Main thread was interrupted.");
            shutdown();
        }
    }

    /**
     * プロパティファイルを読み込み、必要な設定値を初期化します。
     * 
     * @throws Exception プロパティファイルの読み込みに失敗した場合
     */
    private static void loadProperties() throws Exception {
        props = new Properties();
        try (InputStream in = new FileInputStream("Assessment.properties")) {
            props.load(in);
        }
    }

    /**
     * JPA用のEntityManagerFactoryを初期化します。
     * プロパティファイルから取得したデータベース設定を使用します。
     */
    private static void initializeEntityManagerFactory() {
        Map<String, String> jpaProps = new HashMap<>();
        jpaProps.put("jakarta.persistence.jdbc.url", props.getProperty("db_connection_string"));
        jpaProps.put("jakarta.persistence.jdbc.user", props.getProperty("analysis_db_user"));
        jpaProps.put("jakarta.persistence.jdbc.password", props.getProperty("analysis_db_password"));
        jpaProps.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        emfAnalysis = JPAUtil.getAnalysisEntityManagerFactory(jpaProps);
    }

    /**
     * データベース接続を確認し、必要に応じて再接続します。
     * 
     * @return 接続が正常かどうか
     */
    private static boolean checkAndRecoverDatabaseConnection() {
        EntityManager testEm = null;
        try {
            // 解析データベースの接続テスト
            testEm = emfAnalysis.createEntityManager();
            testEm.createNativeQuery("SELECT 1").getSingleResult();
            testEm.close();

            return true;
        } catch (Exception e) {
            logger.warn("Database connection error detected. Attempting to reconnect: {}", e.getMessage());

            try {
                // EntityManagerが開いていれば閉じる
                if (testEm != null && testEm.isOpen()) {
                    testEm.close();
                }

                // EntityManagerFactoryを再作成
                Map<String, String> jpaProps = new HashMap<>();
                jpaProps.put("jakarta.persistence.jdbc.url", props.getProperty("db_connection_string"));
                jpaProps.put("jakarta.persistence.jdbc.user", props.getProperty("analysis_db_user"));
                jpaProps.put("jakarta.persistence.jdbc.password", props.getProperty("analysis_db_password"));
                jpaProps.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");

                JPAUtil.recreateAnalysisEntityManagerFactory(jpaProps);
                emfAnalysis = JPAUtil.getAnalysisEntityManagerFactory(jpaProps);

                logger.info("Database connection recovery completed.");
                return true;
            } catch (Exception recoveryException) {
                logger.error("Failed to recover database connection: {}", recoveryException.getMessage());
                return false;
            }
        }
    }

    /**
     * 指定された時間になったかをチェックし、必要に応じてデータ処理とメール送信を実行します。
     * スケジューラーから定期的に呼び出されます。
     */
    private static void checkAndExecute() {
        try {
            LocalTime now = LocalTime.now();

            logger.info("Check Begin {}", now);

            // プロパティを再読み込みして、設定変更を反映
            loadProperties();

            // ⑤ trigger_time の解析
            LocalTime previousTriggerTime = triggerTime;
            String triggerTimeStr = props.getProperty("trigger_time", "00:40:00");
            triggerTime = LocalTime.parse(triggerTimeStr, DateTimeFormatter.ofPattern("HH:mm:ss"));
            if (previousTriggerTime == null || !previousTriggerTime.equals(triggerTime)) {
                hasExecutedToday = false; // トリガー時間が変更された場合、フラグをリセット
                logger.info("Trigger time updated: {}", triggerTime);
            }

            // 現在時刻がトリガー時刻を過ぎているかチェック
            if (now.isAfter(triggerTime) && !hasExecutedToday) {
                logger.info("The specified time has come. Starting processing: {}", now);
                // データベース接続状態を確認
                if (checkAndRecoverDatabaseConnection()) {
                    // データ処理を実行
                    performAssessment();
                } else {
                    logger.error(
                            "Unable to establish database connection. Skipping data import and email notification processing.");
                }

                hasExecutedToday = true;
            } else if (now.isBefore(triggerTime) && hasExecutedToday) {
                // 日付が変わった場合のリセット（トリガー時刻より前になった場合）
                hasExecutedToday = false;
                logger.info("A new day has begun. Resetting execution flag.");
            }
            logger.info("Check End {}", LocalTime.now());
        } catch (Exception e) {
            logger.error("Error during check processing: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void performAssessment() {
        if (emfAnalysis == null) {
            initializeEntityManagerFactory();
        }

        EntityManager emAnalysis = null;
        try {
            emAnalysis = emfAnalysis.createEntityManager();
            AnalysisSchemaAccessHelper anaHelper = new AnalysisSchemaAccessHelper(emAnalysis);

            // rsu未設定のサンプル名を取得
            var sampleNames = anaHelper.findSampleNames2();
            logger.info("Found {} samples without RSU set.", sampleNames.size());

            // y_stdデータを取得（ここでは仮に固定値を使用）
            Map<String, BigDecimal> yStd = getYStd();
            Map<String, Map<String, BigDecimal>> sampleCtMap = new HashMap<>();
            for (String sampleName : sampleNames) {
                // 各サンプル名について、ctデータを取得
                var ctDataList = anaHelper.findCtBySampleName(sampleName);

                Map<String, BigDecimal> ctMap = new HashMap<>();
                for (var ctData : ctDataList) {
                    String geneName = ctData.getGeneName();
                    // geneNameごとにMapを作成
                    ctMap.putIfAbsent(geneName, ctData.getCt());
                }
                sampleCtMap.put(sampleName, ctMap);
            }

            // 各組み合わせでweightedDiffAverageを計算
            for (double adjustment : adjustments) {
                for (double threshold : thresholds) {
                    BigDecimal result = weightedDiffAverage(sampleCtMap, yStd, adjustment, threshold);
                    logger.info("{}\t{}\t{}", adjustment, threshold, result);
                }
            }
        } catch (Exception e) {
            logger.error("Error during assessment processing: {}", e.getMessage());
            e.printStackTrace();
        } finally {
            if (emAnalysis != null && emAnalysis.isOpen()) {
                emAnalysis.close();
            }
        }
    }

    public static BigDecimal weightedDiffAverage(Map<String, Map<String, BigDecimal>> x, Map<String, BigDecimal> yStd,
            double adjustment,
            double threshold) {
        Map<String, BigDecimal> y = new HashMap<>();
        // calcを呼び出し
        x.forEach((k, v) -> {
            BigDecimal r = CtCalculator.wtAssessment(new CtCalculator(), v, adjustment, threshold);
            y.put(k, r);
        });

        if (y.size() != yStd.size()) {
            throw new IllegalArgumentException("yとy_stdのサイズが一致しません");
        }

        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal weightTotal = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : y.entrySet()) {
            String k = entry.getKey();
            BigDecimal yi = entry.getValue();
            BigDecimal ysi = yStd.get(k);

            // |y - y_std|
            BigDecimal diff = yi.subtract(ysi).abs();

            // 重み判定
            BigDecimal weight = getWeight(ysi);

            // 加算
            weightedSum = weightedSum.add(diff.multiply(weight));
            weightTotal = weightTotal.add(weight);
        }

        // 平均 = (重み付き差の合計) / (重みの合計)
        BigDecimal avg = BigDecimal.ZERO;
        if (weightTotal.compareTo(BigDecimal.ZERO) > 0) {
            avg = weightedSum.divide(weightTotal, 6, RoundingMode.HALF_UP);
        }

        // 平均偏差を返す
        return avg;
    }

    private static BigDecimal getWeight(BigDecimal yStd) {
        int v = yStd.intValue();
        if (v >= 23 && v <= 27)
            return new BigDecimal("1.2");
        if ((v >= 19 && v <= 22) || (v >= 28 && v <= 31))
            return new BigDecimal("1.1");
        if ((v >= 15 && v <= 18) || (v >= 32 && v <= 35))
            return BigDecimal.ONE;
        return new BigDecimal("0.6");
    }

    /**
     * アプリケーションの終了処理を行います。
     * スケジューラーとEntityManagerFactoryを適切に終了します。
     */
    private static void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }

        if (emfAnalysis != null && emfAnalysis.isOpen()) {
            emfAnalysis.close();
        }

        logger.info("Application completed normally.");
    }

    /**
     * プロパティ値を取得する
     * @param key プロパティキー
     * @return プロパティ値
     */
    public static String getProperty(String key) {
        return props != null ? props.getProperty(key) : null;
    }

    /**
     * プロパティ値を取得する（デフォルト値付き）
     * @param key プロパティキー
     * @param defaultValue デフォルト値
     * @return プロパティ値
     */
    public static String getProperty(String key, String defaultValue) {
        return props != null ? props.getProperty(key, defaultValue) : defaultValue;
    }

    /**
     * ログ設定を初期化します。
     * プロパティファイルから読み込んだ値をシステムプロパティに設定し、logback.xmlで参照できるようにします。
     */
    private static void initializeLogging() {
        // プロパティファイルからログ設定値を取得してシステムプロパティに設定
        String logDir = props.getProperty("log_dir", "./logs");
        String logLevel = props.getProperty("log_level", "INFO");

        System.setProperty("log.dir", logDir);
        System.setProperty("log.level", logLevel);

        // ログディレクトリの作成
        try {
            Files.createDirectories(Paths.get(logDir));
        } catch (IOException e) {
            System.err.println("failure to create folder for logs: " + logDir);
            e.printStackTrace();
        }

        // この時点ではまだloggerはnullなので、System.out.printlnを使用
        System.out.println("log settings initialized: " + logDir + ", level=: " + logLevel);
    }

    private static Map<String, BigDecimal> getYStd() {
        Map<String, BigDecimal> yStd = new HashMap<>();
        yStd.put("KyuDai_20250919-10", new BigDecimal(0.0));
        yStd.put("KyuDai_20250918-2", new BigDecimal(2.0));
        yStd.put("KyuDai20250907-3", new BigDecimal(4.0));
        yStd.put("KyuDai_20250922-12", new BigDecimal(5.0));
        yStd.put("KyuDai_20250922-7", new BigDecimal(5.0));
        yStd.put("KyuDai_20250918-12", new BigDecimal(5.0));
        yStd.put("KyuDai_20250922-17", new BigDecimal(9.0));
        yStd.put("KyuDai_20250919-3", new BigDecimal(10.0));
        yStd.put("KyuDai_20250922-6", new BigDecimal(11.0));
        yStd.put("KyuDai_20250922-4", new BigDecimal(11.0));
        yStd.put("KyuDai_20250918-5", new BigDecimal(12.0));
        yStd.put("KyuDai_20250922-18", new BigDecimal(13.0));
        yStd.put("KyuDai_20250922-23", new BigDecimal(13.0));
        yStd.put("KyuDai_20250922-2", new BigDecimal(13.0));
        yStd.put("KyuDai_20250918-6", new BigDecimal(13.0));
        yStd.put("KyuDai_20250919-1", new BigDecimal(14.0));
        yStd.put("KyuDai_20250918-4", new BigDecimal(15.0));
        yStd.put("KyuDai_20250922-13", new BigDecimal(15.0));
        yStd.put("KyuDai_20250919-6", new BigDecimal(15.0));
        yStd.put("KyuDai_20250919-8", new BigDecimal(15.0));
        yStd.put("KyuDai_20250922-10", new BigDecimal(16.0));
        yStd.put("KyuDai_20250922-15", new BigDecimal(16.0));
        yStd.put("KyuDai_20250918-1", new BigDecimal(17.0));
        yStd.put("KyuDai_20250919-7", new BigDecimal(19.0));
        yStd.put("KyuDai_20250919-12", new BigDecimal(20.0));
        yStd.put("KyuDai_20250918-10", new BigDecimal(20.0));
        yStd.put("KyuDai_20250922-3", new BigDecimal(22.0));
        yStd.put("KyuDai_20250922-16", new BigDecimal(22.0));
        yStd.put("KyuDai_20250918-3", new BigDecimal(22.0));
        yStd.put("KyuDai_20250918-8", new BigDecimal(25.0));
        yStd.put("KyuDai_20250922-8", new BigDecimal(25.0));
        yStd.put("KyuDai_20250919-2", new BigDecimal(25.0));
        yStd.put("KyuDai_20250919-9", new BigDecimal(25.0));
        yStd.put("KyuDai_20250919-4", new BigDecimal(26.0));
        yStd.put("KyuDai_20250922-14", new BigDecimal(26.0));
        yStd.put("KyuDai_20250922-9", new BigDecimal(26.0));
        yStd.put("KyuDai20250907-7", new BigDecimal(26.0));
        yStd.put("KyuDai_20250922-22", new BigDecimal(26.0));
        yStd.put("KyuDai_20250922-1", new BigDecimal(27.0));
        yStd.put("KyuDai_20250922-5", new BigDecimal(31.0));
        yStd.put("KyuDai20250907-8", new BigDecimal(31.0));
        yStd.put("KyuDai20250907-2", new BigDecimal(31.0));
        yStd.put("KyuDai20250907-1", new BigDecimal(33.0));
        yStd.put("KyuDai_20250918-11", new BigDecimal(33.0));
        yStd.put("KyuDai_20250918-7", new BigDecimal(35.0));
        yStd.put("KyuDai_20250922-11", new BigDecimal(37.0));
        yStd.put("KyuDai_20250922-21", new BigDecimal(37.0));
        yStd.put("KyuDai_20250918-9", new BigDecimal(41.0));
        yStd.put("KyuDai_20250919-11", new BigDecimal(43.0));
        yStd.put("KyuDai_20250922-20", new BigDecimal(43.0));
        yStd.put("KyuDai20250907-8_2", new BigDecimal(46.0));
        yStd.put("KyuDai_20250922-19", new BigDecimal(51.0));
        yStd.put("KyuDai20250907-4", new BigDecimal(52.0));
        yStd.put("KyuDai20250907-5", new BigDecimal(52.0));
        yStd.put("KyuDai_20250919-5", new BigDecimal(71.0));
        return yStd;
        // インスタンス化防止
    }
}
package com.accdev.gxp21.util;

import java.util.Map;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class JPAUtil {
    private static EntityManagerFactory emfAnalysis;

    public static synchronized EntityManagerFactory getAnalysisEntityManagerFactory(Map<String, String> overrides) {
        if (emfAnalysis == null) {
            emfAnalysis = Persistence.createEntityManagerFactory("GetDataAnaPU", (Map<String, String>) overrides);
        }
        return emfAnalysis;
    }

    /**
     * 解析用EntityManagerFactoryを再作成します（データベース再起動時の対応）
     */
    public static synchronized void recreateAnalysisEntityManagerFactory(Map<String, String> overrides) {
        if (emfAnalysis != null && emfAnalysis.isOpen()) {
            emfAnalysis.close();
        }
        emfAnalysis = Persistence.createEntityManagerFactory("GetDataAnaPU", overrides);
    }

    /**
     * 現在の解析用EntityManagerFactoryを閉じて無効化します
     */
    public static synchronized void closeAnalysisEntityManagerFactory() {
        if (emfAnalysis != null && emfAnalysis.isOpen()) {
            emfAnalysis.close();
            emfAnalysis = null;
        }
    }
}
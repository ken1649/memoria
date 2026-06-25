package com.guoyuan.memoria

object AdConfig {
    // 建議將環境設定分開
    private const val IS_TEST_MODE = false

    // 將 const val 改為 val，並定義一個 getter
    val MAIN_BANNER_ID: String
        get() = if (IS_TEST_MODE) {
            "ca-app-pub-3940256099942544/9214589741" // 測試用 ID
        } else {
            "ca-app-pub-3520859691508878/3440029044" // 正式 廣告ID
        }
}

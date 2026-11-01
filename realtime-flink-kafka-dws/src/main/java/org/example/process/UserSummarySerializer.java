// 文件路径：src/main/java/org/example/process/UserSummarySerializer.java
package org.example.process;

import com.alibaba.fastjson.JSON;
import org.example.bean.UserSummary;

/**
 * UserSummary JSON 序列化器。
 *
 * DWS 用户主题输出链路：
 *
 * UserSummary
 *      ↓
 * JSON
 *      ↓
 * Kafka
 *      ↓
 * dws_user_summary
 */
public final class UserSummarySerializer {

    private UserSummarySerializer() {
        // 工具类禁止实例化
    }

    /**
     * UserSummary -> JSON String
     *
     * @param summary 用户主题一分钟汇总结果
     * @return JSON 字符串；输入为空时返回 null
     */
    public static String serialize(
            UserSummary summary) {

        if (summary == null) {
            return null;
        }

        try {

            return JSON.toJSONString(
                    summary
            );

        } catch (Exception e) {

            System.err.println(
                    "UserSummary序列化失败："
                            + e.getMessage()
            );

            return null;
        }
    }
}
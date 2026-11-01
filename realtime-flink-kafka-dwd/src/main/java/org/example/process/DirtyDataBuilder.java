package org.example.process;

import com.alibaba.fastjson.JSONObject;
import org.example.bean.DirtyData;

/**
 * DirtyData 构造工具。
 *
 * 负责把：
 *
 * 原始 ODS JSON
 *
 * +
 *
 * 清洗异常
 *
 * 转换为统一的脏数据对象。
 */
public class DirtyDataBuilder {

    /**
     * 工具类不创建对象。
     */
    private DirtyDataBuilder() {
    }


    /**
     * 构造脏数据。
     *
     * @param odsJson   原始 ODS JSON
     * @param exception 清洗过程中的异常
     * @return DirtyData
     */
    public static DirtyData build(
            JSONObject odsJson,
            Exception exception) {

        DirtyData dirtyData =
                new DirtyData();


        // ============================================================
        // 1. 来源表
        // ============================================================

        if (odsJson != null) {

            dirtyData.setTableName(
                    odsJson.getString(
                            "table"
                    )
            );

        } else {

            dirtyData.setTableName(
                    null
            );
        }


        // ============================================================
        // 2. ODS 操作类型
        // ============================================================

        if (odsJson != null) {

            dirtyData.setOpType(
                    odsJson.getString(
                            "type"
                    )
            );

        } else {

            dirtyData.setOpType(
                    null
            );
        }


        // ============================================================
        // 3. 异常类型
        // ============================================================

        /*
         * 例如：
         *
         * java.lang.IllegalArgumentException
         *
         * getSimpleName()
         *
         * 得到：
         *
         * IllegalArgumentException
         */
        if (exception != null) {

            dirtyData.setErrorType(
                    exception
                            .getClass()
                            .getSimpleName()
            );

        } else {

            dirtyData.setErrorType(
                    null
            );
        }


        // ============================================================
        // 4. 异常信息
        // ============================================================

        if (exception != null) {

            dirtyData.setErrorMessage(
                    exception.getMessage()
            );

        } else {

            dirtyData.setErrorMessage(
                    null
            );
        }


        // ============================================================
        // 5. 保存原始 ODS JSON
        // ============================================================

        /*
         * 这里不能只保存 data。
         *
         * 应该保存完整消息：
         *
         * {
         *   "table":"orders",
         *   "type":"bootstrap",
         *   "data":{...}
         * }
         *
         * 因为排查问题时，
         * table 和 type 同样有价值。
         */
        if (odsJson != null) {

            dirtyData.setRawData(
                    odsJson.toJSONString()
            );

        } else {

            dirtyData.setRawData(
                    null
            );
        }


        // ============================================================
        // 6. DWD 处理时间
        // ============================================================

        dirtyData.setProcessTs(
                System.currentTimeMillis()
        );


        return dirtyData;
    }
}
package org.example.process;

import org.example.ShopBootstrapApp;
import org.example.TradeBootstrapApp;
import org.example.UserBootstrapApp;
import org.example.bean.ShopSummary;
import org.example.bean.TradeSummary;
import org.example.bean.UserSummary;
import org.junit.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collections;

public class BootstrapSnapshotValidationTest {

    @Test
    public void tradeAcceptsFactBootstrapSnapshotWithoutUpdates() throws Exception {
        TradeBootstrapResult result = new TradeBootstrapResult(
                20015L, 20015L, 0L, 20015,
                null, null, 20015L, 0L, BigDecimal.ZERO,
                Collections.singletonList(new TradeSummary())
        );

        invokeValidation(TradeBootstrapApp.class, TradeBootstrapResult.class, result);
    }

    @Test
    public void shopAcceptsFactBootstrapSnapshotWithoutUpdates() throws Exception {
        ShopBootstrapResult result = new ShopBootstrapResult(
                20015L, 20015L, 0L, 20015L,
                null, null, 20015L, 19999L,
                new BigDecimal("9985570708.91"),
                new BigDecimal("9984940947.39"),
                Collections.nCopies(2164, new ShopSummary())
        );

        invokeValidation(ShopBootstrapApp.class, ShopBootstrapResult.class, result);
    }

    @Test
    public void userAcceptsFactBootstrapSnapshotWithoutUpdates() throws Exception {
        UserBootstrapResult result = new UserBootstrapResult(
                20015L, 20015L, 0L, 20015L,
                null, null, 20015L, 19999L,
                new BigDecimal("9985570708.91"),
                new BigDecimal("9984940947.39"),
                Collections.nCopies(1994, new UserSummary())
        );

        invokeValidation(UserBootstrapApp.class, UserBootstrapResult.class, result);
    }

    private static <T> void invokeValidation(
            Class<?> appClass,
            Class<T> resultClass,
            T result) throws Exception {
        Method validate = appClass.getDeclaredMethod("validate", resultClass);
        validate.setAccessible(true);
        validate.invoke(null, result);
    }
}

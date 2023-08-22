package com.ysj.demo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

/**
 * <p>
 *
 * @author Ysj
 * Create time: 2021/7/21
 */
public class TestStatic {
    private static final String TAG = "TestStatic";

    public TestStatic() {
    }

    public static int tasd_ad9_80989_1() {
        return 1;
    }

    public void t2() {
        Intent a;
        (a = new Intent()).setFlags(2222222);
    }

    public void b(Object var0, String... var3) {
        long x = 1L;
        int a = 1;
        Object o = var3.length == 0 ? var3 : a == 1 ? null : null;
    }

    public static int tra(String o) {
        Log.i(TAG, "tra: " + o);
        return 11111;
    }

    public static void a(Object var0, Object... var3) throws Exception {
        Method var8 = var0.getClass().getMethod("valueOf", Object.class);
        Log.i(TAG, "a: " + var8.invoke(null, var3.length == 0 ? "000" : "var3"));
    }
}

package com.moyu.rpc.invoke;

import java.util.Map;

/**
 * 调用的上下文
 */
public interface Invocation {

    String getMethodName();

    String getServiceName();

    Class<?>[] getParameterTypes();

    Object[] getArguments();

    Map<String, String> getAttachments();

    String getAttachment(String key);

    String getAttachment(String key, String defaultValue);

    Invoker getInvoker();

    Object put(Object key, Object value);

    Object get(Object key);

    Map<Object, Object> getAttributes();
}
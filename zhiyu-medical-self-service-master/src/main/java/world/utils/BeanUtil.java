package world.utils;

import com.alibaba.fastjson2.JSON;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.cglib.beans.BeanMap;

import java.util.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Bean 工具
 *
 */
public class BeanUtil {

    private static Map<String, BeanCopier> beanCopierMap = new HashMap<>();  // 修复1：添加泛型

    /**
     * 复制对象到新对象
     */
    public static <T> T copy(Object src, Class<T> clazz) 
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if ((null == src) || (null == clazz)) return null;
        
        // 修复2：使用 getDeclaredConstructor().newInstance() 替代弃用的 newInstance()
        Object des = clazz.getDeclaredConstructor().newInstance();
        copy(src, des);
        
        // 修复3：添加 SuppressWarnings 注解
        @SuppressWarnings("unchecked")
        T result = (T) des;
        return result;
    }

    /**
     * 复制对象属性
     */
    public static void copy(Object src, Object des) {
        if ((null == src) || (null == des)) return;
        String key = generateKey(src.getClass(), des.getClass());
        BeanCopier copier = beanCopierMap.get(key);
        if (null == copier) {
            copier = BeanCopier.create(src.getClass(), des.getClass(), false);
            beanCopierMap.put(key, copier);
        }
        copier.copy(src, des, null);
    }

    /**
     * Map 转 Bean
     */
    public static <T> T map2Bean(Map<String, Object> map, Class<T> clazz) 
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if ((null == map) || (null == clazz)) return null;
        T bean = clazz.getDeclaredConstructor().newInstance();  // 同样修复
        map2Bean(map, bean);
        return bean;
    }

    /**
     * Map 转 Bean（已存在的Bean）
     */
    public static <T> void map2Bean(Map<String, Object> map, T bean) {
        if ((null == map) || (null == bean)) return;
        BeanMap beanMap = BeanMap.create(bean);
        beanMap.putAll(map);
    }

    /**
     * Bean 转 Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> bean2Map(Object bean) {
        if (null == bean) return null;
        return copy(BeanMap.create(bean));
    }

    /**
     * Bean列表 转 Map列表
     */
    public static <T> List<Map<String, Object>> mapList(List<T> beanList) {
        if ((null == beanList) || (beanList.isEmpty())) return null;
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (T bean : beanList) {  // 使用增强for循环替代索引循环
            mapList.add(bean2Map(bean));
        }
        return mapList;
    }

    /**
     * 复制Map
     */
    public static <K, V> Map<K, V> copy(Map<K, V> src) {
        if (null == src) return null;
        return new HashMap<>(src);  // 直接使用HashMap的拷贝构造
    }

    /**
     * 应用属性（从多个源对象复制到目标对象，仅当目标属性为空时）
     */
    public static void apply(Object des, Object... srcs) {
        if ((null == des) || (null == srcs) || (srcs.length < 1)) return;
        BeanMap desBeanMap = BeanMap.create(des);
        Set<?> keys = desBeanMap.keySet();
        for (Object src : srcs) {
            if (null != src) {
                BeanMap srcBeanMap = BeanMap.create(src);
                for (Object key : keys) {
                    Object value = srcBeanMap.get(key);
                    if ((null != value) && (null == desBeanMap.get(key))) {
                        desBeanMap.put(des, key, value);
                    }
                }
            }
        }
    }

    /**
     * 应用属性（从Map列表复制到目标对象）
     */
    public static void apply(Object des, List<Map<String, Object>> srcList) {
        if ((null == des) || (null == srcList) || srcList.isEmpty()) return;
        BeanMap desBeanMap = BeanMap.create(des);
        Set<?> keys = desBeanMap.keySet();
        for (Map<String, Object> src : srcList) {  // 使用增强for循环
            if ((null != src) && !src.isEmpty()) {
                for (Object key : keys) {
                    Object value = src.get(key);
                    if (null != value) {
                        desBeanMap.put(des, key, value);
                    }
                }
            }
        }
    }

    /**
     * 生成BeanCopier的缓存key
     */
    private static String generateKey(Class<?> src, Class<?> des) {
        return src.getName() + des.getName();
    }

    /**
     * Bean 转 String
     */
    public static <T> String beanToString(T value) {
        if (value == null) {
            return null;
        }
        Class<?> clazz = value.getClass();
        if (clazz == int.class || clazz == Integer.class) {
            return "" + value;
        } else if (clazz == String.class) {
            return (String) value;
        } else if (clazz == long.class || clazz == Long.class) {
            return "" + value;
        } else {
            return JSON.toJSONString(value);
        }
    }

    /**
     * String 转 Bean
     */
    @SuppressWarnings("unchecked")
    public static <T> T stringToBean(String str, Class<T> clazz) {
        if (str == null || str.length() <= 0 || clazz == null) {
            return null;
        }
        if (clazz == int.class || clazz == Integer.class) {
            return (T) Integer.valueOf(str);
        } else if (clazz == String.class) {
            return (T) str;
        } else if (clazz == long.class || clazz == Long.class) {
            return (T) Long.valueOf(str);
        } else {
            return JSON.parseObject(str, clazz);
        }
    }
}
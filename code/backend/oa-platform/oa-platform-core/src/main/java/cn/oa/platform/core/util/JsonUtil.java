package cn.oa.platform.core.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * JSON 工具类（基于 Jackson）
 *
 * @author oa-platform
 */
@Slf4j
public final class JsonUtil {

    private JsonUtil() {
        // 私有构造函数，防止实例化
    }

    /**
     * ObjectMapper 实例（线程安全）
     */
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    /**
     * 创建并配置 ObjectMapper
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 注册 Java 8 时间模块
        mapper.registerModule(new JavaTimeModule());
        // 日期格式
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        // 忽略未知属性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 禁用日期序列化为时间戳
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // 忽略空值
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    /**
     * 获取 ObjectMapper 实例
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * 对象转 JSON 字符串
     *
     * @param obj 对象
     * @return JSON 字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转JSON失败: {}", e.getMessage(), e);
            throw new RuntimeException("对象转JSON失败", e);
        }
    }

    /**
     * 对象转 JSON 字符串（格式化）
     *
     * @param obj 对象
     * @return 格式化的 JSON 字符串
     */
    public static String toJsonPretty(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转JSON失败: {}", e.getMessage(), e);
            throw new RuntimeException("对象转JSON失败", e);
        }
    }

    /**
     * JSON 字符串转对象
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            log.error("JSON转对象失败: {}", e.getMessage(), e);
            throw new RuntimeException("JSON转对象失败", e);
        }
    }

    /**
     * JSON 字符串转对象（支持泛型）
     *
     * @param json          JSON 字符串
     * @param typeReference 类型引用
     * @param <T>           泛型
     * @return 对象
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (IOException e) {
            log.error("JSON转对象失败: {}", e.getMessage(), e);
            throw new RuntimeException("JSON转对象失败", e);
        }
    }

    /**
     * JSON 字符串转对象（支持复杂泛型）
     *
     * @param json          JSON 字符串
     * @param parametrized  参数化类型
     * @param parameterClasses 类型参数
     * @param <T>           泛型
     * @return 对象
     */
    public static <T> T fromJson(String json, Class<?> parametrized, Class<?>... parameterClasses) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructParametricType(parametrized, parameterClasses);
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (IOException e) {
            log.error("JSON转对象失败: {}", e.getMessage(), e);
            throw new RuntimeException("JSON转对象失败", e);
        }
    }

    /**
     * JSON 字符串转 List
     *
     * @param json  JSON 字符串
     * @param clazz 元素类型
     * @param <T>   泛型
     * @return List
     */
    public static <T> List<T> fromJsonList(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructParametricType(List.class, clazz);
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (IOException e) {
            log.error("JSON转List失败: {}", e.getMessage(), e);
            throw new RuntimeException("JSON转List失败", e);
        }
    }

    /**
     * JSON 字符串转 Map
     *
     * @param json JSON 字符串
     * @return Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> fromJsonMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, Map.class);
        } catch (IOException e) {
            log.error("JSON转Map失败: {}", e.getMessage(), e);
            throw new RuntimeException("JSON转Map失败", e);
        }
    }

    /**
     * 对象转换为 Map
     *
     * @param obj 对象
     * @return Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(obj, Map.class);
    }

    /**
     * Map 转换为对象
     *
     * @param map   Map
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 对象
     */
    public static <T> T fromMap(Map<String, ?> map, Class<T> clazz) {
        if (map == null) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(map, clazz);
    }

    /**
     * 深拷贝
     *
     * @param obj   原对象
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 深拷贝对象
     */
    public static <T> T deepCopy(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        return fromJson(toJson(obj), clazz);
    }

    /**
     * 判断是否为有效的 JSON 字符串
     *
     * @param json JSON 字符串
     * @return 是否有效
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}

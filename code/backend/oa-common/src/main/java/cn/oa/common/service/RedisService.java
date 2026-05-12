package cn.oa.common.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public <T> T getJson(String key, TypeReference<T> typeRef) {
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj == null) return null;
        try {
            String json = objectMapper.writeValueAsString(obj);
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.error("Redis JSON 反序列化失败: {}", e.getMessage());
            return null;
        }
    }

    public <T> T getJson(String key, Class<T> clazz) {
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj == null) return null;
        try {
            String json = objectMapper.writeValueAsString(obj);
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Redis JSON 反序列化失败: {}", e.getMessage());
            return null;
        }
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    public boolean deleteByPattern(String pattern) {
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            return true;
        }
        return false;
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    public long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit));
    }

    public void putHash(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public Object getHash(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    public void deleteHash(String key, String... hashKeys) {
        redisTemplate.opsForHash().delete(key, (Object[]) hashKeys);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}

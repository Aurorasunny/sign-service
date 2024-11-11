package com.chenjin.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 键值对象
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-10-27 17:36
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignPair<K, V> {
    private K key;

    private V value;

    /**
     * 构建键值对象
     */
    public static <K, V> SignPair<K, V> of(K key, V value) {
        return new SignPair<>(key, value);
    }
}

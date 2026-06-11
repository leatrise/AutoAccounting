/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.auto.service.ocr

import net.ankio.auto.utils.PrefManager
import org.json.JSONArray

/**
 * 页面特征管理器
 *
 * 负责：存储、匹配、内容指纹生成。
 * 匹配逻辑：包名 + activity（空=任意）+ 指纹（空=不校验，否则相似度阈值）
 */
object PageSignatureManager {

    /**
     * 获取所有已记住的页面签名
     */
    fun getAll(): List<PageSignature> {
        return parse(PrefManager.pageSignatures)
    }

    /**
     * 获取所有不再询问的页面签名
     */
    fun getAllIgnored(): List<PageSignature> {
        return parse(PrefManager.ignoredPageSignatures)
    }

    private fun parse(raw: String): List<PageSignature> {
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { PageSignature.fromJson(it) }
            }
        }.getOrElse { emptyList() }
    }

    /**
     * 添加页面签名
     */
    fun add(sig: PageSignature) {
        val list = getAll().toMutableList()
        list.removeAll { it.key() == sig.key() }
        list.add(sig)
        saveRemembered(list)
        removeIgnored(sig.key())
    }

    /**
     * 将页面加入不再询问列表
     */
    fun ignore(sig: PageSignature) {
        val list = getAllIgnored().toMutableList()
        list.removeAll { it.key() == sig.key() }
        list.add(sig)
        saveIgnored(list)
        remove(sig.key())
    }

    /**
     * 移除指定 key 的签名
     */
    fun remove(key: String) {
        val list = getAll().filter { it.key() != key }.toMutableList()
        saveRemembered(list)
    }

    /**
     * 从不再询问列表移除指定 key
     */
    fun removeIgnored(key: String) {
        val list = getAllIgnored().filter { it.key() != key }.toMutableList()
        saveIgnored(list)
    }

    /**
     * 匹配条件：包名 + activity + 结构指纹
     * 签名中 structureFingerprint 为空时退化为 pkg+activity 匹配（兼容旧数据）
     */
    fun matches(
        packageName: String,
        activityName: String,
        structureFingerprint: String = "",
    ): Boolean = matches(
        getAll(),
        packageName,
        activityName,
        structureFingerprint,
    )

    /**
     * 当前页面是否已被标记为不再询问
     */
    fun isIgnored(
        packageName: String,
        activityName: String,
        structureFingerprint: String = "",
    ): Boolean = matches(
        getAllIgnored(),
        packageName,
        activityName,
        structureFingerprint,
    )

    private fun matches(
        signatures: List<PageSignature>,
        packageName: String,
        activityName: String,
        structureFingerprint: String,
    ): Boolean = signatures.any { sig ->
        sig.packageName == packageName &&
                sig.activityName == activityName &&
                (sig.structureFingerprint.isBlank() || sig.structureFingerprint == structureFingerprint)
    }

    private fun toJson(list: List<PageSignature>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        return arr.toString()
    }

    private fun saveRemembered(list: List<PageSignature>) {
        PrefManager.pageSignatures = toJson(list)
    }

    private fun saveIgnored(list: List<PageSignature>) {
        PrefManager.ignoredPageSignatures = toJson(list)
    }
}

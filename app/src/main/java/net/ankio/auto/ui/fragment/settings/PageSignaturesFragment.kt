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

package net.ankio.auto.ui.fragment.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentPageSignaturesBinding
import net.ankio.auto.databinding.ItemPageSignatureBinding
import net.ankio.auto.service.ocr.PageSignature
import net.ankio.auto.service.ocr.PageSignatureManager
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.utils.getAppInfoFromPackageName

/**
 * 页面特征管理 Fragment
 *
 * 展示并管理已记住页面和不再询问页面。
 */
class PageSignaturesFragment : BaseFragment<FragmentPageSignaturesBinding>() {

    private lateinit var adapter: Adapter
    private var listMode = ListMode.REMEMBERED

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        adapter = Adapter(
            onDelete = { sig -> confirmDelete(sig, listMode) }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.listTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                listMode = if (tab.position == 0) {
                    ListMode.REMEMBERED
                } else {
                    ListMode.IGNORED
                }
                refreshList()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        binding.listTabs.getTabAt(
            if (listMode == ListMode.REMEMBERED) 0 else 1
        )?.select()
        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun confirmDelete(sig: PageSignature, mode: ListMode) {
        val appName = getAppInfoFromPackageName(sig.packageName)?.name ?: sig.packageName
        val message = when (mode) {
            ListMode.REMEMBERED -> getString(R.string.ocr_delete_page_confirm, appName)
            ListMode.IGNORED -> getString(R.string.ocr_delete_ignored_page_confirm, appName)
        }
        BaseSheetDialog.create<BottomSheetDialogBuilder>(requireContext())
            .setTitle(getString(R.string.delete_data))
            .setMessage(message)
            .setPositiveButton(getString(R.string.sure_msg)) { _, _ ->
                when (mode) {
                    ListMode.REMEMBERED -> PageSignatureManager.remove(sig.key())
                    ListMode.IGNORED -> PageSignatureManager.removeIgnored(sig.key())
                }
                refreshList()
            }
            .setNegativeButton(getString(R.string.cancel_msg)) { _, _ -> }
            .show()
    }

    private fun refreshList() {
        val list = when (listMode) {
            ListMode.REMEMBERED -> PageSignatureManager.getAll()
            ListMode.IGNORED -> PageSignatureManager.getAllIgnored()
        }
        adapter.submitList(list)
        binding.emptyView.setText(
            when (listMode) {
                ListMode.REMEMBERED -> R.string.page_signatures_empty
                ListMode.IGNORED -> R.string.ignored_page_signatures_empty
            }
        )
        binding.emptyView.visibility =
            if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private enum class ListMode {
        REMEMBERED,
        IGNORED,
    }

    private class Adapter(
        private val onDelete: (PageSignature) -> Unit
    ) : RecyclerView.Adapter<Adapter.VH>() {

        private var items: List<PageSignature> = emptyList()

        fun submitList(list: List<PageSignature>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b =
                ItemPageSignatureBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val sig = items[position]
            val appInfo = getAppInfoFromPackageName(sig.packageName)
            holder.binding.appIcon.setImageDrawable(appInfo?.icon)
            holder.binding.appName.text = appInfo?.name ?: sig.packageName
            holder.binding.activityName.text = sig.activityName.ifBlank { "-" }
            holder.binding.structureFingerprint.apply {
                text = sig.structureFingerprint.ifBlank { "-" }
                visibility =
                    if (sig.structureFingerprint.isBlank()) android.view.View.GONE
                    else android.view.View.VISIBLE
            }
            holder.itemView.setOnLongClickListener { onDelete(sig); true }
        }

        override fun getItemCount(): Int = items.size

        class VH(val binding: ItemPageSignatureBinding) : RecyclerView.ViewHolder(binding.root)
    }
}

// SPDX-License-Identifier: LicenseRef-GPL-3.0-or-later-OpenSSL

package com.metallic.chiaki.settings

import android.content.ClipData
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.metallic.chiaki.R
import com.metallic.chiaki.common.LogFile
import com.metallic.chiaki.common.LogManager
import com.metallic.chiaki.common.ext.viewModelFactory
import com.metallic.chiaki.common.fileProviderAuthority
import kotlinx.android.synthetic.main.fragment_settings_logs.*

class SettingsLogsFragment: AppCompatDialogFragment(), TitleFragment
{
	private lateinit var viewModel: SettingsLogsViewModel

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
		inflater.inflate(R.layout.fragment_settings_logs, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?)
	{
		val context = context!!

		viewModel = ViewModelProvider(this, viewModelFactory { SettingsLogsViewModel(LogManager(context)) })
			.get(SettingsLogsViewModel::class.java)

		val adapter = SettingsLogsAdapter()
		logsRecyclerView.layoutManager = LinearLayoutManager(context)
		logsRecyclerView.adapter = adapter
		adapter.shareCallback = this::shareLogFile
		viewModel.sessionLogs.observe(viewLifecycleOwner, Observer {
			adapter.logFiles = it
			emptyInfoGroup.visibility = if(it.isEmpty()) View.VISIBLE else View.GONE
		})

		val itemTouchSwipeCallback = object : ItemTouchSwipeCallback(context)
		{
			override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int)
			{
				val pos = viewHolder.adapterPosition
				val file = viewModel.sessionLogs.value?.getOrNull(pos) ?: return
				viewModel.deleteLog(file)
			}
		}
		ItemTouchHelper(itemTouchSwipeCallback).attachToRecyclerView(logsRecyclerView)
	}

	override fun getTitle(resources: Resources): String = resources.getString(R.string.preferences_logs_title)

	private fun shareLogFile(file: LogFile)
	{
		val activity = activity ?: return
		val uri = FileProvider.getUriForFile(activity, fileProviderAuthority, file.file)
		Intent(Intent.ACTION_SEND).also {
			it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			it.type = "text/plain"
			it.putExtra(Intent.EXTRA_STREAM, uri)
			it.clipData = ClipData.newRawUri("", uri)
			startActivity(Intent.createChooser(it, resources.getString(R.string.action_share_log)))
		}

	}
}
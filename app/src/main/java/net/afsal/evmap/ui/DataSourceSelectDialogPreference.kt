package net.afsal.evmap.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import net.afsal.evmap.fragment.DataSourceSelectDialog

class DataSourceSelectDialogPreference(ctx: Context, attrs: AttributeSet) :
    ListPreference(ctx, attrs) {
    override fun onClick() {
        val dialog = DataSourceSelectDialog.getInstance(true)
        dialog.okListener = { selected ->
            value = selected
        }
        dialog.show((context as AppCompatActivity).supportFragmentManager, null)
    }
}
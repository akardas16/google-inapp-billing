package games.moisoni.google_inapp_billing

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.text.TextUtils

object SharedPrefsHelper {
    private const val DEFAULT_SUFFIX = "_preferences"
    private var mPrefs: SharedPreferences? = null
    private fun initPrefs(context: Context, prefsName: String?, mode: Int) {
        mPrefs = context.getSharedPreferences(prefsName, mode)
    }

    private val preferences: SharedPreferences?
        get() {
            if (mPrefs != null) {
                return mPrefs
            }
            throw RuntimeException("SharedPrefsHelper class is not correctly instantiated")
        }

    fun getInt(key: String?, defValue: Int): Int {
        return preferences!!.getInt(key, defValue)
    }

    fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return preferences!!.getBoolean(key, defValue)
    }

    fun getString(key: String?, defValue: String?): String? {
        return preferences!!.getString(key, defValue)
    }

    fun putInt(key: String?, value: Int) {
        val editor = preferences!!.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun putBoolean(key: String?, value: Boolean) {
        val editor = preferences!!.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun putString(key: String?, value: String?) {
        val editor = preferences!!.edit()
        editor.putString(key, value)
        editor.apply()
    }

    class Builder {
        private var mKey: String? = null
        private var mContext: Context? = null
        private var mMode = -1
        private var mUseDefault = false
        fun setPrefsName(prefsName: String?): Builder {
            mKey = prefsName
            return this
        }

        fun setContext(context: Context?): Builder {
            mContext = context
            return this
        }

        fun setMode(mode: Int): Builder {
            mMode = if (mode == ContextWrapper.MODE_PRIVATE) {
                mode
            } else {
                throw RuntimeException("Mode can only be set to ContextWrapper.MODE_PRIVATE")
            }
            return this
        }

        fun setUseDefaultSharedPreference(defaultSharedPreference: Boolean): Builder {
            mUseDefault = defaultSharedPreference
            return this
        }

        fun build() {
            if (mContext == null) {
                throw RuntimeException("Please set the context before building SharedPrefsHelper instance")
            }
            if (TextUtils.isEmpty(mKey)) {
                mKey = mContext!!.packageName
            }
            if (mUseDefault) {
                mKey += DEFAULT_SUFFIX
            }
            if (mMode == -1) {
                mMode = ContextWrapper.MODE_PRIVATE
            }
            initPrefs(mContext!!, mKey, mMode)
        }
    }
}
package com.cxapp.calendar.lam

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.CalendarList
import com.infrastructure.common.base.BasicActivity
import com.infrastructure.common.logger.printInfo
import com.infrastructure.filebase.kv.KV
import com.infrastructure.filebase.kv.insertOrUpdate2DB

/**
 * @author: Brolin
 * @create: 2019/06/25 17:12
 */
class GSuitCalendar(private val mContext: Context) {

    private companion object {
        private val GSUIT_CALENDAR_CHOSEN_ACCOUNT = "GSUIT_CALENDAR_CHOSEN_ACCOUNT"
        private val REQUEST_ACCOUNT_PICKER = 2
    }


    private val mCredential by lazy {
        JacksonFactory.getDefaultInstance()
        val credential = GoogleAccountCredential.usingOAuth2(mContext, setOf(CalendarScopes.CALENDAR))
        credential.selectedAccountName = chosenAccount
        return@lazy credential
    }

    private val mClient by lazy {
        val transport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = JacksonFactory.getDefaultInstance()
        return@lazy Calendar.Builder(transport, jsonFactory, mCredential)
            .setApplicationName("Google-CalendarAndroidSample/1.0")
            .build()
    }

    private var chosenAccount: String? = null
        get() {
            if (field == null) {
                field = KV.query<String>(GSUIT_CALENDAR_CHOSEN_ACCOUNT)
            }
            return field
        }
        set(value) {
            field = value
            if (value != null) {
                value.insertOrUpdate2DB(GSUIT_CALENDAR_CHOSEN_ACCOUNT)
            } else {
                KV.delete(GSUIT_CALENDAR_CHOSEN_ACCOUNT)
            }
        }

    fun chooseAccount(activity: BasicActivity) {
        activity.startActivityForResult(
            mCredential.newChooseAccountIntent(),
            REQUEST_ACCOUNT_PICKER
        ) { resultCode, data ->
            if (resultCode == Activity.RESULT_OK && data != null && data.extras != null) {
                val accountName = data.extras!!.getString(AccountManager.KEY_ACCOUNT_NAME)
                if (accountName != null) {
                    mCredential.selectedAccountName = accountName
                    chosenAccount = accountName
                }
            }
        }
    }


    fun getCalendarList(): CalendarList {
        val feed = mClient.calendarList().list().execute()
        feed.printInfo()
        return feed
    }

    fun getEvents(){
        mClient.events().list("primary")
    }

}
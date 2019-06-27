package com.cxapp.calendar.lam

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.CalendarList
import com.infrastructure.common.base.BasicActivity
import com.infrastructure.common.logger.printInfo
import com.infrastructure.common.permissions.PermissionsBuilder
import com.infrastructure.filebase.kv.KV
import com.infrastructure.filebase.kv.insertOrUpdate2DB
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*

/**
 * @author: Brolin
 * @create: 2019/06/25 17:12
 */
class GSuitCalendar(private val mContext: FragmentActivity) {

    init {
        PermissionsBuilder.instance(mContext){
            it.permissions(Manifest.permission.GET_ACCOUNTS, Manifest.permission.GET_ACCOUNTS)
                .access()
        }
    }

    private companion object {
        private val GSUIT_CALENDAR_CHOSEN_ACCOUNT = "GSUIT_CALENDAR_CHOSEN_ACCOUNT"
        private val REQUEST_ACCOUNT_PICKER = 2
    }


    private val mCredential by lazy {
        JacksonFactory.getDefaultInstance()
        val credential = GoogleAccountCredential.usingOAuth2(mContext, Collections.singleton(CalendarScopes.CALENDAR))
        credential.selectedAccountName = chosenAccount
        return@lazy credential
    }

    private val mClient by lazy {
        val transport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = JacksonFactory.getDefaultInstance()
        return@lazy com.google.api.services.calendar.Calendar.Builder(transport, jsonFactory, mCredential)
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
                    getCalendarList()
                }
            }
        }
    }


    fun getCalendarList() {
        Single.create<Boolean> {
            val feed = mClient.calendarList().list().execute()
            feed.printInfo()
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe()
    }

    fun getEvents(){
        mClient.events().list("primary")
    }

}
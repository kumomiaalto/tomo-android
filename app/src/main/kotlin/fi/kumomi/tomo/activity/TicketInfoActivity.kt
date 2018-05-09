package fi.kumomi.tomo.activity

import android.content.pm.ActivityInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import fi.kumomi.tomo.R
import fi.kumomi.tomo.model.AirlineTicket
import fi.kumomi.tomo.observable.AirlineTicketObservable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_ticket_info.*

class TicketInfoActivity : AppCompatActivity() {
    private val tag = "TicketActivity"
    private var observable: Observable<AirlineTicket>? = null
    private var switchObservable = PublishSubject.create<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(tag, "Activity Created")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_info)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        observable = AirlineTicketObservable().create()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .repeatWhen { it.delay(5, TimeUnit.SECONDS) }
                .retryWhen { it.flatMap { Observable.timer(5, TimeUnit.SECONDS) } }

        switchObservable
                .switchMap { if(it) observable else Observable.never() }
                .subscribe {
                    name.text = "${it.firstName} ${it.lastName}"
                }
    }

    override fun onResume() {
        super.onResume()
        Log.i(tag, "Activity resumed")
        switchObservable.onNext(true)
    }

    override fun onPause() {
        super.onPause()
        Log.i(tag, "Activity Paused")
        switchObservable.onNext(false)
    }
}

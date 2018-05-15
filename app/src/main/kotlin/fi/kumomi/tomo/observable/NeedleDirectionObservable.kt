package fi.kumomi.tomo.observable

import android.content.Context
import fi.kumomi.tomo.TomoApplication
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe

class NeedleDirectionObservable {
    companion object {
        fun create(context: TomoApplication): Observable<Double> {
            return Observable.create({ emitter ->
                emitter.onNext(context.direction)
                emitter.onComplete()
            })
        }
    }
}

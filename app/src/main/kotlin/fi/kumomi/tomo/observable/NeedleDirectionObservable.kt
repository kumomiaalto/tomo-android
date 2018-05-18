package fi.kumomi.tomo.observable

import fi.kumomi.tomo.TomoApplication
import io.reactivex.Observable

class NeedleDirectionObservable {
    companion object {
        fun create(context: TomoApplication): Observable<Double> {
            return Observable.create({ emitter ->
                emitter.onNext(context.rotateAngle)
                emitter.onComplete()
            })
        }
    }
}

package fi.kumomi.tomo.util

class RadiansToDegrees {
    companion object {
        fun convert(radians: Double): Double {
            var fRadians = radians
            if (fRadians < 0)
                fRadians += (2 * Math.PI)

            return Math.toDegrees(fRadians)
        }
    }
}
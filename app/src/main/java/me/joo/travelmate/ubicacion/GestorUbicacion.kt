package me.joo.travelmate.ubicacion
import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

object GestorUbicacion {
    @SuppressLint("MissingPermission")
    fun obtener(contexto: Context, resultado: (Double?, Double?) -> Unit) {
        val cliente = LocationServices.getFusedLocationProviderClient(contexto)
        cliente.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { resultado(it?.latitude, it?.longitude) }
            .addOnFailureListener { resultado(null, null) }
    }
}

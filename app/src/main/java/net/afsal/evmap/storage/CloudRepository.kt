package net.afsal.evmap.storage

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import net.afsal.evmap.model.Favorite

/**
 * Centralized repository for all Firestore cloud operations.
 *
 * Manages two Firestore document paths per authenticated user:
 *   - `users/{uid}/profile/vehicle_info`   → Vehicle Name & Registration Number
 *   - `users/{uid}/favorites/{docId}`       → Favorited charger IDs
 *
 * All public methods are coroutine-friendly (suspend) and fail silently
 * when the user is not authenticated, keeping the local-first UX intact.
 */
object CloudRepository {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /** Returns the current Firebase UID, or null if not signed in. */
    private fun uid(): String? = auth.currentUser?.uid

    // ─── Vehicle Profile ────────────────────────────────────────────────

    data class VehicleProfile(
        val vehicleName: String = "",
        val vehicleRegistration: String = "",
        val vehicleChargeRate: Double = 0.0
    )

    /**
     * Fetches the vehicle profile from Firestore.
     * Returns null when the user is not authenticated or no document exists.
     */
    suspend fun getVehicleProfile(): VehicleProfile? {
        val uid = uid() ?: return null
        return try {
            val doc = firestore
                .collection("users").document(uid)
                .collection("profile").document("vehicle_info")
                .get()
                .await()
            if (doc.exists()) {
                VehicleProfile(
                    vehicleName = doc.getString("vehicle_name") ?: "",
                    vehicleRegistration = doc.getString("vehicle_registration") ?: "",
                    vehicleChargeRate = doc.getDouble("vehicle_charge_rate") ?: 0.0
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Writes (or merges) the vehicle profile fields into Firestore.
     * Accepts a partial map so callers can push a single field at a time.
     */
    suspend fun setVehicleProfile(
        vehicleName: String,
        vehicleRegistration: String,
        vehicleChargeRate: Double = 0.0
    ) {
        val uid = uid() ?: return
        try {
            firestore
                .collection("users").document(uid)
                .collection("profile").document("vehicle_info")
                .set(
                    hashMapOf(
                        "vehicle_name" to vehicleName,
                        "vehicle_registration" to vehicleRegistration,
                        "vehicle_charge_rate" to vehicleChargeRate
                    ),
                    SetOptions.merge()
                )
                .await()
        } catch (_: Exception) {
            // Silently fail — local data is the source of truth
        }
    }

    // ─── Favorites ──────────────────────────────────────────────────────

    /**
     * Pushes a favorite charger ID into Firestore.
     * Document ID format: `{dataSource}_{chargerId}` to guarantee uniqueness.
     */
    suspend fun pushFavorite(chargerId: Long, chargerDataSource: String, chargerName: String) {
        val uid = uid() ?: return
        try {
            firestore
                .collection("users").document(uid)
                .collection("favorites")
                .document("${chargerDataSource}_${chargerId}")
                .set(
                    hashMapOf(
                        "chargerId" to chargerId,
                        "chargerDataSource" to chargerDataSource,
                        "chargerName" to chargerName
                    )
                )
                .await()
        } catch (_: Exception) {
            // Silently fail
        }
    }

    /**
     * Deletes a favorite charger document from Firestore.
     */
    suspend fun removeFavorite(chargerId: Long, chargerDataSource: String) {
        val uid = uid() ?: return
        try {
            firestore
                .collection("users").document(uid)
                .collection("favorites")
                .document("${chargerDataSource}_${chargerId}")
                .delete()
                .await()
        } catch (_: Exception) {
            // Silently fail
        }
    }

    /**
     * Fetches all cloud-stored favorite IDs.
     * Returns a list of (chargerId, chargerDataSource) pairs.
     */
    suspend fun getAllCloudFavorites(): List<Pair<Long, String>> {
        val uid = uid() ?: return emptyList()
        return try {
            val snapshot = firestore
                .collection("users").document(uid)
                .collection("favorites")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                val id = doc.getLong("chargerId")
                val ds = doc.getString("chargerDataSource")
                if (id != null && ds != null) id to ds else null
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

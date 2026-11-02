package com.otm.ordertracker.data.repo

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "OtmFirestore"

/** Live query results; served from the offline cache first, then kept in sync with the server. */
fun Query.snapshotFlow(): Flow<QuerySnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            Log.e(TAG, "Query listener failed", error)
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

fun DocumentReference.snapshotFlow(): Flow<DocumentSnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            Log.e(TAG, "Document listener failed", error)
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

/**
 * Writes are not awaited: Firestore applies them to the local cache immediately (so the UI updates
 * and the app works offline) and syncs to the server in the background. We only log failures.
 */
fun Task<*>.logFailure(what: String) {
    addOnFailureListener { Log.e(TAG, "$what failed", it) }
}

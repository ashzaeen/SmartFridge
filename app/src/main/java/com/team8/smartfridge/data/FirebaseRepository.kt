package com.team8.smartfridge.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.team8.smartfridge.BuildConfig
import com.team8.smartfridge.demo.DemoStore
import com.team8.smartfridge.model.*
import kotlinx.coroutines.tasks.await

/**
 * Every Firebase call the app makes. In the DEMO build (BuildConfig.DEMO) each
 * method is served from [DemoStore] instead, and Firebase is never touched.
 */
class FirebaseRepository {

    val auth: FirebaseAuth by lazy { Firebase.auth }
    private val db by lazy { Firebase.firestore }

    val currentUid: String?
        get() = if (BuildConfig.DEMO) DemoStore.signedIn else auth.currentUser?.uid

    val isLoggedIn: Boolean
        get() = currentUid != null

    // ── Auth ──────────────────────────────────────────────────────────────────

    suspend fun register(email: String, password: String): String {
        if (BuildConfig.DEMO) return DemoStore.register(email)
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user?.uid ?: throw IllegalStateException("Registration succeeded but no user returned.")
    }

    suspend fun login(email: String, password: String): String {
        if (BuildConfig.DEMO) return DemoStore.login(email)
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user?.uid ?: throw IllegalStateException("Login succeeded but no user returned.")
    }

    fun logout() {
        if (BuildConfig.DEMO) { DemoStore.signedIn = null; return }
        auth.signOut()
    }

    suspend fun sendPasswordResetEmail(email: String) {
        if (BuildConfig.DEMO) return
        auth.sendPasswordResetEmail(email).await()
    }

    // ── User profile ──────────────────────────────────────────────────────────

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    suspend fun saveUserProfile(uid: String, user: User) {
        if (BuildConfig.DEMO) { DemoStore.saveProfile(uid, user); return }
        userDoc(uid).set(user).await()
    }

    suspend fun getUserProfile(uid: String): User? {
        if (BuildConfig.DEMO) return DemoStore.profile(uid)
        return userDoc(uid).get().await().toObject(User::class.java)
    }

    suspend fun updateUserProfile(uid: String, user: User) {
        if (BuildConfig.DEMO) { DemoStore.saveProfile(uid, user); return }
        userDoc(uid).set(user).await()
    }

    // ── Daily logs ────────────────────────────────────────────────────────────

    private fun logDoc(uid: String, date: String) =
        userDoc(uid).collection("daily_logs").document(date)

    suspend fun getLogForDate(uid: String, date: String): DailyLog? {
        if (BuildConfig.DEMO) return DemoStore.logFor(uid, date)
        return logDoc(uid, date).get().await().toObject(DailyLog::class.java)
    }

    suspend fun saveLog(uid: String, log: DailyLog) {
        if (BuildConfig.DEMO) { DemoStore.saveLog(uid, log); return }
        logDoc(uid, log.date).set(log).await()
    }

    suspend fun getAllLogs(uid: String): List<DailyLog> {
        if (BuildConfig.DEMO) return DemoStore.allLogs(uid)
        return userDoc(uid).collection("daily_logs")
            .orderBy("date", Query.Direction.DESCENDING)
            .get().await()
            .toObjects(DailyLog::class.java)
    }

    // ── Pantry ────────────────────────────────────────────────────────────────

    private fun pantryCol(uid: String) = userDoc(uid).collection("pantry_items")

    suspend fun getPantry(uid: String): List<PantryItem> {
        if (BuildConfig.DEMO) return DemoStore.pantryFor(uid)
        return pantryCol(uid).whereEqualTo("available", true)
            .get().await()
            .toObjects(PantryItem::class.java)
    }

    suspend fun addPantryItem(uid: String, item: PantryItem): String {
        if (BuildConfig.DEMO) return DemoStore.addPantry(uid, item)
        val ref = if (item.itemId.isNotBlank())
            pantryCol(uid).document(item.itemId)
        else
            pantryCol(uid).document()
        ref.set(item.copy(itemId = ref.id)).await()
        return ref.id
    }

    suspend fun addPantryItems(uid: String, items: List<PantryItem>) {
        if (BuildConfig.DEMO) { items.forEach { DemoStore.addPantry(uid, it.copy(itemId = "")) }; return }
        items.chunked(499).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { item ->
                val ref = pantryCol(uid).document()
                batch.set(ref, item.copy(itemId = ref.id))
            }
            batch.commit().await()
        }
    }

    suspend fun deletePantryItem(uid: String, itemId: String) {
        if (BuildConfig.DEMO) { DemoStore.removePantry(uid, itemId); return }
        pantryCol(uid).document(itemId).delete().await()
    }

    suspend fun clearPantry(uid: String) {
        if (BuildConfig.DEMO) { DemoStore.clearPantry(uid); return }
        val docs = pantryCol(uid).get().await().documents
        docs.chunked(499).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }

    // ── Saved recipes ─────────────────────────────────────────────────────────

    private fun savedRecipeDoc(uid: String, recipeId: Int) =
        userDoc(uid).collection("saved_recipes").document(recipeId.toString())

    suspend fun saveRecipe(uid: String, recipe: SavedRecipe) {
        if (BuildConfig.DEMO) { DemoStore.saveRecipe(uid, recipe); return }
        savedRecipeDoc(uid, recipe.recipeId).set(recipe).await()
    }

    suspend fun removeRecipe(uid: String, recipeId: Int) {
        if (BuildConfig.DEMO) { DemoStore.removeRecipe(uid, recipeId); return }
        savedRecipeDoc(uid, recipeId).delete().await()
    }

    suspend fun getSavedRecipes(uid: String): List<SavedRecipe> {
        if (BuildConfig.DEMO) return DemoStore.savedRecipes(uid)
        return userDoc(uid).collection("saved_recipes")
            .get().await()
            .toObjects(SavedRecipe::class.java)
    }

    suspend fun isSaved(uid: String, recipeId: Int): Boolean {
        if (BuildConfig.DEMO) return DemoStore.isSaved(uid, recipeId)
        return savedRecipeDoc(uid, recipeId).get().await().exists()
    }
}

package com.example.impulsepurchaserecoverykit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.impulsepurchaserecoverykit.ParsedReceipt
import com.example.impulsepurchaserecoverykit.database.AppDatabase
import com.example.impulsepurchaserecoverykit.database.ReceiptRepository
import com.example.impulsepurchaserecoverykit.database.entities.ReceiptEntity
import com.example.impulsepurchaserecoverykit.database.models.CategorySpend
import com.example.impulsepurchaserecoverykit.database.models.CategoryCount
import com.example.impulsepurchaserecoverykit.database.models.WeeklySpend
import com.example.impulsepurchaserecoverykit.database.models.WeeklyRegret
import com.example.impulsepurchaserecoverykit.database.entities.ItemReactionEntity
import com.example.impulsepurchaserecoverykit.ui.screens.OcrFailureReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ReceiptViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReceiptRepository

    // State for the UI
    private val _receiptCount = MutableStateFlow(0)
    val receiptCount: StateFlow<Int> = _receiptCount
    private val _ocrFailureReason = MutableStateFlow<OcrFailureReason?>(null)
    val ocrFailureReason: StateFlow<OcrFailureReason?> = _ocrFailureReason

    lateinit var averageRegret: Flow<Double?>
        private set
    init{
        val database = AppDatabase.getDatabase(application)
        repository = ReceiptRepository(database)

        averageRegret = repository.getAverageRegretScoreFlow()

        loadStats()
    }

    // ========== Receipt Operations ==========

    fun setOcrFailureReason(reason: OcrFailureReason?){
        _ocrFailureReason.value = reason
    }
    /**
     * Save a scanned receipt to database
     */
    fun saveReceipt(parsedReceipt: ParsedReceipt, imageUri: String?, onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                val receiptId = repository.saveReceipt(parsedReceipt, imageUri)
                loadStats() // Refresh stats
                onSuccess(receiptId)
            } catch (e: Exception) {
                // Handle error
                android.util.Log.e("ReceiptViewModel", "Error saving receipt", e)
            }
        }
    }

    /**
     * Get all receipts
     */
    fun getAllReceipts(): Flow<List<ReceiptEntity>> {
        return repository.getAllReceipts()
    }

    /**
     * Update regret score
     */
    fun updateRegretScore(
        receiptId: Long,
        regretScore: Int,
        note: String?,
        onDone: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            repository.updateRegretScore(receiptId, regretScore, note)
            loadStats()
            onDone?.invoke()
        }
    }

    /**
     * Delete a receipt
     */
    fun deleteReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch {
            repository.deleteReceipt(receipt)
            loadStats() // Refresh stats
        }
    }

    // ========== Statistics ==========

    private fun loadStats() {
        viewModelScope.launch {
            _receiptCount.value = repository.getReceiptCount()
        }
    }

    fun getHighRegretReceipts(): Flow<List<ReceiptEntity>> {
        return repository.getHighRegretReceipts(7)
    }
    fun getRecentReceipts(limit: Int = 5): Flow<List<ReceiptEntity>> {
        return repository.getRecentReceipts(limit)
    }

    fun getItemsForReceipt(receiptId: Long): Flow<List<com.example.impulsepurchaserecoverykit.database.entities.ItemEntity>> {
        return repository.getItemsForReceiptFlow(receiptId)
    }

    fun getSpendByCategory(): Flow<List<CategorySpend>> {
        return repository.getSpendByCategory()
    }

    fun getItemCountByCategory(): Flow<List<CategoryCount>> {
        return repository.getItemCountByCategory()
    }

    fun getTotalSpend(): Flow<Double?> {
        return repository.getTotalSpend()
    }

    fun getTopRegretReceipts(limit: Int = 3): Flow<List<ReceiptEntity>> {
        return repository.getTopRegretReceipts(limit)
    }

    fun getWeeklySpend(): Flow<List<WeeklySpend>> {
        return repository.getWeeklySpend()
    }

    fun getWeeklyAverageRegret(): Flow<List<WeeklyRegret>> {
        return repository.getWeeklyAverageRegret()
    }

    fun addEmotionCheckIn(
        receiptId: Long,
        regretScore: Int,
        mood: String,
        notes: String?,
        onDone: (() -> Unit)? = null
    )
    {
        viewModelScope.launch {
            try {
                repository.addEmotionCheckIn(receiptId, regretScore, mood, notes)
                loadStats()
                onDone?.invoke()
            }catch (e: Exception){
                android.util.Log.e("ReceiptViewModel", "Error saving emotion check-in", e)
            }
        }
    }
    fun getItemReactionsForReceipt(receiptId: Long): Flow<List<ItemReactionEntity>> {
        return repository.getItemReactionsForReceipt(receiptId)
    }

    fun setItemReaction(receiptId: Long, itemId: Long, reaction: Int) {
        viewModelScope.launch {
            repository.setItemReaction(receiptId, itemId, reaction)
        }
    }

    fun saveItemReactions(receiptId: Long, draft: Map<Long, Int>) {
        viewModelScope.launch {
            try {
                repository.saveItemReactions(receiptId, draft)
            } catch (e: Exception) {
                android.util.Log.e("ReceiptViewModel", "Error saving item reactions", e)
            }
        }
    }
    fun updatePurchaseTime(receiptId: Long, time: String){
        viewModelScope.launch{
            repository.updatePurchaseTime(receiptId, time)
        }
    }

}
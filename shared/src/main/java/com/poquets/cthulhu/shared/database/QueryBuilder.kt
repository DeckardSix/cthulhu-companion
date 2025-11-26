package com.poquets.cthulhu.shared.database

/**
 * Builder pattern for constructing complex card queries
 */
class QueryBuilder(private val gameType: GameType) {
    private var expansion: String? = null
    private var region: String? = null
    private var neighborhoodId: Long? = null
    private var locationId: Long? = null
    private var encountered: String? = null
    private var onlyUnencountered: Boolean = false
    private var limit: Int? = null
    private var orderBy: String? = null
    
    fun withExpansion(expansion: String): QueryBuilder {
        this.expansion = expansion
        return this
    }
    
    fun withRegion(region: String): QueryBuilder {
        this.region = region
        return this
    }
    
    fun withNeighborhood(neighborhoodId: Long): QueryBuilder {
        this.neighborhoodId = neighborhoodId
        return this
    }
    
    fun withLocation(locationId: Long): QueryBuilder {
        this.locationId = locationId
        return this
    }
    
    fun withEncountered(encountered: String): QueryBuilder {
        this.encountered = encountered
        return this
    }
    
    fun onlyUnencountered(): QueryBuilder {
        this.onlyUnencountered = true
        return this
    }
    
    fun limit(count: Int): QueryBuilder {
        this.limit = count
        return this
    }
    
    fun orderBy(field: String): QueryBuilder {
        this.orderBy = field
        return this
    }
    
    /**
     * Execute the query using the database helper
     * Note: This is a simplified version. For complex queries, use the database helper methods directly.
     */
    fun execute(context: android.content.Context): List<UnifiedCard> {
        // Use appropriate database helper method based on filters
        return when {
            onlyUnencountered -> {
                UnifiedCardDatabaseHelper.getInstance(context)
                    .getUnencounteredCards(gameType, expansion, region)
            }
            region != null && expansion != null -> {
                UnifiedCardDatabaseHelper.getInstance(context)
                    .getCardsByGameTypeExpansionAndRegion(gameType, expansion!!, region!!)
            }
            region != null -> {
                UnifiedCardDatabaseHelper.getInstance(context)
                    .getCardsByGameTypeAndRegion(gameType, region!!)
            }
            expansion != null -> {
                UnifiedCardDatabaseHelper.getInstance(context)
                    .getCardsByGameTypeAndExpansion(gameType, expansion!!)
            }
            else -> {
                UnifiedCardDatabaseHelper.getInstance(context)
                    .getCardsByGameType(gameType)
            }
        }.let { cards ->
            // Apply additional filters
            var filtered = cards
            
            neighborhoodId?.let { id ->
                filtered = filtered.filter { it.neighborhoodId == id }
            }
            
            locationId?.let { id ->
                filtered = filtered.filter { it.locationId == id }
            }
            
            encountered?.let { status ->
                filtered = filtered.filter { it.encountered == status }
            }
            
            // Apply limit
            limit?.let { filtered.take(it) } ?: filtered
        }
    }
}

/**
 * Extension function to create a query builder
 */
fun queryCards(gameType: GameType): QueryBuilder {
    return QueryBuilder(gameType)
}


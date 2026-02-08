package io.krazy.dependency.api;

import java.util.Collection;
import java.util.Map;

/**
 * Represents the result of the dependency mapping process.
 * Contains a collection of {@link DependencyRecord}s for all registered
 * services.
 */
public final class MappingResult
{
    private final Map<Class<?>, DependencyRecord> dependencyRecordMap;

    /**
     * Constructs a new MappingResult.
     *
     * @param dependencyRecordMap a map of implementation types to their dependency
     *                            records
     */
    public MappingResult(Map<Class<?>, DependencyRecord> dependencyRecordMap)
    {
        this.dependencyRecordMap = dependencyRecordMap;
    }

    /**
     * Checks if a dependency record exists for the specified implementation type.
     *
     * @param klass the implementation class
     * @return true if a record exists, false otherwise
     */
    public boolean hasRecord(Class<?> klass)
    {
        return dependencyRecordMap.containsKey(klass);
    }

    /**
     * Gets the dependency record for the specified implementation type.
     *
     * @param klass the implementation class
     * @return the {@link DependencyRecord}, or null if not found
     */
    public DependencyRecord getRecord(Class<?> klass)
    {
        return dependencyRecordMap.get(klass);
    }

    /**
     * Gets all dependency records in this result.
     *
     * @return an unmodifiable collection of {@link DependencyRecord}s
     */
    public Collection<DependencyRecord> getRecords()
    {
        return dependencyRecordMap.values();
    }

    /**
     * Gets the total number of registered dependency records.
     *
     * @return the record count
     */
    public int getRecordCount()
    {
        return dependencyRecordMap.size();
    }
}

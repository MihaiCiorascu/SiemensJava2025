package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final List<Item> processedItems = new CopyOnWriteArrayList<>();

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {

        List<Long> itemIds = itemRepository.findAllIds();

        if (itemIds.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        List<CompletableFuture<Item>> futures = new ArrayList<>();
        
        for (Long id : itemIds) {
            CompletableFuture<Item> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Optional<Item> itemOpt = itemRepository.findById(id);
                    if (itemOpt.isPresent()) {
                        Item item = itemOpt.get();
                        item.setStatus("PROCESSED");
                        Item savedItem = itemRepository.save(item);
                        processedItems.add(savedItem);
                        processedCount.incrementAndGet();
                        return savedItem;
                    }
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
                return null;
            }, executor);
            
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<Item> result = new ArrayList<>();
                    for (CompletableFuture<Item> future : futures) {
                        try {
                            Item item = future.get();
                            if (item != null) {
                                result.add(item);
                            }
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }
                    return result;
                });
    }
}
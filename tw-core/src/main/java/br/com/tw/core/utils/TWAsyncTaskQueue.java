package br.com.tw.core.utils;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;

public final class TWAsyncTaskQueue {

    private final JavaPlugin plugin;
    private final ExecutorService executor;

    public TWAsyncTaskQueue(JavaPlugin plugin, String threadName) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, threadName);
            thread.setDaemon(true);
            return thread;
        });
    }

    public void submit(String description, Runnable task) {
        try {
            executor.execute(() -> {
                try {
                    task.run();
                } catch (RuntimeException e) {
                    plugin.getLogger().log(Level.SEVERE, description, e);
                }
            });
        } catch (RejectedExecutionException e) {
            plugin.getLogger().log(Level.SEVERE, description + " (fila já encerrada).", e);
        }
    }

    public void shutdownAndAwait() {
        executor.shutdown();
        boolean interrupted = false;
        while (!executor.isTerminated()) {
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    plugin.getLogger().severe("Ainda aguardando operações assíncronas antes de desativar o plugin.");
                }
            } catch (InterruptedException e) {
                interrupted = true;
                executor.shutdownNow();
                plugin.getLogger().log(Level.SEVERE,
                        "Interrompido enquanto aguardava operações assíncronas; aguardando o encerramento.", e);
            }
        }
        if (interrupted) Thread.currentThread().interrupt();
    }
}

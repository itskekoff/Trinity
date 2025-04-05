package me.f1nal.trinity.execution.compile;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.util.ByteUtil;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class ClassWriterTask {
    private final ExecutorService executorService = Executors.newFixedThreadPool(6);
    private final List<ClassInput> classNodes;
    private final Map<String, byte[]> resources;
    private final Trinity trinity;
    private final Console console;
    private final File outputFile;

    public ClassWriterTask(List<ClassInput> classNodes, Map<String, byte[]> resources, Trinity trinity, Console console, File outputFile) {
        this.classNodes = classNodes;
        this.resources = resources;
        this.trinity = trinity;
        this.console = console;
        this.outputFile = outputFile;
    }

    private ClassNode getType(String typeName) {
        ClassInput classInput = trinity.getExecution().getClassInput(typeName);
        if (classInput != null) {
            return classInput.getNode();
        }
        return trinity.getJrtInput().getClass(typeName);
    }

    public void build(Consumer<Float> progressConsumer, Runnable finish) {
        if (classNodes.isEmpty()) {
            console.warn("Project contains only resources (empty classes), writing resources");
            final int resourceSize = resources.size();
            if (resourceSize == 0) {
                console.warn("Project is empty, no classes or resources to write");
                finish.run();
                return;
            }
            final Map<String, byte[]> entryMap = new HashMap<>();
            final AtomicInteger processed = new AtomicInteger();
            for (Map.Entry<String, byte[]> resource : resources.entrySet()) {
                executorService.submit(() -> {
                    entryMap.put(resource.getKey(), resource.getValue());
                    int processedCount = processed.incrementAndGet();
                    progressConsumer.accept((float) processedCount / (float) resourceSize);
                    if (processedCount == resourceSize) {
                        this.writeJarFile(entryMap);
                        progressConsumer.accept(0f);
                        finish.run();
                    }
                });
            }
        } else {
            final Map<String, byte[]> entryMap = new HashMap<>(resources);
            final AtomicInteger written = new AtomicInteger();
            final int classSize = classNodes.size();
            for (ClassInput classInput : classNodes) {
                executorService.submit(() -> {
                    ClassNode classNode = classInput.getNode();
                    try {
                        SafeClassWriter classWriter = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, this::getType, console);
                        classNode.accept(classWriter);
                        entryMap.put(classNode.name + ".class", classWriter.toByteArray());
                    } catch (Throwable throwable) {
                        executorService.shutdown();
                        throwable.printStackTrace();
                        finish.run();
                        console.error("Error in class %s: %s".formatted(classNode.name, throwable.getMessage()));
                        return;
                    }
                    int writtenCount = written.incrementAndGet();
                    if (writtenCount == classSize) {
                        this.writeJarFile(entryMap);
                        progressConsumer.accept(0f);
                        finish.run();
                        return;
                    }
                    progressConsumer.accept((float) writtenCount / (float) classSize);
                });
            }
        }
    }


    private void writeJarFile(Map<String, byte[]> entryMap) {
        byte[] jarBytes = null;
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            JarOutputStream jarOutputStream = new JarOutputStream(byteArrayOutputStream);
            for (Map.Entry<String, byte[]> entry : entryMap.entrySet()) {
                try {
                    jarOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                    jarOutputStream.write(entry.getValue());
                    jarOutputStream.closeEntry();
                } catch (Exception ohShit) {}
            }
            jarOutputStream.close();
            jarBytes = byteArrayOutputStream.toByteArray();
            console.info("Created in-memory JAR of {}.", ByteUtil.getHumanReadableByteCountSI(jarBytes.length));
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            console.error("Failed to create JAR file in memory.");
            return;
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(this.outputFile);
            fileOutputStream.write(jarBytes);
            fileOutputStream.close();
            console.info("JAR successfully saved to disk.");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            console.error("Failed to write file to disk.");
            return;
        }
    }
}

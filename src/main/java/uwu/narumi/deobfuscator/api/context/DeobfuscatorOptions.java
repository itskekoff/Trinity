package uwu.narumi.deobfuscator.api.context;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Immutable options for deobfuscator
 */
public record DeobfuscatorOptions(
    @Nullable Path inputJar,
    List<ExternalFile> externalFiles,
    Set<Path> libraries,

    @Nullable Path outputJar,
    @Nullable Path outputDir,

    List<Supplier<Transformer>> transformers,

    @MagicConstant(flagsFromClass = ClassWriter.class) int classWriterFlags,

    boolean printStacktraces,
    boolean continueOnError,
    boolean verifyBytecode,
    boolean skipFiles
) {
  public static Builder builder() {
    return new Builder();
  }

  /**
   * @param path Path to the raw file
   * @param pathInJar Relative path to file as if it were in .jar
   */
  public record ExternalFile(Path path, String pathInJar) {
  }

  /**
   * Builder for {@link DeobfuscatorOptions}
   */
  public static class Builder {
    // Inputs
    @Nullable
    private Path inputJar = null;
    private final List<ExternalFile> externalFiles = new ArrayList<>();
    private final Set<Path> libraries = new HashSet<>();

    // Outputs
    @Nullable
    private Path outputJar = null;
    @Nullable
    private Path outputDir = null;

    // Transformers
    private final List<Supplier<Transformer>> transformers = new ArrayList<>();

    // Other config
    @MagicConstant(flagsFromClass = ClassWriter.class)
    private int classWriterFlags = ClassWriter.COMPUTE_FRAMES;

    private boolean printStacktraces = true;
    private boolean continueOnError = false;
    private boolean verifyBytecode = false;
    private boolean skipFiles = false;

    private Builder() {
    }

    /**
     * Your input jar file
     */
    @Contract("_ -> this")
    public Builder inputJar(@Nullable Path inputJar) {
      this.inputJar = inputJar;
      if (this.inputJar != null) {
        // Auto fill output jar
        if (this.outputJar == null) {
          String fullName = inputJar.getFileName().toString();
          int dot = fullName.lastIndexOf('.');

          this.outputJar = inputJar.getParent()
              .resolve(dot == -1 ? fullName + "-out" : fullName.substring(0, dot) + "-out" + fullName.substring(dot));
        }
      }
      return this;
    }

    /**
     * Add an external file to the deobfuscation context. You can add raw .class files or files that would be in .jar
     *
     * @param path Path to an external file
     * @param pathInJar Relative path to file if it were in .jar
     */
    @Contract("_,_ -> this")
    public Builder externalFile(Path path, String pathInJar) {
      this.externalFiles.add(new ExternalFile(path, pathInJar));
      return this;
    }

    /**
     * Adds all files from the directory to the deobfuscation context
     *
     * @param path Path to the directory
     */
    @Contract("_ -> this")
    public Builder inputDir(Path path) {
      try {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String pathInJar = path.relativize(file).toString();
            externalFile(file, pathInJar);
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return this;
    }

    /**
     * Add libraries to the classpath. You can pass here files or directories.
     *
     * @param paths Paths to libraries
     */
    @Contract("_ -> this")
    public Builder libraries(Path... paths) {
      for (Path path : paths) {
        if (Files.isDirectory(path)) {
          try {
            // Walk through directory
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
              @Override
              public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                libraries.add(file);
                return FileVisitResult.CONTINUE;
              }
            });
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        } else {
          this.libraries.add(path);
        }
      }
      return this;
    }

    /**
     * Output jar for deobfuscated classes. Automatically filled when input jar is set
     */
    @Contract("_ -> this")
    public Builder outputJar(@Nullable Path outputJar) {
      this.outputJar = outputJar;
      return this;
    }

    /**
     * Set output dir it if you want to output raw compiled classes instead of jar file
     */
    @Contract("_ -> this")
    public Builder outputDir(@Nullable Path outputDir) {
      this.outputDir = outputDir;
      return this;
    }

    /**
     * Transformers to run. You need to specify them in lambda form:
     * <pre>
     * {@code
     * () -> new MyTransformer(true, false),
     * () -> new AnotherTransformer(),
     * () -> new SuperTransformer()
     * }
     * </pre>
     *
     * We can push it further, and we can replace lambdas with no arguments with method references:
     * <pre>
     * {@code
     * () -> new MyTransformer(true, false),
     * AnotherTransformer::new,
     * SuperTransformer::new
     * }
     * </pre>
     */
    @SafeVarargs
    @Contract("_ -> this")
    public final Builder transformers(Supplier<Transformer>... transformers) {
      this.transformers.addAll(List.of(transformers));
      return this;
    }

    /**
     * Flags for {@link ClassWriter}.
     * <ul>
     * <li><code>0</code> - Deobfuscated jar can't be run</li>
     * <li>{@link ClassWriter#COMPUTE_FRAMES} - Makes a runnable deobfuscated jar</li>
     * </ul>
     */
    @Contract("_ -> this")
    public Builder classWriterFlags(@MagicConstant(flagsFromClass = ClassWriter.class) int classWriterFlags) {
      this.classWriterFlags = classWriterFlags;
      return this;
    }

    /**
     * Disables stacktraces logging
     */
    @Contract(" -> this")
    public Builder noStacktraces() {
      this.printStacktraces = false;
      return this;
    }

    /**
     * Continue deobfuscation even if errors occur
     */
    @Contract(" -> this")
    public Builder continueOnError() {
      this.continueOnError = true;
      return this;
    }

    /**
     * Verify bytecode after each transformer run. Useful when debugging which
     * transformer is causing issues (aka broke bytecode)
     */
    @Contract(" -> this")
    public Builder verifyBytecode() {
      this.verifyBytecode = true;
      return this;
    }

    /**
     * Skips files during saving.
     */
    @Contract(" -> this")
    public Builder skipFiles() {
      this.skipFiles = true;
      return this;
    }

    /**
     * Build immutable {@link DeobfuscatorOptions} with options verification
     */
    public DeobfuscatorOptions build() {
      // Verify some options
      if (this.inputJar == null && this.externalFiles.isEmpty()) {
        throw new IllegalStateException("No input files provided");
      }
      if (this.outputJar == null && this.outputDir == null) {
        throw new IllegalStateException("No output file or directory provided");
      }
      if (this.outputJar != null && this.outputDir != null) {
        throw new IllegalStateException("Output jar and output dir cannot be set at the same time");
      }

      return new DeobfuscatorOptions(
          // Input
          inputJar,
          externalFiles,
          libraries,
          // Output
          outputJar,
          outputDir,
          // Transformers
          transformers,
          // Flags
          classWriterFlags,
          // Other config
          printStacktraces,
          continueOnError,
          verifyBytecode,
          skipFiles
      );
    }
  }
}

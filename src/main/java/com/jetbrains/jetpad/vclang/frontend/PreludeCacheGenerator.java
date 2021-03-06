package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleDynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleStaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.resolving.ResolvingModuleLoader;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.module.DefaultModuleLoader;
import com.jetbrains.jetpad.vclang.module.caching.CachePersistenceException;
import com.jetbrains.jetpad.vclang.module.caching.CacheStorageSupplier;
import com.jetbrains.jetpad.vclang.module.caching.CachingModuleLoader;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.DefinitionLocator;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;
import com.jetbrains.jetpad.vclang.typechecking.order.BaseDependencyListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class PreludeCacheGenerator {
  private static class PreludeBuildCacheSupplier implements CacheStorageSupplier<PreludeStorage.SourceId> {
    private final Path targetPath;

    private PreludeBuildCacheSupplier(Path targetPath) {
      this.targetPath = targetPath;
    }

    @Override
    public InputStream getCacheInputStream(PreludeStorage.SourceId sourceId) {
      throw new IllegalStateException();
    }

    @Override
    public OutputStream getCacheOutputStream(PreludeStorage.SourceId sourceId) {
      Path path = targetPath.resolve(PreludeStorage.CACHE_PATH);
      try {
        Files.createDirectories(path.getParent());
        return Files.newOutputStream(path);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class PreludeDefLocator implements DefinitionLocator<PreludeStorage.SourceId> {
    private final PreludeStorage.SourceId preludeSourceId;

    private PreludeDefLocator(PreludeStorage.SourceId preludeSourceId) {
      this.preludeSourceId = preludeSourceId;
    }

    @Override
    public PreludeStorage.SourceId sourceOf(Abstract.Definition definition) {
      return preludeSourceId;
    }
  }

  static class PreludePersistenceProvider implements PersistenceProvider<PreludeStorage.SourceId> {
    @Override
    public URI getUri(PreludeStorage.SourceId sourceId) {
      throw new IllegalStateException();
    }

    @Override
    public PreludeStorage.SourceId getModuleId(URI sourceUrl) {
      throw new IllegalStateException();
    }

    @Override
    public String getIdFor(Abstract.Definition definition) {
      if (!(definition instanceof Concrete.Definition)) throw new IllegalStateException();
      Concrete.Position pos = ((Concrete.Definition) definition).getPosition();
      if (pos == null) throw new IllegalStateException();
      return pos.line + ";" + pos.column;
    }

    @Override
    public Abstract.Definition getFromId(PreludeStorage.SourceId sourceId, String id) {
      throw new IllegalStateException();
    }
  }

  public static void main(String[] args) {
    final NameResolver nameResolver = new NameResolver();
    final StaticNamespaceProvider staticNsProvider = new SimpleStaticNamespaceProvider();
    final DynamicNamespaceProvider dynamicNsProvider = new SimpleDynamicNamespaceProvider();
    final ListErrorReporter errorReporter = new ListErrorReporter();
    PreludeStorage storage = new PreludeStorage();
    ResolvingModuleLoader<PreludeStorage.SourceId> baseModuleLoader = new ResolvingModuleLoader<>(storage, new DefaultModuleLoader.ModuleLoadingListener<PreludeStorage.SourceId>(), nameResolver, staticNsProvider, dynamicNsProvider, new ConcreteResolveListener(errorReporter), errorReporter);
    CachingModuleLoader<PreludeStorage.SourceId> moduleLoader = new CachingModuleLoader<>(baseModuleLoader, new PreludePersistenceProvider(), new PreludeBuildCacheSupplier(Paths.get(args[0])), new PreludeDefLocator(storage.preludeSourceId), false);
    nameResolver.setModuleLoader(moduleLoader);
    Abstract.ClassDefinition prelude = moduleLoader.load(storage.preludeSourceId);
    if (!errorReporter.getErrorList().isEmpty()) throw new IllegalStateException();
    new Typechecking(moduleLoader.getTypecheckerState(), staticNsProvider, dynamicNsProvider, errorReporter, new Prelude.UpdatePreludeReporter(moduleLoader.getTypecheckerState()), new BaseDependencyListener()).typecheckModules(Collections.singleton(prelude));
    try {
      moduleLoader.persistModule(storage.preludeSourceId);
    } catch (IOException | CachePersistenceException e) {
      throw new IllegalStateException();
    }
  }
}
